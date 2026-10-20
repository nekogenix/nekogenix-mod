package io.github.nekogenix.network;

import io.github.nekogenix.Content;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InjectionInput(boolean held, boolean offHeld) implements CustomPacketPayload {
    public static final Type<InjectionInput> TYPE = new Type<>(Content.id("injection_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InjectionInput> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, InjectionInput::held,
                    ByteBufCodecs.BOOL, InjectionInput::offHeld, InjectionInput::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
