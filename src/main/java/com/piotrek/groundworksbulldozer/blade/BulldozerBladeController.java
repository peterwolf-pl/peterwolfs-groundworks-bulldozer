package com.piotrek.groundworksbulldozer.blade;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworksbulldozer.integration.groundworks.IGranularTerrainAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static final int MAX_BLADE_CAPACITY = 3072; // 6.0 full blocks of granular material (6.0 m³ - 6 blocks capacity)
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
        return tick(terrain, prevTransform, currTransform, carriedUnitsBefore, carriedMaterial, MAX_BLADE_CAPACITY);
    }

    /**
     * Executes one tick of blade physical interaction with Groundworks terrain.
     */
    public static BladeTickResult tick(
            IGranularTerrainAccess terrain,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            int carriedUnitsBefore,
            GranularMaterial carriedMaterial,
            int maxCapacity
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

                // A. Shave the full moldboard face, not a thin line of sample points.
                // Adjacent samples on a 6 m blade are ~0.375 m apart, so a column
                // sitting between them was previously left standing. Close that gap,
                // and also look one block ahead so the hull cannot climb uncut dirt.
                double bladeTopY = pt.y + currTransform.height();
                double[] along = {0.0D};
                double[] forwardOffsets = {0.0D, 0.30D};

                for (double alongT : along) {
                    Vec3 alongPt = pt;
                    for (double fwd : forwardOffsets) {
                        Vec3 samplePt = alongPt.add(currTransform.forward().scale(fwd));
                        BlockPos pos = BlockPos.containing(samplePt.x, samplePt.y, samplePt.z);
                        List<BlockPos> candidates = new ArrayList<>(3);
                        candidates.add(pos);
                        if (bladeTopY > pos.above().getY() + 0.05D) {
                            candidates.add(pos.above());
                        }
                        if (bladeTopY > pos.above(2).getY() + 0.05D) {
                            candidates.add(pos.above(2));
                        }
                        // Cutting edge inside the lower slice also shaves the cell immediately below
                        if (pt.y < pos.getY() + 0.20D) {
                            candidates.add(pos.below());
                        }

                        for (BlockPos targetPos : candidates) {
                            if (!processedBlocks.add(targetPos) || !terrain.isDiggable(targetPos)) {
                                continue;
                            }
                            GranularMaterial sourceMaterial = terrain.getMaterial(targetPos);
                            if (sourceMaterial == GranularMaterial.EMPTY) {
                                continue;
                            }

                            GranularMaterial requiredMaterial =
                                    currentMaterial == GranularMaterial.EMPTY
                                            ? sourceMaterial
                                            : currentMaterial;

                            // A blade carry buffer is single-material. Foreign terrain
                            // remains untouched until the current load is deposited.
                            if (sourceMaterial.id() != requiredMaterial.id()) {
                                continue;
                            }

                            // One full cell per sample. A 512 cap left the upper half
                            // of a block standing, which then solidified into a pillar.
                            ExcavationResult excavation = terrain.excavateMicrovoxelsAbove(
                                    targetPos,
                                    pt.y,
                                    4096,
                                    requiredMaterial
                            );
                            if (!excavation.success()) {
                                continue;
                            }
                            if (excavation.material().id() != requiredMaterial.id()) {
                                throw new IllegalStateException(
                                        "Groundworks material filter violation: required="
                                                + requiredMaterial.name()
                                                + ", removed=" + excavation.material().name()
                                );
                            }

                            int removed = excavation.unitsRemoved();
                            totalExcavated += removed;
                            affected.add(targetPos);
                            if (currentMaterial == GranularMaterial.EMPTY) {
                                currentMaterial = excavation.material();
                            }

                            // Keep the rolling load on the blade. Do not deposit it
                            // back into the cell that was just cut — depositWithOverflow
                            // stacks upward and that is the pillar in the video.
                            int room = Math.max(0, maxCapacity - currentUnits);
                            int kept = Math.min(room, removed);
                            currentUnits += kept;
                            int overflow = removed - kept;
                            if (overflow <= 0) {
                                continue;
                            }

                            int placed = depositAhead(
                                    terrain,
                                    currTransform,
                                    samplePt,
                                    bladeTopY,
                                    targetPos,
                                    currentMaterial,
                                    overflow,
                                    e
                            );
                            if (placed > 0) {
                                totalDeposited += placed;
                                overflow -= placed;
                            }
                            if (overflow > 0) {
                                currentUnits += overflow;
                            }
                        }
                    }
                }

            }
        }

        // ── 2. Grade Low Ground / Spread Layer Under Raised Blade ──────────
        // When not in full transport position (bladeHeight <= 0.65m), material carried in front of
        // the moldboard flows smoothly underneath the cutting edge, laying down a flat, even
        // layer/lift of soil equal to the clearance between the ground and the blade cutting edge.
        if (!currTransform.isTransport()
                && currentUnits > 0
                && currentMaterial != GranularMaterial.EMPTY) {

            Map<BlockPos, Double> gradingTargets = new LinkedHashMap<>();
            for (Vec3 pt : currEdge) {
                BlockPos gradePos = BlockPos.containing(
                        pt.x,
                        Math.nextDown(pt.y),
                        pt.z
                );

                // Do not fill under high ground that was just excavated or that has material above it
                if (affected.contains(gradePos) || affected.contains(gradePos.above())) {
                    continue;
                }
                if (terrain.getMaterial(gradePos.above()) != GranularMaterial.EMPTY) {
                    continue;
                }

                // Multiple edge samples can hit the same block. Use the lowest
                // blade point in that cell so grading never deposits above steel.
                gradingTargets.merge(gradePos, pt.y, Math::min);
            }

            for (Map.Entry<BlockPos, Double> entry : gradingTargets.entrySet()) {
                if (currentUnits <= 0) {
                    break;
                }

                BlockPos gradePos = entry.getKey();
                double targetWorldY = entry.getValue();

                int deposited = terrain.fillDepressionBelow(
                        gradePos,
                        targetWorldY,
                        currentMaterial,
                        currentUnits
                );

                if (deposited > 0) {
                    totalDeposited += deposited;
                    currentUnits -= deposited;
                    affected.add(gradePos);
                    terrain.markSimulate(gradePos);
                }
            }
        }

        // ── 3. Lateral Spill ───────────────────────────────────────────────
        // The visible carried surcharge represents the rolling load in front of the moldboard.
        // Keep that load intact until the blade is physically full; only then can it escape
        // around the two wings.
        if (currentUnits >= maxCapacity && currentMaterial != GranularMaterial.EMPTY) {
            int spillPerSide = maxCapacity >= 6000 ? 96 : 48;

            // Left wing spill
            Vec3 leftSpillPt = currTransform.leftWingPoint().subtract(currTransform.right().scale(0.5D));
            BlockPos leftSpillPos = BlockPos.containing(leftSpillPt.x, leftSpillPt.y, leftSpillPt.z);
            int leftDeposited = depositAtMostTwoHigh(
                    terrain, leftSpillPos, currentMaterial, Math.min(currentUnits, spillPerSide));
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
                int rightDeposited = depositAtMostTwoHigh(
                        terrain, rightSpillPos, currentMaterial, Math.min(currentUnits, spillPerSide));
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
            resultMaterial = GranularMaterial.EMPTY;
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
     * Puts overflow ahead of the moldboard as a low pryzma: at most two blocks
     * high, spread over two or three rows. Never back into the cell just cut.
     */
    private static int depositAhead(
            IGranularTerrainAccess terrain,
            BladeTransform transform,
            Vec3 samplePt,
            double bladeTopY,
            BlockPos cutPos,
            GranularMaterial material,
            int units,
            int edgeIndex
    ) {
        Set<BlockPos> affected = new HashSet<>();
        int placed = depositPryzma(
                terrain,
                transform.cuttingEdgeCenter(),
                transform.forward(),
                transform.right(),
                transform.width(),
                material,
                units,
                cutPos,
                affected
        );
        return placed;
    }

    /** Wing spill may fill the cell and one block above it. Never a tower. */
    private static int depositAtMostTwoHigh(
            IGranularTerrainAccess terrain,
            BlockPos pos,
            GranularMaterial material,
            int units
    ) {
        if (units <= 0) {
            return 0;
        }
        int placed = terrain.depositInCell(pos, material, units);
        int left = units - placed;
        if (left > 0) {
            placed += terrain.depositInCell(pos.above(), material, left);
        }
        return placed;
    }

    /**
     * A dozer spoil heap is a pad, not a wall. Two blocks is the ceiling.
     * A large load uses three rows; a smaller load still uses two, never one.
     */
    private static int depositPryzma(
            IGranularTerrainAccess terrain,
            Vec3 center,
            Vec3 forward,
            Vec3 right,
            double width,
            GranularMaterial material,
            int units,
            BlockPos avoid,
            Set<BlockPos> affected
    ) {
        if (units <= 0 || material == null || material == GranularMaterial.EMPTY) {
            return 0;
        }
        int columns = Math.max(3, (int) Math.round(width));
        int unitsPerRow = columns * 2 * 512;
        int rows = Math.max(2, (int) Math.ceil((double) units / Math.max(1, unitsPerRow)));
        double spacing = Math.max(0.9D, width / columns);

        List<BlockPos> bottoms = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (int row = 0; row < rows; row++) {
            double reach = 0.55D + row * 1.05D;
            for (int col = 0; col < columns; col++) {
                double side = (col - (columns - 1) * 0.5D) * spacing;
                Vec3 pt = center.add(forward.scale(reach)).add(right.scale(side));
                BlockPos base = BlockPos.containing(pt.x, pt.y, pt.z);
                if (avoid != null && base.equals(avoid)) {
                    base = new BlockPos(
                            base.getX() + (int) Math.round(forward.x),
                            base.getY(),
                            base.getZ() + (int) Math.round(forward.z)
                    );
                }
                if (avoid != null && base.equals(avoid)) {
                    continue;
                }
                if (seen.add(base)) {
                    bottoms.add(base);
                }
            }
        }
        if (bottoms.isEmpty()) {
            return 0;
        }

        int remaining = units;
        int placed = 0;
        for (int guard = 0; guard < 64 && remaining > 0; guard++) {
            int progress = 0;
            for (int layer = 0; layer < 2 && remaining > 0; layer++) {
                for (BlockPos base : bottoms) {
                    if (remaining <= 0) {
                        break;
                    }
                    BlockPos pos = layer == 0 ? base : base.above();
                    if (avoid != null && pos.equals(avoid)) {
                        continue;
                    }
                    int put = terrain.depositInCell(pos, material, Math.min(remaining, 128));
                    if (put <= 0) {
                        continue;
                    }
                    placed += put;
                    remaining -= put;
                    progress += put;
                    affected.add(pos.immutable());
                    terrain.markSimulate(pos);
                }
            }
            if (progress == 0) {
                break;
            }
        }
        return placed;
    }

    /**
     * Leaves the carried load as a low pryzma: at most two blocks high,
     * two rows for a normal load and three when the load would not fit.
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

        double width = prevTransform.width();

        int piled = depositPryzma(
                terrain,
                center,
                forward,
                right,
                width,
                carriedMaterial,
                currentUnits,
                null,
                affected
        );
        totalDeposited += piled;
        currentUnits -= piled;

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
