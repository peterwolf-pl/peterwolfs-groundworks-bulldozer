package com.piotrek.groundworksbulldozer.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Differential crawler track kinematics and terrain-conforming suspension simulation.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Differential track steering allowing true in-place pivot turns when throttle is neutral.</li>
 *   <li>Weighted acceleration and mechanical deceleration curves.</li>
 *   <li>4-point ground contact sampling conforming vehicle pitch and roll to the terrain.</li>
 * </ul>
 */
public class BulldozerTrackController {

    public static final double MAX_SPEED = 0.13D;
    public static final double ACCELERATION = 0.045D;
    public static final double BRAKING = 0.075D;
    public static final double TRACK_GAUGE = 2.2D;
    public static final double TRACK_LENGTH = 3.4D;

    private float leftTrackSpeed = 0.0F;
    private float rightTrackSpeed = 0.0F;
    private float vehiclePitch = 0.0F;
    private float vehicleRoll = 0.0F;

    public record TrackState(
            float leftSpeed,
            float rightSpeed,
            Vec3 forwardDelta,
            float yawDeltaDegrees,
            float pitch,
            float roll
    ) {}

    public TrackState tick(
            ServerLevel level,
            Vec3 currentPos,
            float currentYaw,
            float throttleInput,
            float steerInput,
            boolean onGround,
            boolean underHeavyLoad
    ) {
        double maxSpeed = underHeavyLoad ? MAX_SPEED * 0.75D : MAX_SPEED;

        double targetLeft;
        double targetRight;

        if (Math.abs(throttleInput) < 0.01F && Math.abs(steerInput) > 0.01F) {
            // True differential in-place pivot turn:
            // Steer > 0 (turn right): Left track drives forward, Right track reverses
            targetLeft = steerInput * (maxSpeed * 0.85D);
            targetRight = -steerInput * (maxSpeed * 0.85D);
        } else if (Math.abs(throttleInput) > 0.01F) {
            // Forward/reverse driving with curvature steering
            targetLeft = (throttleInput + steerInput * 0.65D) * maxSpeed;
            targetRight = (throttleInput - steerInput * 0.65D) * maxSpeed;
        } else {
            targetLeft = 0.0D;
            targetRight = 0.0D;
        }

        targetLeft = Mth.clamp(targetLeft, -maxSpeed, maxSpeed);
        targetRight = Mth.clamp(targetRight, -maxSpeed, maxSpeed);

        leftTrackSpeed = approach(leftTrackSpeed, (float) targetLeft);
        rightTrackSpeed = approach(rightTrackSpeed, (float) targetRight);

        double avgForward = (leftTrackSpeed + rightTrackSpeed) * 0.5D;
        float yawDelta = (float) Math.toDegrees((leftTrackSpeed - rightTrackSpeed) / TRACK_GAUGE) * 1.5F;

        if (!onGround) {
            avgForward *= 0.5D;
            yawDelta *= 0.5F;
        }

        double yawRad = Math.toRadians(currentYaw);
        Vec3 forwardDelta = new Vec3(
                -Math.sin(yawRad) * avgForward,
                0.0D,
                Math.cos(yawRad) * avgForward
        );

        if (onGround && level != null) {
            sampleTerrainOrientation(level, currentPos, currentYaw);
        } else {
            vehiclePitch = Mth.lerp(0.1F, vehiclePitch, 0.0F);
            vehicleRoll = Mth.lerp(0.1F, vehicleRoll, 0.0F);
        }

        return new TrackState(
                leftTrackSpeed,
                rightTrackSpeed,
                forwardDelta,
                yawDelta,
                vehiclePitch,
                vehicleRoll
        );
    }

    private float approach(float current, float target) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001F) {
            return target;
        }
        float rate = (Math.abs(target) < 0.001F) ? (float) BRAKING : (float) ACCELERATION;
        if (diff > 0.0F) {
            return Math.min(current + rate, target);
        } else {
            return Math.max(current - rate, target);
        }
    }

    private void sampleTerrainOrientation(ServerLevel level, Vec3 basePos, float yaw) {
        double yawRad = Math.toRadians(yaw);
        Vec3 heading = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

        double halfGauge = TRACK_GAUGE * 0.5D;
        double halfLength = TRACK_LENGTH * 0.5D;

        double frontLeftY = sampleGroundHeight(level, basePos.add(heading.scale(halfLength)).subtract(right.scale(halfGauge)));
        double frontRightY = sampleGroundHeight(level, basePos.add(heading.scale(halfLength)).add(right.scale(halfGauge)));
        double rearLeftY = sampleGroundHeight(level, basePos.subtract(heading.scale(halfLength)).subtract(right.scale(halfGauge)));
        double rearRightY = sampleGroundHeight(level, basePos.subtract(heading.scale(halfLength)).add(right.scale(halfGauge)));

        double frontAvg = (frontLeftY + frontRightY) * 0.5D;
        double rearAvg = (rearLeftY + rearRightY) * 0.5D;
        double leftAvg = (frontLeftY + rearLeftY) * 0.5D;
        double rightAvg = (frontRightY + rearRightY) * 0.5D;

        double targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, TRACK_LENGTH));
        double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, TRACK_GAUGE));

        targetPitch = Mth.clamp(targetPitch, -30.0D, 30.0D);
        targetRoll = Mth.clamp(targetRoll, -20.0D, 20.0D);

        this.vehiclePitch = (float) Mth.lerp(0.2D, this.vehiclePitch, targetPitch);
        this.vehicleRoll = (float) Mth.lerp(0.2D, this.vehicleRoll, targetRoll);
    }

    private double sampleGroundHeight(ServerLevel level, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos.x, pos.y + 0.5D, pos.z);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos check = bp.below(dy);
            if (!level.getBlockState(check).isAir()) {
                return check.getY() + 1.0D;
            }
        }
        return pos.y;
    }

    public float leftTrackSpeed() {
        return leftTrackSpeed;
    }

    public float rightTrackSpeed() {
        return rightTrackSpeed;
    }

    public float vehiclePitch() {
        return vehiclePitch;
    }

    public float vehicleRoll() {
        return vehicleRoll;
    }

    public void setTrackSpeeds(float left, float right) {
        this.leftTrackSpeed = left;
        this.rightTrackSpeed = right;
    }

    public void setOrientation(float pitch, float roll) {
        this.vehiclePitch = pitch;
        this.vehicleRoll = roll;
    }
}
