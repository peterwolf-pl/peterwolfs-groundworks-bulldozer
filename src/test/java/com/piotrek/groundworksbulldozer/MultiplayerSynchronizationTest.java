package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.network.BulldozerInputPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiplayerSynchronizationTest {

    @Test
    @DisplayName("BulldozerInputPayload encodes and decodes client controls with exact float fidelity")
    void testNetworkPayloadRoundtrip() {
        BulldozerInputPayload clientIntent = new BulldozerInputPayload(
                1.0F,   // Full forward throttle (W)
                -0.75F, // Differential left steer (A)
                -0.50F, // Blade down (Arrow Down)
                0.25F   // Blade tilt
        );

        net.minecraft.core.RegistryAccess registryAccess = net.minecraft.core.RegistryAccess.EMPTY;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);

        // Encode to byte stream (as client would send)
        BulldozerInputPayload.CODEC.encode(buf, clientIntent);

        // Decode from byte stream (as server would receive)
        BulldozerInputPayload receivedIntent = BulldozerInputPayload.CODEC.decode(buf);

        assertEquals(clientIntent.throttle(), receivedIntent.throttle(), 1e-6F, "Throttle must match");
        assertEquals(clientIntent.steer(), receivedIntent.steer(), 1e-6F, "Steer must match");
        assertEquals(clientIntent.bladeLift(), receivedIntent.bladeLift(), 1e-6F, "Blade lift must match");
        assertEquals(clientIntent.bladeTilt(), receivedIntent.bladeTilt(), 1e-6F, "Blade tilt must match");
    }
}
