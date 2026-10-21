package io.github.nekogenix.network;

import io.github.nekogenix.Content;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PassageInput(float x, float z, boolean entering) implements CustomPacketPayload {
    public static final Type<PassageInput> TYPE = new Type<>(Content.id("passage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PassageInput> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PassageInput::x, ByteBufCodecs.FLOAT, PassageInput::z,
            ByteBufCodecs.BOOL, PassageInput::entering, PassageInput::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
