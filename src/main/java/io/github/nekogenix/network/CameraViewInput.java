package io.github.nekogenix.network;

import io.github.nekogenix.Content;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CameraViewInput(float height, int preference) implements CustomPacketPayload {
    public static final Type<CameraViewInput> TYPE = new Type<>(Content.id("camera_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraViewInput> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, CameraViewInput::height, ByteBufCodecs.VAR_INT, CameraViewInput::preference, CameraViewInput::new);

    @Override public Type<CameraViewInput> type() { return TYPE; }
}
