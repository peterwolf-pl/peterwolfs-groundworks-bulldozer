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
    @DisplayName("Nose-down pitch must not drop the cutting edge into a crater")
    void noseDownPitchDoesNotDeepenTheCut() {
        Vec3 base = new Vec3(0.0D, 64.0D, 0.0D);
        BladeTransform noseDown = BladeTransform.compute(base, 0.0F, 40.0F, 0.0F, -0.60F, 0.0F, 6.0D, 1.8D, 4.2D, 17);

        assertEquals(63.40D, noseDown.cuttingEdgeY(), 1e-3D,
                "40 degree nose-down must not add armLength * sin(pitch) to the cut");
        assertEquals(-0.60D, noseDown.cuttingEdgeDepth(), 1e-3D);
        assertTrue(noseDown.forward().y > -0.05D,
                "Moldboard must not aim the berm into the floor");
    }

    @Test
    @DisplayName("Blade height on a pitched chassis stays a fixed world cut")
    void bladeHeightIsDepthRelativeToTheChassisDatum() {
        BladeTransform cut = BladeTransform.compute(
                new Vec3(0.0D, 64.0D, 0.0D),
                0.0F, 12.0F, 0.0F, -0.30F, 0.0F,
                6.0D, 1.8D, 4.2D, 17
        );

        assertEquals(-0.30D, cut.cuttingEdgeDepth(), 1e-3D);
        assertEquals(63.70D, cut.cuttingEdgeY(), 1e-3D,
                "Pitch must not move a 4.2 m arm off the chassis datum");
    }

    @Test
    @DisplayName("Positive roll raises the left wing, matching left-high track convention")
    void positiveRollRaisesTheLeftWing() {
        BladeTransform rolled = BladeTransform.compute(new Vec3(0.0D, 64.0D, 0.0D), 0.0F, 0.0F, 10.0F, 0.0F, 0.0F);
        assertTrue(rolled.leftWingPoint().y > rolled.rightWingPoint().y);
    }
}
