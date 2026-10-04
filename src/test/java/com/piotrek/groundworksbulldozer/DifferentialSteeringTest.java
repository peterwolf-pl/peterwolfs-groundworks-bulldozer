package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController;
import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController.TrackState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferentialSteeringTest {

    @Test
    @DisplayName("In-place pivot turn: throttle neutral and steer active drives tracks in opposite directions")
    void testInPlacePivotTurn() {
        BulldozerTrackController controller = new BulldozerTrackController();

        // Step several ticks to allow tracks to accelerate
        TrackState state = null;
        for (int i = 0; i < 10; i++) {
            state = controller.tick(
                    null,
                    new Vec3(0, 0, 0),
                    0.0F,
                    0.0F,  // Neutral throttle (no W or S)
                    1.0F,  // Steer right (Key D)
                    true,
                    false
            );
        }

        assert state != null;
        // Right turn: left track must drive forward, right track must reverse!
        assertTrue(state.leftSpeed() > 0.05F, "Left track must drive forward during right pivot");
        assertTrue(state.rightSpeed() < -0.05F, "Right track must reverse during right pivot");

        // Forward translation should be approximately 0
        assertEquals(0.0D, state.forwardDelta().length(), 0.015D, "In-place pivot should not translate forward");

        // Yaw delta must turn clockwise (positive angle delta)
        assertTrue(state.yawDeltaDegrees() > 0.5F, "Steering right should produce positive turning yaw delta");
    }

    @Test
    @DisplayName("Straight driving: both tracks accelerate equally with zero yaw rotation")
    void testStraightDriving() {
        BulldozerTrackController controller = new BulldozerTrackController();

        TrackState state = null;
        for (int i = 0; i < 10; i++) {
            state = controller.tick(
                    null,
                    new Vec3(0, 0, 0),
                    0.0F,
                    1.0F,  // Forward throttle (Key W)
                    0.0F,  // Neutral steer
                    true,
                    false
            );
        }

        assert state != null;
        assertTrue(state.leftSpeed() > 0.08F);
        assertEquals(state.leftSpeed(), state.rightSpeed(), 1e-4F, "Left and right tracks must match in straight driving");
        assertEquals(0.0F, state.yawDeltaDegrees(), 1e-4F, "No turning during straight driving");
        assertTrue(state.forwardDelta().z > 0.08D, "Forward delta must move along heading");
    }
}
