package com.piotrek.groundworksbulldozer.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.network.BulldozerInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Gathers operator keyboard inputs each client tick and transmits control packets to the server.
 *
 * <p>Supports:
 * <ul>
 *   <li><b>Driving (W / S / A / D)</b>: Forward/reverse throttle & differential track steering.</li>
 *   <li><b>Blade (Arrow Up / Arrow Down)</b>: Raise / lower blade height continuously.</li>
 *   <li><b>Blade Tilt (Arrow Left / Arrow Right)</b>: Fine blade tilt adjustment.</li>
 * </ul>
 */
public final class BulldozerInputHandler {

    private static float lastThrottle;
    private static float lastSteer;
    private static float lastBladeLift;
    private static float lastBladeTilt;
    private static int keepaliveTicks;

    private BulldozerInputHandler() {}

    public static void clientTick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        if (client.player.getVehicle() instanceof GroundworksBulldozerEntity dozer) {
            // Read driving inputs (WASD)
            boolean keyForward = client.options.keyUp.isDown()
                    || (client.player.input != null && client.player.input.keyPresses.forward());
            boolean keyBackward = client.options.keyDown.isDown()
                    || (client.player.input != null && client.player.input.keyPresses.backward());
            boolean keyLeft = client.options.keyLeft.isDown()
                    || (client.player.input != null && client.player.input.keyPresses.left());
            boolean keyRight = client.options.keyRight.isDown()
                    || (client.player.input != null && client.player.input.keyPresses.right());

            boolean inGame = client.mouseHandler != null && client.mouseHandler.isMouseGrabbed();

            // Read blade lift inputs (Arrow Up / Down)
            boolean bladeUp = (BulldozerKeyBindings.KEY_BLADE_UP != null && BulldozerKeyBindings.KEY_BLADE_UP.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_UP));
            boolean bladeDown = (BulldozerKeyBindings.KEY_BLADE_DOWN != null && BulldozerKeyBindings.KEY_BLADE_DOWN.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_DOWN));

            // Read blade tilt inputs (Arrow Left / Right)
            boolean tiltLeft = (BulldozerKeyBindings.KEY_BLADE_TILT_LEFT != null && BulldozerKeyBindings.KEY_BLADE_TILT_LEFT.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_LEFT));
            boolean tiltRight = (BulldozerKeyBindings.KEY_BLADE_TILT_RIGHT != null && BulldozerKeyBindings.KEY_BLADE_TILT_RIGHT.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_RIGHT));

            float throttle = 0.0F;
            float steer = 0.0F;
            float bladeLift = 0.0F;
            float bladeTilt = 0.0F;

            if (keyForward) throttle += 1.0F;
            if (keyBackward) throttle -= 1.0F;
            if (keyLeft) steer -= 1.0F;
            if (keyRight) steer += 1.0F;

            if (bladeUp) bladeLift += 1.0F;
            if (bladeDown) bladeLift -= 1.0F;

            if (tiltLeft) bladeTilt -= 1.0F;
            if (tiltRight) bladeTilt += 1.0F;

            boolean changed = throttle != lastThrottle
                    || steer != lastSteer
                    || bladeLift != lastBladeLift
                    || bladeTilt != lastBladeTilt;

            if (changed || --keepaliveTicks <= 0) {
                ClientPlayNetworking.send(new BulldozerInputPayload(
                        throttle, steer, bladeLift, bladeTilt
                ));

                lastThrottle = throttle;
                lastSteer = steer;
                lastBladeLift = bladeLift;
                lastBladeTilt = bladeTilt;
                keepaliveTicks = 5;
            }
        } else {
            lastThrottle = 0.0F;
            lastSteer = 0.0F;
            lastBladeLift = 0.0F;
            lastBladeTilt = 0.0F;
            keepaliveTicks = 0;
        }
    }
}
