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
        assertTrue(raised.isRaised());
        assertFalse(lowered.isRaised());
    }
}
