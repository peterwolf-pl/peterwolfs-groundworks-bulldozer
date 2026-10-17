package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.vehicle.GradeDatum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeDatumTest {

    @Test
    @DisplayName("A lowered blade holds the cut plane while the tracks sit in the slot")
    void holdsSlotFloor() {
        double datum = GradeDatum.hold(70.0D, 69.40D, 69.40D, -0.60F);
        assertEquals(70.0D, datum, 1e-6D);
        assertEquals(69.40D, GradeDatum.edgeY(datum, 69.40D, -0.60F), 1e-6D);
    }

    @Test
    @DisplayName("Driving below the cut floor re-latches so the edge is not left in the air")
    void followsOffTheGrade() {
        // Video: lever at -0.60, chassis 1.45 m under the latch, edge reported +0.85 m above tracks.
        double stuck = 70.0D;
        double vehicleY = 68.55D;
        double released = GradeDatum.hold(stuck, vehicleY, vehicleY, -0.60F);
        assertEquals(vehicleY, released, 1e-6D);
        assertEquals(vehicleY - 0.60D, GradeDatum.edgeY(stuck, vehicleY, -0.60F), 1e-6D);
    }

    @Test
    @DisplayName("A raised blade reports clearance from the tracks, not a stale latch")
    void raisedIgnoresStaleLatch() {
        assertEquals(64.48D, GradeDatum.edgeY(63.0D, 64.0D, 0.48F), 1e-6D);
        assertEquals(64.0D, GradeDatum.hold(63.0D, 64.0D, 64.0D, 0.48F), 1e-6D);
    }
}
