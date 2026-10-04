package com.piotrek.groundworksbulldozer.blade;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksbulldozer.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Physical terrain interaction controller for the bulldozer blade.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Continuous height grading: Raised blade doesn't touch ground; lowered blade cuts deeper.</li>
 *   <li>Volumetric conservation: Every removed microvoxel is accounted for in the live carry buffer,
 *       depression filling, forward berm, or lateral spillage. Zero duplication, zero loss.</li>
 *   <li>Terrain leveling: Cuts off peaks above blade edge and fills ruts/depressions below blade edge.</li>
 *   <li>Lateral spillage: Overloaded material escapes past left and right blade wings.</li>
 *   <li>Groundworks relaxation: Marks modified cells for natural angle-of-repose settling.</li>
 * </ul>
 */
public final class BulldozerBladeController {

    public static final int MAX_BLADE_CAPACITY = 768; // 1.5 full blocks of granular material
    public static final double MIN_MOVE_SPEED = 0.005D;
    public static final double MAX_VALID_MOVE = 2.0D;
    public static final int SWEEP_SUBDIVISIONS = 3;

    public record BladeTickResult(
            int unitsExcavated,
            int unitsDeposited,
            int carriedUnitsAfter,
            GranularMaterial carriedMaterialAfter,
            boolean isPushing,
            List<BlockPos> affectedPositions
    ) {
        public static final BladeTickResult NONE = new BladeTickResult(
                0, 0, 0, GranularMaterial.EMPTY, false, List.of()
        );
    }

    private BulldozerBladeController() {}

