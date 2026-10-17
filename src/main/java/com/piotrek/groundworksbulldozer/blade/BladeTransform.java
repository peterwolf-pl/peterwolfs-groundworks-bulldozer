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
        double cuttingEdgeDepth,
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
     * @param vehiclePitch Vehicle pitch in degrees. Positive is nose down, matching the track controller.
     * @param vehicleRoll  Vehicle roll in degrees. Positive means the left side is higher.
     * @param bladeHeight  Signed depth of the cutting edge relative to the track plane (-0.60 .. +0.80)
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
        return compute(
                vehiclePos,
                vehicleYaw,
                vehiclePitch,
                vehicleRoll,
                bladeHeight,
                bladeAngle,
                DEFAULT_BLADE_WIDTH,
                DEFAULT_BLADE_HEIGHT,
                MOUNT_FORWARD_OFFSET,
                NUM_EDGE_SAMPLES
        );
    }

    /**
     * Computes the blade's world-space transform with customizable blade geometry.
     */
    public static BladeTransform compute(
            Vec3 vehiclePos,
            float vehicleYaw,
            float vehiclePitch,
            float vehicleRoll,
            float bladeHeight,
            float bladeAngle,
            double bladeWidth,
            double bladeHeightDim,
            double armLength,
            int numSamples
    ) {
        double yawRad = Math.toRadians(vehicleYaw);
        double rollRad = Math.toRadians(vehicleRoll);
        // vehiclePitch is accepted so callers can pass the live attitude, but the
        // cut plane does not use it. See the cutting-edge comment below.

        // Vehicle base directional vectors (facing where tracks drive)
        // In Minecraft: 0 deg = South (+Z), 90 deg = West (-X), 180 deg = North (-Z), 270 deg = East (+X)
        // Standard forward vector: (-sin(yaw), 0, cos(yaw))
        Vec3 baseForward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 baseRight = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 baseUp = new Vec3(0.0D, 1.0D, 0.0D);

        // Cut elevation is the chassis datum plus the operator's blade height.
        // Vehicle pitch must not move this point. A 4.2 m arm at 40° nose-down
        // drops the edge 2.7 m, the tracks fall into that hole, and the cut runs away.
        Vec3 cuttingEdgeCenter = new Vec3(
                vehiclePos.x + baseForward.x * armLength,
                vehiclePos.y + bladeHeight,
                vehiclePos.z + baseForward.z * armLength
        );

        // Roll still tips the wings, but a hard cap stops one side opening a trench.
        double cappedRoll = Mth.clamp(rollRad, Math.toRadians(-6.0D), Math.toRadians(6.0D));
        Vec3 right = rotateVector(baseRight, baseForward, -cappedRoll).normalize();
        Vec3 up = baseUp;

        // Moldboard faces along the machine. Operator blade angle tips it.
        // Vehicle pitch is not added — that aims the berm into the floor.
        double bladePitch = Math.toRadians(bladeAngle);
        Vec3 bladeUp = rotateVector(up, right, bladePitch).normalize();
        Vec3 bladeForward = right.cross(bladeUp).normalize();

        // Moldboard geometric center is half height above cutting edge
        Vec3 bladeCenter = cuttingEdgeCenter.add(bladeUp.scale(bladeHeightDim * 0.5D));

        // Sample points along the cutting edge from left wing (-W/2) to right wing (+W/2)
        int samples = Math.max(2, numSamples);
        List<Vec3> edgePoints = new ArrayList<>(samples);
        double halfWidth = bladeWidth * 0.5D;
        for (int i = 0; i < samples; i++) {
            double fraction = (double) i / (samples - 1); // 0.0 .. 1.0
            double lateral = -halfWidth + (fraction * bladeWidth); // -halfWidth .. +halfWidth
            Vec3 pt = cuttingEdgeCenter.add(right.scale(lateral));
            edgePoints.add(pt);
        }

        // Depth against the horizontal chassis datum. Pitch is not part of this number.
        double cuttingEdgeDepth = Double.POSITIVE_INFINITY;
        for (Vec3 pt : edgePoints) {
            cuttingEdgeDepth = Math.min(cuttingEdgeDepth, pt.y - vehiclePos.y);
        }

        // Compute world-space AABB enclosing the entire blade moldboard and cutting edge
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        // Include bottom cutting edge points and top moldboard edge points
        for (Vec3 pt : edgePoints) {
            Vec3 topPt = pt.add(bladeUp.scale(bladeHeightDim));
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
                bladeWidth,
                bladeHeightDim,
                bladeHeight,
                bladeAngle,
                cuttingEdgeDepth,
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
     * Returns true if the blade is in full transport position (raised high for road travel without grading).
     */
    public boolean isTransport() {
        return bladeHeight > 0.65D;
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
