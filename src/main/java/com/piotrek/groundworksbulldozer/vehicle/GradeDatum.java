package com.piotrek.groundworksbulldozer.vehicle;

/**
 * World elevation the lowered blade holds while the tracks settle into the slot.
 *
 * <p>The latch must not outlive the grade. If the chassis or the ground under the
 * tracks drops past the cut floor, the machine has left that pass. The blade then
 * follows the tracks again. Otherwise a deep-cut lever planes empty air and the
 * next contact bites a whole bank.
 */
public final class GradeDatum {

    /** How far below the cut floor still counts as sitting in the slot. */
    public static final double SLOT_MARGIN = 0.25D;

    /**
     * A larger gap is a stale latch, not a settled slot. The slot is only as deep
     * as the blade setting (0.60 m).
     */
    public static final double STALE_GAP = 1.05D;

    private GradeDatum() {
    }

    public static double hold(double datum, double vehicleY, double groundSupportY, float bladeHeight) {
        boolean bladeDown = bladeHeight < -0.02F;
        if (Double.isNaN(datum) || !bladeDown || vehicleY > datum + 0.02D) {
            return vehicleY;
        }
        double cutFloor = datum + bladeHeight;
        boolean leftTheGrade = vehicleY < cutFloor - SLOT_MARGIN
                || groundSupportY < cutFloor - SLOT_MARGIN;
        return leftTheGrade ? vehicleY : datum;
    }

    /** Cutting-edge world Y the HUD should report. A stale latch must not invent clearance. */
    public static double edgeY(double datum, double vehicleY, float bladeHeight) {
        if (bladeHeight >= -0.02F || Double.isNaN(datum) || Math.abs(datum - vehicleY) > STALE_GAP) {
            return vehicleY + bladeHeight;
        }
        return datum + bladeHeight;
    }
}
