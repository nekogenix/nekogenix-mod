package io.github.nekogenix.network;

import io.github.nekogenix.Content;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AirJumpInput() implements CustomPacketPayload {
    public static final Type<AirJumpInput> TYPE = new Type<>(Content.id("air_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirJumpInput> STREAM_CODEC = StreamCodec.unit(new AirJumpInput());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
