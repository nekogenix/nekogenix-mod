package io.github.nekogenix.network;

import io.github.nekogenix.Content;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FelineVisionInput(boolean automatic, boolean enabled) implements CustomPacketPayload {
    public static final Type<FelineVisionInput> TYPE = new Type<>(Content.id("feline_vision_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FelineVisionInput> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FelineVisionInput::automatic, ByteBufCodecs.BOOL, FelineVisionInput::enabled, FelineVisionInput::new);

    @Override public Type<FelineVisionInput> type() { return TYPE; }
}
