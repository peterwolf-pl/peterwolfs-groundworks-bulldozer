package com.piotrek.groundworksbulldozer.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Compact client operator intent packet.
 *
 * <p>Transmits:
 * <ul>
 *   <li>Normalized drive throttle (-1.0 .. +1.0 for W / S)</li>
 *   <li>Normalized steer input (-1.0 .. +1.0 for A / D)</li>
 *   <li>Normalized blade lift input (-1.0 = lower, +1.0 = raise, 0.0 = hold)</li>
 *   <li>Normalized blade tilt input (-1.0 .. +1.0)</li>
 * </ul>
 */
public record BulldozerInputPayload(
        float throttle,
        float steer,
        float bladeLift,
        float bladeTilt
) implements CustomPacketPayload {

    public static final Type<BulldozerInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("pw_groundworks_bulldozer", "bulldozer_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulldozerInputPayload> CODEC = new StreamCodec<>() {
        @Override
        public BulldozerInputPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BulldozerInputPayload(
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BulldozerInputPayload payload) {
            buffer.writeFloat(payload.throttle);
            buffer.writeFloat(payload.steer);
            buffer.writeFloat(payload.bladeLift);
            buffer.writeFloat(payload.bladeTilt);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
