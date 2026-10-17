package com.piotrek.groundworksbulldozer.vehicle;

import net.minecraft.util.Mth;

/**
 * One blade command becomes a straight ramp over the machine's own length, then the lever returns to zero.
 *
 * <p>Lowering the blade sets how deep the bite is. Driving consumes that offset: the cutting edge is
 * already part-way down the ramp (it sits an arm-length ahead of the tracks), and the tracks arrive
 * on the new grade only after the whole machine has traveled its length. A raise of the same amount
 * climbs back out the same way. A new lever movement on the slope starts another ramp.
 */
public final class GradePass {

    private GradePass() {
    }

    public static double chassisProgress(double traveled, double vehicleLength) {
        return Mth.clamp(traveled / Math.max(vehicleLength, 1.0E-4D), 0.0D, 1.0D);
    }

    /** The blade is ahead of the tracks, so it is further along the same ramp. */
    public static double bladeProgress(double traveled, double armLength, double vehicleLength) {
        return Mth.clamp((traveled + Math.max(0.0D, armLength)) / Math.max(vehicleLength, 1.0E-4D), 0.0D, 1.0D);
    }

    public static double elevation(double originY, float command, double progress) {
        return originY + command * progress;
    }

    public static float remainingHeight(float command, double chassisProgress) {
        return (float) (command * (1.0D - chassisProgress));
    }

    /** Angle between the ramp and the grade the pass started on. Zero once the machine is on the slope. */
    public static float biteDegrees(float command, double vehicleLength, double chassisProgress) {
        float full = (float) Math.toDegrees(Math.atan2(command, Math.max(vehicleLength, 1.0E-4D)));
        return full * (float) (1.0D - chassisProgress);
    }
}
