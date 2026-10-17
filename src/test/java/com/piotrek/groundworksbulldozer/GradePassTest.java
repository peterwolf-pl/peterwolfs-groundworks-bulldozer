package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.vehicle.GradePass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradePassTest {

    @Test
    @DisplayName("A lowered blade bites ahead of the tracks and levels after one machine length")
    void cutRampsOverVehicleLengthThenReturnsToZero() {
        double origin = 64.0D;
        float command = -0.60F;
        double length = 6.2D;
        double arm = 4.2D;

        assertEquals(origin, GradePass.elevation(origin, command, GradePass.chassisProgress(0.0D, length)), 1.0E-6D);
        assertEquals(origin + command * (arm / length),
                GradePass.elevation(origin, command, GradePass.bladeProgress(0.0D, arm, length)), 1.0E-4D);
        assertTrue(GradePass.biteDegrees(command, length, 0.0D) < -4.0F);

        assertEquals(0.0F, GradePass.remainingHeight(command, GradePass.chassisProgress(length, length)), 1.0E-4F);
        assertEquals(0.0F, GradePass.biteDegrees(command, length, 1.0D), 1.0E-4F);
        assertEquals(63.40D, GradePass.elevation(origin, command, 1.0D), 1.0E-4D);
    }

    @Test
    @DisplayName("Raising by the burial amount climbs back to zero over one machine length")
    void raiseStraightensOverVehicleLength() {
        double origin = 63.40D;
        float command = 0.60F;
        double length = 3.4D;

        assertEquals(64.00D, GradePass.elevation(origin, command, GradePass.chassisProgress(length, length)), 1.0E-4D);
        assertEquals(0.0F, GradePass.remainingHeight(command, 1.0D), 1.0E-4F);
        assertTrue(GradePass.biteDegrees(command, length, 0.0D) > 0.0F);
    }
}
