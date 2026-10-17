package com.piotrek.groundworksbulldozer.vehicle;

import com.piotrek.groundworks.api.GroundworksApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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
    private static final float YAW_RESPONSE_SCALE = 0.50F;

    private final double maxSpeedConfig;
    private final double accelerationConfig;
    private final double brakingConfig;
    private final double trackGaugeConfig;
    private final double trackLengthConfig;
    private final float yawResponseScaleConfig;

    private float leftTrackSpeed = 0.0F;
    private float rightTrackSpeed = 0.0F;
    private float vehiclePitch = 0.0F;
    private float vehicleRoll = 0.0F;

    public BulldozerTrackController() {
        this(TRACK_GAUGE, TRACK_LENGTH, MAX_SPEED, ACCELERATION, BRAKING, YAW_RESPONSE_SCALE);
    }

    public BulldozerTrackController(
            double trackGauge,
            double trackLength,
            double maxSpeed,
            double acceleration,
            double braking,
            float yawResponseScale
    ) {
        this.trackGaugeConfig = trackGauge;
        this.trackLengthConfig = trackLength;
        this.maxSpeedConfig = maxSpeed;
        this.accelerationConfig = acceleration;
        this.brakingConfig = braking;
        this.yawResponseScaleConfig = yawResponseScale;
    }

    public double trackLength() {
        return this.trackLengthConfig;
    }

    public record TrackState(
            float leftSpeed,
            float rightSpeed,
            Vec3 forwardDelta,
            float yawDeltaDegrees,
            float pitch,
            float roll,
            double groundSupportY
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
        double maxSpeed = underHeavyLoad ? maxSpeedConfig * 0.75D : maxSpeedConfig;

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
        // Heavy bulldozer turntable response: keep differential track speeds and
        // straight-line travel unchanged, but reduce chassis yaw to one third of
        // the previous value so pivot turns feel appropriately slow and massive.
        float yawDelta = (float) Math.toDegrees(
                (leftTrackSpeed - rightTrackSpeed) / trackGaugeConfig
        ) * yawResponseScaleConfig;

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

        double groundSupportY = currentPos.y;
        if (level != null) {
            groundSupportY = sampleTerrainOrientation(level, currentPos, currentYaw);
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
                vehicleRoll,
                groundSupportY
        );
    }

    private float approach(float current, float target) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001F) {
            return target;
        }
        float rate = (Math.abs(target) < 0.001F) ? (float) brakingConfig : (float) accelerationConfig;
        if (diff > 0.0F) {
            return Math.min(current + rate, target);
        } else {
            return Math.max(current - rate, target);
        }
    }

    private double sampleTerrainOrientation(ServerLevel level, Vec3 basePos, float yaw) {
        double yawRad = Math.toRadians(yaw);
        Vec3 heading = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

        double halfGauge = trackGaugeConfig * 0.5D;
        double halfLength = trackLengthConfig * 0.5D;
        double maxAllowedGroundY = basePos.y + 2.5D;

        // Sample 6 track contact points: front, mid, rear across left and right tracks
        double frontLeftY = sampleGroundHeight(level, basePos.add(heading.scale(halfLength)).subtract(right.scale(halfGauge)), maxAllowedGroundY);
        double frontRightY = sampleGroundHeight(level, basePos.add(heading.scale(halfLength)).add(right.scale(halfGauge)), maxAllowedGroundY);

        double midLeftY = sampleGroundHeight(level, basePos.subtract(right.scale(halfGauge)), maxAllowedGroundY);
        double midRightY = sampleGroundHeight(level, basePos.add(right.scale(halfGauge)), maxAllowedGroundY);

        double rearLeftY = sampleGroundHeight(level, basePos.subtract(heading.scale(halfLength)).subtract(right.scale(halfGauge)), maxAllowedGroundY);
        double rearRightY = sampleGroundHeight(level, basePos.subtract(heading.scale(halfLength)).add(right.scale(halfGauge)), maxAllowedGroundY);

        // Crawler tracks conform up to ±45° slopes and step drops.
        // Scale the maximum contact drop to vehicle track length so heavy bulldozers (6.2m length)
        // and standard bulldozers can descend steep slopes, scarps, and embankments without
        // falsely clamping rear/front contact points to an artificially shallow floor.
        double maxContactDrop = Math.max(2.5D, halfLength * 0.75D + 1.5D);
        double supportFloor = basePos.y - maxContactDrop;
        frontLeftY = Math.max(frontLeftY, supportFloor);
        frontRightY = Math.max(frontRightY, supportFloor);
        midLeftY = Math.max(midLeftY, supportFloor);
        midRightY = Math.max(midRightY, supportFloor);
        rearLeftY = Math.max(rearLeftY, supportFloor);
        rearRightY = Math.max(rearRightY, supportFloor);

        double frontAvg = (frontLeftY + frontRightY) * 0.5D;
        double midAvg = (midLeftY + midRightY) * 0.5D;
        double rearAvg = (rearLeftY + rearRightY) * 0.5D;
        double leftAvg = (frontLeftY + midLeftY + rearLeftY) / 3.0D;
        double rightAvg = (frontRightY + midRightY + rearRightY) / 3.0D;

        // Real crawler pitch conforming to terrain supports:
        // Positive pitch = nose down, negative pitch = nose up
        double targetPitch;
        double avgEnds = (frontAvg + rearAvg) * 0.5D;
        if (midAvg > avgEnds + 0.20D) {
            // Teetering on a ridge/crest: pitch forward or back depending on front vs rear
            if (frontAvg < rearAvg) {
                targetPitch = Math.toDegrees(Math.atan2(midAvg - frontAvg, halfLength));
            } else {
                targetPitch = Math.toDegrees(Math.atan2(rearAvg - midAvg, halfLength));
            }
        } else {
            targetPitch = Math.toDegrees(Math.atan2(rearAvg - frontAvg, trackLengthConfig));
        }

        double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, trackGaugeConfig));

        targetPitch = Mth.clamp(targetPitch, -45.0D, 45.0D);
        targetRoll = Mth.clamp(targetRoll, -30.0D, 30.0D);

        this.vehiclePitch = (float) Mth.lerp(0.25D, this.vehiclePitch, targetPitch);
        this.vehicleRoll = (float) Mth.lerp(0.25D, this.vehicleRoll, targetRoll);

        // Ground support baseline: front and rear idler footprint define the continuous slope.
        // Single protruding incomplete blocks in the middle cannot launch the heavy machine up:
        double groundSupport = avgEnds;
        if (midAvg > avgEnds + 0.05D) {
            groundSupport = avgEnds + Math.min(0.15D, (midAvg - avgEnds) * 0.35D);
        }
        return groundSupport;
    }

    /**
     * Samples the exact world-space surface below a track contact point.
     *
     * <p>Groundworks terrain may occupy only part of a block, so treating every
     * non-air block as a full cube makes the dozer float and produces incorrect
     * pitch/roll. Search several blocks downward, preferring the public
     * Groundworks surface query and falling back to vanilla collision geometry.
     */
    public static double sampleGroundHeight(ServerLevel level, Vec3 pos) {
        return sampleGroundHeight(level, pos, pos.y + 2.0D);
    }

    public static double sampleGroundHeight(ServerLevel level, Vec3 pos, double maxAllowedGroundY) {
        BlockPos start = BlockPos.containing(pos.x, Math.min(pos.y + 0.5D, maxAllowedGroundY), pos.z);

        for (int dy = 0; dy <= 12; dy++) {
            BlockPos check = start.below(dy);

            double surfaceY = GroundworksApi.getSurfaceWorldY(
                    level, check, pos.x, pos.z);
            if (Double.isFinite(surfaceY)) {
                if (surfaceY <= maxAllowedGroundY) {
                    return surfaceY;
                }
            }

            BlockState state = level.getBlockState(check);
            if (!state.isAir()) {
                VoxelShape shape = state.getCollisionShape(level, check);
                if (!shape.isEmpty()) {
                    double top = check.getY() + shape.max(Direction.Axis.Y);
                    if (top <= maxAllowedGroundY) {
                        return top;
                    }
                }
                if (state.isSolid()) {
                    double top = check.getY() + 1.0D;
                    if (top <= maxAllowedGroundY) {
                        return top;
                    }
                }
            }
        }

        // No surface within the search. Hold the chassis datum instead of
        // commanding a 12 m fall — that fall is what pitched the dozer into its own cut.
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
