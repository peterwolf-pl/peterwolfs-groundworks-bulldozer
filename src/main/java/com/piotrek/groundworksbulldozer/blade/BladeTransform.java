package com.piotrek.groundworksbulldozer.blade;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * World-space physical transform, collision geometry, and cutting edge representation of the bulldozer blade.
 *
 * <p>Stores the exact 3D pose of the moldboard and bottom cutting edge.
 * Provides discrete sampling points along the cutting edge for swept volumetric terrain grading.
 */
public record BladeTransform(
        Vec3 center,
        Vec3 cuttingEdgeCenter,
        Vec3 forward,
        Vec3 right,
        Vec3 up,
        double width,
        double height,
        double bladeHeight,
        double bladeAngle,
        List<Vec3> cuttingEdgePoints,
        AABB boundingBox
) {

    public static final double DEFAULT_BLADE_WIDTH = 3.0D;
    public static final double DEFAULT_BLADE_HEIGHT = 0.9D;
    public static final double MOUNT_FORWARD_OFFSET = 2.1D;
    public static final double PIVOT_HEIGHT = 0.45D;
    public static final int NUM_EDGE_SAMPLES = 9;

    public BladeTransform {
        cuttingEdgePoints = Collections.unmodifiableList(new ArrayList<>(cuttingEdgePoints));
    }

    /**
     * Computes the blade's world-space transform from vehicle pose and blade controls.
     *
     * @param vehiclePos   World position of vehicle (ground level under center)
     * @param vehicleYaw   Vehicle yaw in degrees
     * @param vehiclePitch Vehicle pitch in degrees
     * @param vehicleRoll  Vehicle roll in degrees
     * @param bladeHeight  Continuous vertical offset of cutting edge relative to ground (-0.60 .. +0.80)
     * @param bladeAngle   Blade tilt / angle in degrees (-15 .. +25)
     * @return World-space BladeTransform
     */
    public static BladeTransform compute(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float vehicleRoll,
            float bladeHeight,
            float bladeAngle
    ) {
        double yawRad = Math.toRadians(vehicleYaw);
        double pitchRad = Math.toRadians(vehiclePitch);
        double rollRad = Math.toRadians(vehicleRoll);

        // Vehicle base directional vectors (facing where tracks drive)
        // In Minecraft: 0 deg = South (+Z), 90 deg = West (-X), 180 deg = North (-Z), 270 deg = East (+X)
        // Standard forward vector: (-sin(yaw), 0, cos(yaw))
        Vec3 baseForward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 baseRight = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 baseUp = new Vec3(0.0D, 1.0D, 0.0D);

        // Apply vehicle pitch & roll to orientation vectors
        Vec3 forward = rotateVector(baseForward, baseRight, -pitchRad);
        forward = rotateVector(forward, baseForward, rollRad).normalize();

        Vec3 right = rotateVector(baseRight, baseForward, rollRad);
        right = rotateVector(right, baseRight, -pitchRad).normalize();

        Vec3 up = forward.cross(right).normalize();

        // Push arm kinematics: blade center is located forward from vehicle trunnion mount
        // As blade raises, push arms swing in an arc
        double armLength = MOUNT_FORWARD_OFFSET;
        double effectiveElevation = bladeHeight;

        // Cutting edge center in world space
        Vec3 cuttingEdgeCenter = vehiclePos
                .add(forward.scale(armLength))
                .add(up.scale(effectiveElevation));

        // Blade pitch rotation (tilted around blade right axis)
        double totalBladePitch = Math.toRadians(vehiclePitch + bladeAngle);
        Vec3 bladeUp = rotateVector(up, right, totalBladePitch).normalize();
        Vec3 bladeForward = right.cross(bladeUp).normalize();

        // Moldboard geometric center is half height above cutting edge
        Vec3 bladeCenter = cuttingEdgeCenter.add(bladeUp.scale(DEFAULT_BLADE_HEIGHT * 0.5D));

        // Sample points along the cutting edge from left wing (-W/2) to right wing (+W/2)
        List<Vec3> edgePoints = new ArrayList<>(NUM_EDGE_SAMPLES);
        double halfWidth = DEFAULT_BLADE_WIDTH * 0.5D;
        for (int i = 0; i < NUM_EDGE_SAMPLES; i++) {
            double fraction = (double) i / (NUM_EDGE_SAMPLES - 1); // 0.0 .. 1.0
            double lateral = -halfWidth + (fraction * DEFAULT_BLADE_WIDTH); // -halfWidth .. +halfWidth
            Vec3 pt = cuttingEdgeCenter.add(right.scale(lateral));
            edgePoints.add(pt);
        }

        // Compute world-space AABB enclosing the entire blade moldboard and cutting edge
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        // Include bottom cutting edge points and top moldboard edge points
        for (Vec3 pt : edgePoints) {
            Vec3 topPt = pt.add(bladeUp.scale(DEFAULT_BLADE_HEIGHT));
            Vec3 frontPt = pt.add(bladeForward.scale(0.35D));
            Vec3 rearPt = pt.subtract(bladeForward.scale(0.35D));

            for (Vec3 corner : List.of(pt, topPt, frontPt, rearPt)) {
                minX = Math.min(minX, corner.x);
                minY = Math.min(minY, corner.y);
                minZ = Math.min(minZ, corner.z);
                maxX = Math.max(maxX, corner.x);
                maxY = Math.max(maxY, corner.y);
                maxZ = Math.max(maxZ, corner.z);
            }
        }

        AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        return new BladeTransform(
                bladeCenter,
                cuttingEdgeCenter,
                bladeForward,
                right,
                bladeUp,
                DEFAULT_BLADE_WIDTH,
                DEFAULT_BLADE_HEIGHT,
                bladeHeight,
                bladeAngle,
                edgePoints,
                box
        );
    }

    /**
     * Left wing tip of the blade cutting edge.
     */
    public Vec3 leftWingPoint() {
        return cuttingEdgePoints.getFirst();
    }

    /**
     * Right wing tip of the blade cutting edge.
     */
    public Vec3 rightWingPoint() {
        return cuttingEdgePoints.getLast();
    }

    /**
     * World Y of the lowest point of the cutting edge.
     */
    public double cuttingEdgeY() {
        return cuttingEdgeCenter.y;
    }

    /**
     * Returns true if the cutting edge is raised above the threshold where terrain interaction occurs.
     */
    public boolean isRaised() {
        return bladeHeight > 0.05D;
    }

    /**
     * Rotates a vector around an axis by a given angle in radians (Rodrigues' rotation formula).
     */
    private static Vec3 rotateVector(Vec3 v, Vec3 axis, double angleRad) {
        Vec3 k = axis.normalize();
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return v.scale(cos)
                .add(k.cross(v).scale(sin))
                .add(k.scale(k.dot(v) * (1.0D - cos)));
    }
}