    /**
     * Executes one tick of blade physical interaction with Groundworks terrain.
     *
     * @param terrain            Terrain access interface (live world or test harness)
     * @param prevTransform      Blade transform from previous tick
     * @param currTransform      Blade transform for current tick
     * @param carriedUnitsBefore Units currently held in front of the blade
     * @param carriedMaterial    Current material type held in front of the blade
     * @return Result of the blade grading tick
     */
    public static BladeTickResult tick(
            IGranularTerrainAccess terrain,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            int carriedUnitsBefore,
            GranularMaterial carriedMaterial
    ) {
        if (prevTransform == null || currTransform == null) {
            return new BladeTickResult(0, 0, carriedUnitsBefore, carriedMaterial, false, List.of());
        }

        Vec3 prevCenter = prevTransform.cuttingEdgeCenter();
        Vec3 currCenter = currTransform.cuttingEdgeCenter();
        Vec3 motion = currCenter.subtract(prevCenter);
        double dist = motion.length();

        // Must be actively moving (and not teleporting)
        if (dist < MIN_MOVE_SPEED || dist > MAX_VALID_MOVE) {
            return new BladeTickResult(0, 0, carriedUnitsBefore, carriedMaterial, false, List.of());
        }

        // Check if moving forward in the direction of the blade face
        double forwardAlignment = motion.normalize().dot(currTransform.forward());
        if (forwardAlignment < 0.1D) {
            // Not moving forward into terrain (reversing or pure lateral drift)
            return new BladeTickResult(0, 0, carriedUnitsBefore, carriedMaterial, false, List.of());
        }

        int currentUnits = carriedUnitsBefore;
        GranularMaterial currentMaterial = carriedMaterial != null ? carriedMaterial : GranularMaterial.EMPTY;

        int totalExcavated = 0;
        int totalDeposited = 0;
        Set<BlockPos> affected = new HashSet<>();

        // ── 1. Swept Cutting Edge Sampling ─────────────────────────────────
        List<Vec3> prevEdge = prevTransform.cuttingEdgePoints();
        List<Vec3> currEdge = currTransform.cuttingEdgePoints();
        int edgeCount = Math.min(prevEdge.size(), currEdge.size());

        Set<BlockPos> processedBlocks = new HashSet<>();

        for (int step = 1; step <= SWEEP_SUBDIVISIONS; step++) {
            double alpha = (double) step / SWEEP_SUBDIVISIONS;

            for (int e = 0; e < edgeCount; e++) {
                Vec3 p0 = prevEdge.get(e);
                Vec3 p1 = currEdge.get(e);
                Vec3 pt = p0.lerp(p1, alpha);

                // A. Shave high ground (Cut material in front of moldboard & above cutting edge)
                BlockPos pos = BlockPos.containing(pt.x, pt.y, pt.z);
                BlockPos above = pos.above();
                BlockPos below = pos.below();

                List<BlockPos> candidates = new ArrayList<>(3);
                if (pt.y + BladeTransform.DEFAULT_BLADE_HEIGHT > above.getY()) {
                    candidates.add(above);
                }
                candidates.add(pos);
                if (pt.y < pos.getY() + 0.35D) {
                    candidates.add(below);
                }

                for (BlockPos targetPos : candidates) {
                    if (processedBlocks.add(targetPos) && terrain.isDiggable(targetPos)) {
                        int room = MAX_BLADE_CAPACITY - currentUnits;
                        if (room > 0) {
                            GranularCell cell = terrain.getCell(targetPos);
                            int cellMatId = (cell != null && !cell.isEmpty()) ? cell.materialId() : 0;

                            int toRemove = Math.min(room, 64);
                            int removed = terrain.excavateMicrovoxelsAbove(targetPos, pt.y, toRemove);
                            if (removed > 0) {
                                totalExcavated += removed;
                                currentUnits += removed;
                                affected.add(targetPos);

                                if (currentMaterial == GranularMaterial.EMPTY && cellMatId != 0) {
                                    currentMaterial = GranularMaterialRegistry.byId(cellMatId);
                                }
                                if (currentMaterial == GranularMaterial.EMPTY) {
                                    currentMaterial = GranularMaterialRegistry.DIRT;
                                }
                            }
                        }
                    }
                }

                // B. Fill small depressions behind blade cutting edge
                if (currentUnits > 0 && currentMaterial != GranularMaterial.EMPTY) {
                    Vec3 rearPt = pt.subtract(currTransform.forward().scale(0.65D));
                    BlockPos fillPos = BlockPos.containing(rearPt.x, rearPt.y - 0.1D, rearPt.z);
                    if (!affected.contains(fillPos)) {
                        int fillQuota = Math.min(currentUnits, 16);
                        int filled = terrain.fillDepressionBelow(fillPos, pt.y, currentMaterial, fillQuota);
                        if (filled > 0) {
                            totalDeposited += filled;
                            currentUnits -= filled;
                            affected.add(fillPos);
                        }
                    }
                }
            }
        }

        // ── 2. Forward Berm Deposition (Active Pushing Forward) ────────────
        // When carrying material, actively push it ahead of the moldboard into a rolling berm
        if (currentUnits > 32 && currentMaterial != GranularMaterial.EMPTY) {
            int toPushAhead = Math.min(currentUnits, Math.max(16, currentUnits / 3));

            Vec3 frontCenter = currTransform.cuttingEdgeCenter().add(currTransform.forward().scale(0.85D));
            Vec3 frontLeft = currTransform.leftWingPoint().add(currTransform.forward().scale(0.85D)).add(currTransform.right().scale(0.5D));
            Vec3 frontRight = currTransform.rightWingPoint().add(currTransform.forward().scale(0.85D)).subtract(currTransform.right().scale(0.5D));

            BlockPos[] pushPositions = new BlockPos[]{
                    BlockPos.containing(frontCenter.x, frontCenter.y, frontCenter.z),
                    BlockPos.containing(frontLeft.x, frontLeft.y, frontLeft.z),
                    BlockPos.containing(frontRight.x, frontRight.y, frontRight.z)
            };

            int perPos = Math.max(1, toPushAhead / pushPositions.length);
            for (BlockPos pushPos : pushPositions) {
                if (currentUnits <= 0) break;
                int deposited = terrain.deposit(pushPos, currentMaterial, Math.min(currentUnits, perPos));
                if (deposited > 0) {
                    totalDeposited += deposited;
                    currentUnits -= deposited;
                    affected.add(pushPos);
                    terrain.markSimulate(pushPos);
                }
            }
        }

        // ── 3. Lateral Spill (Material escapes around blade left/right edges)
        // When blade has significant material or is overloaded, material spills around wings
        if (currentUnits > 96 && currentMaterial != GranularMaterial.EMPTY) {
            int overflow = currentUnits - 96;
            int spillPerSide = Math.min(24, Math.max(1, overflow / 6));

            // Left wing spill
            Vec3 leftSpillPt = currTransform.leftWingPoint().subtract(currTransform.right().scale(0.5D));
            BlockPos leftSpillPos = BlockPos.containing(leftSpillPt.x, leftSpillPt.y, leftSpillPt.z);
            int leftDeposited = terrain.deposit(leftSpillPos, currentMaterial, Math.min(currentUnits, spillPerSide));
            if (leftDeposited > 0) {
                totalDeposited += leftDeposited;
                currentUnits -= leftDeposited;
                affected.add(leftSpillPos);
                terrain.markSimulate(leftSpillPos);
            }

            // Right wing spill
            if (currentUnits > 96) {
                Vec3 rightSpillPt = currTransform.rightWingPoint().add(currTransform.right().scale(0.5D));
                BlockPos rightSpillPos = BlockPos.containing(rightSpillPt.x, rightSpillPt.y, rightSpillPt.z);
                int rightDeposited = terrain.deposit(rightSpillPos, currentMaterial, Math.min(currentUnits, spillPerSide));
                if (rightDeposited > 0) {
                    totalDeposited += rightDeposited;
                    currentUnits -= rightDeposited;
                    affected.add(rightSpillPos);
                    terrain.markSimulate(rightSpillPos);
                }
            }
        }

        GranularMaterial resultMaterial = currentMaterial;
        if (currentUnits <= 0) {
            currentUnits = 0;
            if (totalExcavated == 0 && totalDeposited == 0) {
                resultMaterial = GranularMaterial.EMPTY;
            }
        }

        boolean isPushing = totalExcavated > 0 || currentUnits > 32;

        return new BladeTickResult(
                totalExcavated,
                totalDeposited,
                currentUnits,
                resultMaterial,
                isPushing,
                new ArrayList<>(affected)
        );
    }
}
