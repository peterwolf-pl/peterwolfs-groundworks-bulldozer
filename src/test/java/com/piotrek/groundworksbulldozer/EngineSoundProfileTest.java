package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.vehicle.EngineSoundProfile;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineSoundProfileTest {

    @Test
    void trackLoadRaisesPitchAndVolume() {
        EngineSoundProfile.Mix idle = EngineSoundProfile.forTrackSpeeds(0.0F, 0.0F);
        EngineSoundProfile.Mix loaded = EngineSoundProfile.forTrackSpeeds(0.13F, -0.13F);

        assertTrue(loaded.pitch() > idle.pitch());
        assertTrue(loaded.volume() > idle.volume());
    }

    @Test
    void originalVorbisLoopIsPackaged() throws Exception {
        String path = "/assets/pw_groundworks_bulldozer/sounds/engine_loop.ogg";
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input);
            assertArrayEquals(new byte[] {'O', 'g', 'g', 'S'}, input.readNBytes(4));
        }
    }
}
