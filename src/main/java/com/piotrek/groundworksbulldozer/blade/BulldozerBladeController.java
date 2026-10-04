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
 *   <li>Front load retention: Excavated material remains pushed against the moldboard.</li>
 *   <li>Lateral spillage: Material escapes past the wings only after the blade reaches capacity.</li>
 *   <li>Groundworks relaxation: Marks modified cells for natural angle-of-repose settling.</li>
 * </ul>
 */
public final class BulldozerBladeController {

    public static final int MAX_BLADE_CAPACITY = 1536; // 3.0 full blocks of granular material (3.0 m³ - 2x capacity)
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

        // Check movement direction relative to blade orientation
        double forwardAlignment = motion.normalize().dot(currTransform.forward());

        // Reversing away from pushed material: leave the full carried load as a blade-width heap on the ground!
        if (forwardAlignment < -0.05D) {
            if (carriedUnitsBefore > 0 && carriedMaterial != GranularMaterial.EMPTY) {
                return depositReversingHeap(terrain, prevTransform, carriedUnitsBefore, carriedMaterial);
            }
            return new BladeTickResult(0, 0, 0, GranularMaterial.EMPTY, false, List.of());
        }

        if (forwardAlignment < 0.1D) {
            // Not moving forward into terrain (pure lateral drift or stationary)
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
                            GranularCell cell = terrain.getOrConvert(targetPos);
                            int cellMatId = (cell != null && !cell.isEmpty()) ? cell.materialId() : 0;

                            int toRemove = Math.min(room, 128);
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

            }
        }

        // ── 2. Lateral Spill ───────────────────────────────────────────────
        // The visible carried surcharge represents the rolling load in front of the moldboard.
        // Keep that load intact until the blade is physically full; only then can it escape
        // around the two wings.
        if (currentUnits >= MAX_BLADE_CAPACITY && currentMaterial != GranularMaterial.EMPTY) {
            int spillPerSide = 48;

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
            if (currentUnits > 0) {
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

    /**
     * Deposits the entire carried blade load into a blade-width heap on the ground when reversing.
     */
    private static BladeTickResult depositReversingHeap(
            IGranularTerrainAccess terrain,
            BladeTransform prevTransform,
            int carriedUnitsBefore,
            GranularMaterial carriedMaterial
    ) {
        int currentUnits = carriedUnitsBefore;
        int totalDeposited = 0;
        Set<BlockPos> affected = new HashSet<>();

        Vec3 forward = prevTransform.forward();
        Vec3 right = prevTransform.right();
        Vec3 center = prevTransform.cuttingEdgeCenter();

        // 5 points spanning the full 3.0m width of the blade
        Vec3 ptCenter = center.add(forward.scale(0.55D));
        Vec3 ptMidLeft = center.subtract(right.scale(0.75D)).add(forward.scale(0.45D));
        Vec3 ptMidRight = center.add(right.scale(0.75D)).add(forward.scale(0.45D));
        Vec3 ptOuterLeft = center.subtract(right.scale(1.35D)).add(forward.scale(0.35D));
        Vec3 ptOuterRight = center.add(right.scale(1.35D)).add(forward.scale(0.35D));

        Vec3[] heapPoints = new Vec3[]{ptCenter, ptMidLeft, ptMidRight, ptOuterLeft, ptOuterRight};
        double[] ratios = new double[]{0.32D, 0.24D, 0.24D, 0.10D, 0.10D};

        for (int i = 0; i < heapPoints.length && currentUnits > 0; i++) {
            Vec3 pt = heapPoints[i];
            BlockPos targetPos = BlockPos.containing(pt.x, pt.y, pt.z);

            int quota = (i == heapPoints.length - 1) ? currentUnits : (int) Math.round(carriedUnitsBefore * ratios[i]);
            quota = Math.min(currentUnits, Math.max(1, quota));

            int deposited = terrain.deposit(targetPos, carriedMaterial, quota);
            if (deposited > 0) {
                totalDeposited += deposited;
                currentUnits -= deposited;
                affected.add(targetPos);
                terrain.markSimulate(targetPos);
            }
        }

        // If any units remain due to column capacity, deposit remainder into center
        if (currentUnits > 0) {
            BlockPos centerPos = BlockPos.containing(ptCenter.x, ptCenter.y, ptCenter.z);
            int deposited = terrain.deposit(centerPos, carriedMaterial, currentUnits);
            totalDeposited += deposited;
            currentUnits -= deposited;
            affected.add(centerPos);
            terrain.markSimulate(centerPos);
        }

        return new BladeTickResult(
                0,
                totalDeposited,
                currentUnits,
                currentUnits > 0 ? carriedMaterial : GranularMaterial.EMPTY,
                false,
                new ArrayList<>(affected)
        );
    }
}
