package com.piotrek.groundworksbulldozer.tree;

import com.piotrek.groundworks.api.tree.GroundworksTreeApi;
import com.piotrek.groundworks.api.tree.GroundworksTreeApi.GroundworksTreeInfo;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.entity.HeavyBulldozerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative controller for bulldozer tree felling and fallen log pushing.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Instantaneous shredding and clearing of tree foliage as the 3.0m blade plows through canopies.</li>
 *   <li>Physical tree toppling: Bulldozer tracks generate massive tractive force; driving the blade
 *       into a standing trunk exerts forward torque, snapping the base and felling the tree away
 *       in the direction of vehicle travel.</li>
 *   <li>Fallen log clearing: Driving into horizontal/fallen logs slides and pushes them forward across
 *       terrain, stepping up obstacles or falling down ruts, opening roads through dense forest.</li>
 * </ul>
 */
public final class BulldozerTreeController {

    public static final int MAX_LEAVES_PER_TICK = 32;
    public static final double CONTACT_PROXIMITY_RADIUS = 1.05D;

    private BulldozerTreeController() {}

    /**
     * Executes leaf destruction, tree felling, and fallen log pushing for one server tick.
     */
    public static void tick(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        if (prevTransform == null || currTransform == null) {
            return;
        }

        AABB bladeBox = computeSweptBladeBox(prevTransform, currTransform);

        // 1. Shred leaves intersected by the blade moldboard
        smashLeaves(level, bulldozer, bladeBox, currTransform, driver);

        // 2. Process physical tree felling and fallen log pushing
        processTreeAndLogInteractions(level, bulldozer, bladeBox, prevTransform, currTransform, driver);
    }

