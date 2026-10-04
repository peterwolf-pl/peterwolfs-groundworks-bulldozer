package com.piotrek.groundworksbulldozer.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Keybindings for bulldozer driving and blade height operation.
 */
public final class BulldozerKeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(GroundworksBulldozerMod.id("controls"));

    public static KeyMapping KEY_BLADE_UP;
    public static KeyMapping KEY_BLADE_DOWN;
    public static KeyMapping KEY_BLADE_TILT_LEFT;
    public static KeyMapping KEY_BLADE_TILT_RIGHT;

    private BulldozerKeyBindings() {}

    public static void register() {
        KEY_BLADE_UP = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_bulldozer.blade_up",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_UP,
                CATEGORY
        ));

        KEY_BLADE_DOWN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_bulldozer.blade_down",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_DOWN,
                CATEGORY
        ));

        KEY_BLADE_TILT_LEFT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_bulldozer.blade_tilt_left",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LEFT,
                CATEGORY
        ));

        KEY_BLADE_TILT_RIGHT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_bulldozer.blade_tilt_right",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RIGHT,
                CATEGORY
        ));
    }
}
