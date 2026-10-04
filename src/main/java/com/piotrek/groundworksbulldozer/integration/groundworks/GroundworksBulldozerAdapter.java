package com.piotrek.groundworksbulldozer.integration.groundworks;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.block.entity.GranularBlockEntity;
import com.piotrek.groundworks.networking.GranularSyncHandler;
import com.piotrek.groundworks.terrain.cell.DirtyFlags;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.conversion.BlockConverter;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side adapter bridging the Bulldozer mod to Peterwolf's Groundworks engine.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Never destroying blocks directly — only using Groundworks granular conversions.</li>
 *   <li>Exact volumetric removal of microvoxels intersected by the blade cutting edge.</li>
 *   <li>Leveling depressions flush with blade cutting grade.</li>
 *   <li>Volume-conserving berm pushing and lateral spillage.</li>
 *   <li>Triggering Groundworks granular relaxation on modified cells.</li>
 * </ul>
 */
public class GroundworksBulldozerAdapter implements IGranularTerrainAccess {

    private final ServerLevel level;

    public GroundworksBulldozerAdapter(ServerLevel level) {
        this.level = level;
    }

    public static GroundworksBulldozerAdapter of(ServerLevel level) {
        return new GroundworksBulldozerAdapter(level);
    }

    @Override
    public boolean isDiggable(BlockPos pos) {
        GranularCell cell = GroundworksApi.queryCell(level, pos);
        if (cell != null && !cell.isEmpty()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return BlockConverter.isConvertible(state);
    }

    @Override
    public GranularCell getCell(BlockPos pos) {
        return GroundworksApi.queryCell(level, pos);
    }

    @Override
    public GranularCell getOrConvert(BlockPos pos) {
        GranularWorldStorage storage = GranularWorldStorage.get(level);
        return storage.getOrConvert(pos);
    }

    @Override
    public int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits) {
        if (maxUnits <= 0) {
            return 0;
        }

        GranularWorldStorage storage = GranularWorldStorage.get(level);
        GranularCell cell = storage.getOrConvert(pos);
        if (cell == null || cell.isEmpty()) {
            return 0;
        }

        double localCutY = (worldCutY - pos.getY()) * GranularCell.RESOLUTION;

        // If blade is above the top of this block, nothing in this block is cut
        if (localCutY >= GranularCell.RESOLUTION) {
            return 0;
        }

        int removed = 0;
        int startY = Math.max(0, (int) Math.floor(localCutY));

        for (int y = GranularCell.RESOLUTION - 1; y >= startY && removed < maxUnits; y--) {
            // If localCutY falls within layer y, shave only if y >= localCutY
            if (y < localCutY) {
                continue;
            }
            for (int z = 0; z < GranularCell.RESOLUTION && removed < maxUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && removed < maxUnits; x++) {
                    if (cell.clear(x, y, z)) {
                        removed++;
                    }
                }
            }
        }

        if (removed > 0) {
            syncCell(storage, pos, cell);
        }

        return removed;
    }

    @Override
    public int fillDepressionBelow(BlockPos pos, double targetWorldY, GranularMaterial material, int availableUnits) {
        if (availableUnits <= 0 || material == null || material.id() == 0) {
            return 0;
        }

        double localTargetY = (targetWorldY - pos.getY()) * GranularCell.RESOLUTION;
        if (localTargetY <= 0.0D) {
            return 0;
        }

        GranularWorldStorage storage = GranularWorldStorage.get(level);
        GranularCell cell = storage.getCell(pos);

        if (cell == null) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                cell = GranularCell.empty();
                cell.setMaterialId(material.id());
                storage.putCell(pos, cell);
                level.setBlock(pos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            } else if (BlockConverter.isConvertible(state)) {
                cell = storage.getOrConvert(pos);
            } else {
                return 0;
            }
        }

        if (cell == null) {
            return 0;
        }

        if (cell.isEmpty()) {
            cell.setMaterialId(material.id());
        } else if (cell.materialId() != material.id()) {
            return 0; // Mixed materials not supported in single cell
        }

        int maxLocalY = Math.min(GranularCell.RESOLUTION - 1, (int) Math.floor(localTargetY));
        int added = 0;

        for (int y = 0; y <= maxLocalY && added < availableUnits; y++) {
            for (int z = 0; z < GranularCell.RESOLUTION && added < availableUnits; z++) {
                for (int x = 0; x < GranularCell.RESOLUTION && added < availableUnits; x++) {
                    if (cell.set(x, y, z)) {
                        added++;
                    }
                }
            }
        }

        if (added > 0) {
            syncCell(storage, pos, cell);
        }

        return added;
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) {
            return 0;
        }
        DepositResult result = GroundworksApi.depositWithOverflow(level, pos, material, units);
        return result.unitsDeposited();
    }

    @Override
    public void markSimulate(BlockPos pos) {
        GranularWorldStorage storage = GranularWorldStorage.get(level);
        GranularCell cell = storage.getCell(pos);
        if (cell != null) {
            cell.markDirty(DirtyFlags.SIMULATE);
            storage.enqueueDirty(pos);
            storage.setDirty();
        }
    }

    private void syncCell(GranularWorldStorage storage, BlockPos pos, GranularCell cell) {
        cell.markDirty(DirtyFlags.SYNC | DirtyFlags.MESH | DirtyFlags.SIMULATE);
        storage.enqueueDirty(pos);
        storage.setDirty();

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GranularBlockEntity gbe) {
            gbe.setMaterialId(cell.materialId());
            gbe.setChanged();
            level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }

        GranularSyncHandler.sendCellUpdate(level, pos, cell);

        if (cell.isEmpty()) {
            storage.removeCell(pos);
            level.removeBlock(pos, false);
        }
    }
}
