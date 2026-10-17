package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BladeTransformKinematicsTest {

    @Test
    @DisplayName("BladeTransform computes accurate cutting edge span and world AABB")
    void testBladeGeometrySpan() {
        Vec3 base = new Vec3(10.0D, 64.0D, 10.0D);
        BladeTransform transform = BladeTransform.compute(base, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        assertNotNull(transform);
        assertNotNull(transform.boundingBox());
        assertEquals(BladeTransform.NUM_EDGE_SAMPLES, transform.cuttingEdgePoints().size());

        // Left and right wing tips must span across blade width (~3.0m)
        Vec3 leftWing = transform.leftWingPoint();
        Vec3 rightWing = transform.rightWingPoint();
        double span = leftWing.distanceTo(rightWing);

        assertEquals(BladeTransform.DEFAULT_BLADE_WIDTH, span, 1e-3D, "Cutting edge span must equal blade width 3.0m");

        // Cutting edge center is forward from vehicle center
        assertTrue(transform.cuttingEdgeCenter().z > base.z, "Cutting edge must be forward in heading direction");
        assertEquals(base.y, transform.cuttingEdgeY(), 1e-3D, "Zero blade height must place cutting edge at ground Y");
    }

    @Test
    @DisplayName("Blade height elevation strictly offsets cutting edge Y in world space")
    void testBladeHeightElevation() {
        Vec3 base = new Vec3(0.0D, 64.0D, 0.0D);

        BladeTransform lowered = BladeTransform.compute(base, 0.0F, 0.0F, 0.0F, -0.30F, 0.0F);
        BladeTransform raised = BladeTransform.compute(base, 0.0F, 0.0F, 0.0F, 0.50F, 0.0F);

        assertEquals(63.70D, lowered.cuttingEdgeY(), 1e-3D, "Lowered blade edge Y must be 64 - 0.30 = 63.70");
        assertEquals(64.50D, raised.cuttingEdgeY(), 1e-3D, "Raised blade edge Y must be 64 + 0.50 = 64.50");
        assertEquals(-0.30D, lowered.cuttingEdgeDepth(), 1e-3D);
        assertEquals(0.50D, raised.cuttingEdgeDepth(), 1e-3D);
        assertTrue(raised.isRaised());
        assertFalse(lowered.isRaised());
    }

    @Test
    @DisplayName("Nose-down pitch conforms cutting edge to the ramp slope and allows deep plunging")
    void noseDownPitchConformsToRampSlope() {
        Vec3 base = new Vec3(0.0D, 64.0D, 0.0D);
        float pitch = 20.0F; // 20 degrees nose-down ramp
        float bladeHeight = -0.60F;
        double armLength = 4.2D;
        BladeTransform noseDown = BladeTransform.compute(base, 0.0F, pitch, 0.0F, bladeHeight, 0.0F, 6.0D, 1.8D, armLength, 17);

        // Cutting edge follows the ramp: base.y - armLength * sin(20°) + cos(20°) * bladeHeight
        double expectedY = 64.0D - armLength * Math.sin(Math.toRadians(pitch)) + Math.cos(Math.toRadians(pitch)) * bladeHeight;
        assertEquals(expectedY, noseDown.cuttingEdgeY(), 1e-3D,
                "Cutting edge must follow the ramp slope and plunge deep into the incline");
        assertEquals(bladeHeight, noseDown.cuttingEdgeDepth(), 1e-3D);
        assertTrue(noseDown.cuttingEdgeY() < 64.0D,
                "Lowered blade on a downslope must cut into the ramp below chassis center");
    }

    @Test
    @DisplayName("Blade cutting edge on a slope matches projected track plane elevation")
    void bladeHeightMatchesProjectedTrackPlane() {
        Vec3 base = new Vec3(0.0D, 64.0D, 0.0D);
        float pitch = 15.0F;
        double armLength = 4.2D;
        BladeTransform flushBlade = BladeTransform.compute(
                base, 0.0F, pitch, 0.0F, 0.0F, 0.0F,
                6.0D, 1.8D, armLength, 17
        );

        // When bladeHeight is 0, cutting edge is exactly on the track plane on the slope
        double expectedFlushY = 64.0D - armLength * Math.sin(Math.toRadians(pitch));
        assertEquals(expectedFlushY, flushBlade.cuttingEdgeY(), 1e-3D,
                "Zero blade height must place the cutting edge flush with the slope");

        // When plunged to -0.80m, cutting edge digs 0.80m below the slope plane
        BladeTransform plunged = BladeTransform.compute(
                base, 0.0F, pitch, 0.0F, -0.80F, 0.0F,
                6.0D, 1.8D, armLength, 17
        );
        assertTrue(plunged.cuttingEdgeY() < flushBlade.cuttingEdgeY(),
                "Plunged blade must penetrate deeper than the flush slope surface");
    }

    @Test
    @DisplayName("Positive roll raises the left wing, matching left-high track convention")
    void positiveRollRaisesTheLeftWing() {
        BladeTransform rolled = BladeTransform.compute(new Vec3(0.0D, 64.0D, 0.0D), 0.0F, 0.0F, 10.0F, 0.0F, 0.0F);
        assertTrue(rolled.leftWingPoint().y > rolled.rightWingPoint().y);
    }
}
