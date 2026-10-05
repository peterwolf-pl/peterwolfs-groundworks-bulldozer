package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksbulldozer.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * In-memory grading backend for deterministic volume-conservation tests.
 */
public class TestGranularTerrain implements IGranularTerrainAccess {

    private static final double EPSILON = 1.0E-9D;

    private final Map<BlockPos, GranularCell> cells = new HashMap<>();
    private final Map<BlockPos, GranularMaterial> convertibleBlocks = new HashMap<>();
    private final Set<BlockPos> simulatedPositions = new HashSet<>();

    public void putCell(BlockPos pos, GranularCell cell) {
        cells.put(pos.immutable(), cell);
    }

    public void createFullCell(BlockPos pos, GranularMaterial material) {
        cells.put(pos.immutable(), GranularCell.full(material));
    }

    public void createConvertibleBlock(BlockPos pos, GranularMaterial material) {
        convertibleBlocks.put(pos.immutable(), material);
    }

    public void createPile(BlockPos pos, GranularMaterial material, int microvoxelLayers) {
        GranularCell cell = GranularCell.empty();
        cell.setMaterialId(material.id());
        int layers = Math.clamp(microvoxelLayers, 0, GranularCell.RESOLUTION);
        for (int y = 0; y < layers; y++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                    cell.set(x, y, z);
                }
            }
        }
        cells.put(pos.immutable(), cell);
    }

    public int countTotalWorldUnits() {
        int total = 0;
        for (GranularCell cell : cells.values()) {
            total += cell.unitCount();
        }
        return total;
    }

    public Set<BlockPos> simulatedPositions() {
        return simulatedPositions;
    }

    /**
     * Test-only inspection helper. Not part of the production terrain boundary.
     */
    public GranularCell getCell(BlockPos pos) {
        return cells.get(pos);
    }

    @Override
    public boolean isDiggable(BlockPos pos) {
        GranularCell cell = cells.get(pos);
        return (cell != null && !cell.isEmpty()) || convertibleBlocks.containsKey(pos);
    }

    @Override
    public GranularMaterial getMaterial(BlockPos pos) {
        GranularCell cell = cells.get(pos);
        if (cell != null && !cell.isEmpty()) {
            return cell.material();
        }
        GranularMaterial convertible = convertibleBlocks.get(pos);
        return convertible != null ? convertible : GranularMaterial.EMPTY;
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) return 0;

        GranularCell cell = getOrConvert(pos);
        if (cell == null || cell.isEmpty()) return 0;

        double localCutY = (worldCutY - pos.getY()) * GranularCell.RESOLUTION;
        int startY = localCutY <= 0.0D
                ? 0
                : (int) Math.ceil(localCutY - EPSILON);
        if (startY >= GranularCell.RESOLUTION) return 0;

        int removed = 0;
        for (int y = GranularCell.RESOLUTION - 1; y >= startY && removed < maxUnits; y--) {
            for (int z = 0; z < GranularCell.RESOLUTION && removed < maxUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && removed < maxUnits; x++) {
                    if (cell.clear(x, y, z)) {
                        removed++;
                    }
                }
            }
        }

        if (cell.isEmpty()) {
            cells.remove(pos);
        }
        return removed;
    }

    @Override
    public int fillDepressionBelow(
            BlockPos pos,
            double targetWorldY,
            GranularMaterial material,
            int availableUnits
    ) {
        if (availableUnits <= 0 || material == null || material.id() == 0) return 0;

        double localTargetY = (targetWorldY - pos.getY()) * GranularCell.RESOLUTION;
        int fullLayers = localTargetY <= 0.0D
                ? 0
                : Math.min(
                        GranularCell.RESOLUTION,
                        (int) Math.floor(localTargetY + EPSILON)
                );
        if (fullLayers <= 0) return 0;

        GranularCell cell = cells.computeIfAbsent(pos.immutable(), p -> {
            GranularCell c = GranularCell.empty();
            c.setMaterialId(material.id());
            return c;
        });

        if (cell.isEmpty()) {
            cell.setMaterialId(material.id());
        } else if (cell.materialId() != material.id()) {
            return 0;
        }

        int added = 0;
        for (int y = 0; y < fullLayers && added < availableUnits; y++) {
            for (int z = 0; z < GranularCell.RESOLUTION && added < availableUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && added < availableUnits; x++) {
                    if (cell.set(x, y, z)) {
                        added++;
                    }
                }
            }
        }
        return added;
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) return 0;

        int remaining = units;
        int totalAdded = 0;
        BlockPos current = pos;

        for (int attempt = 0; attempt < 8 && remaining > 0; attempt++) {
            GranularCell cell = cells.computeIfAbsent(current.immutable(), p -> {
                GranularCell c = GranularCell.empty();
                c.setMaterialId(material.id());
                return c;
            });

            if (cell.isEmpty()) {
                cell.setMaterialId(material.id());
            }

            if (cell.materialId() == material.id()) {
                int added = cell.addFromBottom(remaining);
                totalAdded += added;
                remaining -= added;
            }

            if (remaining > 0) {
                current = current.above();
            }
        }
        return totalAdded;
    }

    @Override
    public void markSimulate(BlockPos pos) {
        simulatedPositions.add(pos.immutable());
    }

    private GranularCell getOrConvert(BlockPos pos) {
        BlockPos key = pos.immutable();
        GranularCell existing = cells.get(key);
        if (existing != null) {
            return existing;
        }

        GranularMaterial material = convertibleBlocks.remove(key);
        if (material == null) {
            return null;
        }

        GranularCell converted = GranularCell.full(material);
        cells.put(key, converted);
        return converted;
    }
}