    /**
     * Shreds and clears tree leaves in the path of the bulldozer blade.
     */
    private static void smashLeaves(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            AABB bladeBox,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        int minX = (int) Math.floor(bladeBox.minX);
        int minY = (int) Math.floor(bladeBox.minY);
        int minZ = (int) Math.floor(bladeBox.minZ);
        int maxX = (int) Math.ceil(bladeBox.maxX);
        int maxY = (int) Math.ceil(bladeBox.maxY);
        int maxZ = (int) Math.ceil(bladeBox.maxZ);

        Entity breaker = driver != null ? driver : bulldozer;
        int destroyed = 0;
        BlockPos lastLeaf = null;

        for (int x = minX; x <= maxX && destroyed < MAX_LEAVES_PER_TICK; x++) {
            for (int y = minY; y <= maxY && destroyed < MAX_LEAVES_PER_TICK; y++) {
                for (int z = minZ; z <= maxZ && destroyed < MAX_LEAVES_PER_TICK; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.LEAVES)) {
                        continue;
                    }

                    if (isPointNearBlade(Vec3.atCenterOf(pos), currTransform, CONTACT_PROXIMITY_RADIUS)) {
                        level.destroyBlock(pos, true, breaker);
                        destroyed++;
                        lastLeaf = pos;
                    }
                }
            }
        }

        if (destroyed > 0 && lastLeaf != null) {
            level.playSound(
                    null,
                    lastLeaf,
                    SoundEvents.AZALEA_LEAVES_BREAK,
                    SoundSource.BLOCKS,
                    1.2F,
                    0.9F + (float) (Math.random() * 0.2F)
            );
        }
    }

    /**
     * Detects logs in the blade path and applies felling or pushing physics.
     */
    private static void processTreeAndLogInteractions(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            AABB bladeBox,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        BlockPos contactLogPos = findContactLog(level, bladeBox, currTransform);
        if (contactLogPos == null) {
            if (bulldozer.tickCount - bulldozer.getLastTreePushTick() > 10) {
                bulldozer.setTreePushStrain(0.0F);
                bulldozer.setPushedTreeBase(null);
                bulldozer.setPushingTree(false);
            }
            return;
        }

        // Check standing tree first
        GroundworksTreeInfo standingTree = GroundworksTreeApi.detectTree(level, contactLogPos);
        if (standingTree != null) {
            processStandingTreeFelling(level, bulldozer, contactLogPos, standingTree, prevTransform, currTransform, driver);
            return;
        }

        // Check fallen / horizontal log
        if (GroundworksTreeApi.isFallenLog(level, contactLogPos)) {
            processFallenLogPushing(level, bulldozer, contactLogPos, prevTransform, currTransform, driver);
            return;
        }

        // Fallback: solitary vertical stump
        processStumpPushing(level, bulldozer, contactLogPos, prevTransform, currTransform, driver);
    }

    /**
     * Topples standing trees under sustained tractive force from bulldozer tracks.
     */
    private static void processStandingTreeFelling(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            BlockPos contactLogPos,
            GroundworksTreeInfo tree,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        BlockPos treeBase = tree.base();
        if (bulldozer.getPushedTreeBase() == null || !bulldozer.getPushedTreeBase().equals(treeBase)) {
            bulldozer.setPushedTreeBase(treeBase);
            bulldozer.setTreePushStrain(0.0F);
        }

        float effort = calculatePushEffort(bulldozer, prevTransform, currTransform);
        if (effort < 0.04F) {
            return;
        }

        double contactY = contactLogPos.getY() + 0.5D;
        double treeBaseY = treeBase.getY();
        double height = Math.max(0.5D, contactY - treeBaseY);

        // Leverage: M = F * h
        float leverage = Math.max(0.4F, (float) (height / 1.5D));
        if (bulldozer instanceof HeavyBulldozerEntity) {
            leverage *= 1.5F;
        }
        float deltaStrain = effort * leverage;

        float currentStrain = bulldozer.getTreePushStrain() + deltaStrain;
        bulldozer.setTreePushStrain(currentStrain);
        bulldozer.setPushingTree(true);
        bulldozer.setLastTreePushTick(bulldozer.tickCount);

        if (bulldozer.tickCount % 3 == 0) {
            level.playSound(
                    null,
                    contactLogPos,
                    SoundEvents.WOOD_HIT,
                    SoundSource.BLOCKS,
                    1.2F,
                    0.80F + (float) (Math.random() * 0.15F)
            );
            spawnWoodParticles(level, contactLogPos);
        }

        float requiredThreshold = GroundworksTreeApi.calculateBreakThreshold(tree.height());
        if (currentStrain >= requiredThreshold) {
            // SNAP and topple tree forward along bulldozer heading
            Direction fallDirection = getNearestHorizontal(currTransform.forward().x, currTransform.forward().z);

            ServerPlayer player = driver instanceof ServerPlayer sp ? sp : null;
            GroundworksTreeApi.fellTree(level, player, tree, fallDirection);

            level.playSound(
                    null,
                    contactLogPos,
                    SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS,
                    1.8F,
                    0.85F
            );

            bulldozer.setTreePushStrain(0.0F);
            bulldozer.setPushedTreeBase(null);
            bulldozer.setPushingTree(false);
        }
    }

    /**
     * Pushes fallen logs ahead of the bulldozer blade across terrain.
     */
    private static void processFallenLogPushing(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            BlockPos contactLogPos,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        float effort = calculatePushEffort(bulldozer, prevTransform, currTransform);
        if (effort < 0.03F) {
            return;
        }

        float rate = bulldozer instanceof HeavyBulldozerEntity ? 1.20F : 0.60F;
        float strain = bulldozer.getTreePushStrain() + effort * rate;
        bulldozer.setTreePushStrain(strain);
        bulldozer.setPushingTree(true);
        bulldozer.setLastTreePushTick(bulldozer.tickCount);

        if (bulldozer.tickCount % 4 == 0) {
            level.playSound(
                    null,
                    contactLogPos,
                    SoundEvents.WOOD_HIT,
                    SoundSource.BLOCKS,
                    0.9F,
                    0.80F + (float) (Math.random() * 0.15F)
            );
            spawnWoodParticles(level, contactLogPos);
        }

        float threshold = bulldozer instanceof HeavyBulldozerEntity ? 0.08F : 0.20F;
        if (strain >= threshold) {
            Direction pushDir = getNearestHorizontal(currTransform.forward().x, currTransform.forward().z);
            GroundworksTreeApi.pushFallenLog(level, contactLogPos, pushDir, bulldozer);
            bulldozer.setTreePushStrain(0.0F);
        }
    }

    /**
     * Uproots remaining solitary stumps under heavy bulldozer pushing.
     */
    private static void processStumpPushing(
            ServerLevel level,
            GroundworksBulldozerEntity bulldozer,
            BlockPos stumpPos,
            BladeTransform prevTransform,
            BladeTransform currTransform,
            @Nullable Entity driver
    ) {
        float effort = calculatePushEffort(bulldozer, prevTransform, currTransform);
        if (effort < 0.05F) {
            return;
        }

        float rate = bulldozer instanceof HeavyBulldozerEntity ? 1.0F : 0.40F;
        float strain = bulldozer.getTreePushStrain() + effort * rate;
        bulldozer.setTreePushStrain(strain);
        bulldozer.setPushingTree(true);
        bulldozer.setLastTreePushTick(bulldozer.tickCount);

        float req = bulldozer instanceof HeavyBulldozerEntity
                ? GroundworksTreeApi.BASE_BREAK_THRESHOLD * 0.7F
                : GroundworksTreeApi.BASE_BREAK_THRESHOLD * 1.2F;
        if (strain >= req) {
            Entity breaker = driver != null ? driver : bulldozer;
            level.destroyBlock(stumpPos, true, breaker);
            bulldozer.setTreePushStrain(0.0F);
            bulldozer.setPushedTreeBase(null);
            bulldozer.setPushingTree(false);
        }
    }

    /**
     * Calculates push effort exerted by crawler tracks and blade forward speed.
     */
    public static float calculatePushEffort(
            GroundworksBulldozerEntity bulldozer,
            BladeTransform prevTransform,
            BladeTransform currTransform
    ) {
        float effort = 0.0F;

        // Throttle input (W key forward drive)
        effort += Math.max(0.0F, bulldozer.getInputThrottle()) * 1.2F;

        // Actual track speed
        effort += bulldozer.getAverageTrackSpeed() * 5.0F;

        // Dynamic motion of the blade cutting edge
        Vec3 motion = currTransform.cuttingEdgeCenter().subtract(prevTransform.cuttingEdgeCenter());
        double forwardDist = motion.dot(currTransform.forward());
        if (forwardDist > 0.01D) {
            effort += (float) (forwardDist * 10.0D);
        }

        if (bulldozer instanceof HeavyBulldozerEntity) {
            effort *= 3.0F;
        }

        return Math.min(effort, bulldozer instanceof HeavyBulldozerEntity ? 12.0F : 4.0F);
    }

    /**
     * Maps horizontal (dx, dz) force vector to the nearest cardinal Direction.
     */
    public static Direction getNearestHorizontal(double dx, double dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0.0D ? Direction.EAST : Direction.WEST;
        } else {
            return dz >= 0.0D ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private static @Nullable BlockPos findContactLog(
            ServerLevel level,
            AABB bladeBox,
            BladeTransform currTransform
    ) {
        int minX = (int) Math.floor(bladeBox.minX);
        int minY = (int) Math.floor(bladeBox.minY);
        int minZ = (int) Math.floor(bladeBox.minZ);
        int maxX = (int) Math.ceil(bladeBox.maxX);
        int maxY = (int) Math.ceil(bladeBox.maxY);
        int maxZ = (int) Math.ceil(bladeBox.maxZ);

        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
                        continue;
                    }

                    Vec3 center = Vec3.atCenterOf(pos);
                    if (isPointNearBlade(center, currTransform, CONTACT_PROXIMITY_RADIUS)) {
                        double dSq = center.distanceToSqr(currTransform.cuttingEdgeCenter());
                        if (dSq < closestDistSq) {
                            closestDistSq = dSq;
                            closest = pos;
                        }
                    }
                }
            }
        }
        return closest;
    }

    private static boolean isPointNearBlade(Vec3 point, BladeTransform transform, double radius) {
        double rSq = radius * radius;
        if (point.distanceToSqr(transform.cuttingEdgeCenter()) <= rSq) return true;
        if (point.distanceToSqr(transform.center()) <= rSq) return true;

        for (Vec3 edgePt : transform.cuttingEdgePoints()) {
            if (point.distanceToSqr(edgePt) <= rSq) return true;
        }

        // Check if inside inflated bounding box
        return transform.boundingBox().inflate(radius * 0.4D).contains(point);
    }

    private static AABB computeSweptBladeBox(BladeTransform prev, BladeTransform curr) {
        List<Vec3> points = new ArrayList<>();
        points.add(prev.center());
        points.add(prev.cuttingEdgeCenter());
        points.addAll(prev.cuttingEdgePoints());

        points.add(curr.center());
        points.add(curr.cuttingEdgeCenter());
        points.addAll(curr.cuttingEdgePoints());

        // Also add forward envelope point
        points.add(curr.cuttingEdgeCenter().add(curr.forward().scale(0.8D)));

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Vec3 pt : points) {
            minX = Math.min(minX, pt.x);
            minY = Math.min(minY, pt.y);
            minZ = Math.min(minZ, pt.z);
            maxX = Math.max(maxX, pt.x);
            maxY = Math.max(maxY, pt.y);
            maxZ = Math.max(maxZ, pt.z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(0.4D);
    }

    private static void spawnWoodParticles(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                8,
                0.2D, 0.2D, 0.2D,
                0.05D
        );
    }
}
