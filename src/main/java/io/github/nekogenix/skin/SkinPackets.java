package io.github.nekogenix.skin;

import io.github.nekogenix.Content;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SkinPackets {
    public record Hello(UUID account) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(Content.id("skin_hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hello> CODEC = StreamCodec.of((b, p) -> b.writeUUID(p.account), b -> new Hello(b.readUUID()));
        @Override public Type<Hello> type() { return TYPE; }
    }
    public record Challenge(String nonce) implements CustomPacketPayload {
        public static final Type<Challenge> TYPE = new Type<>(Content.id("skin_challenge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Challenge> CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.nonce, 40), b -> new Challenge(b.readUtf(40)));
        @Override public Type<Challenge> type() { return TYPE; }
    }
    public record Proof() implements CustomPacketPayload {
        public static final Type<Proof> TYPE = new Type<>(Content.id("skin_proof"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Proof> CODEC = StreamCodec.unit(new Proof());
        @Override public Type<Proof> type() { return TYPE; }
    }
    public record Status(boolean allowed, String message) implements CustomPacketPayload {
        public static final Type<Status> TYPE = new Type<>(Content.id("skin_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.of((b, p) -> { b.writeBoolean(p.allowed); b.writeUtf(p.message, 64); }, b -> new Status(b.readBoolean(), b.readUtf(64)));
        @Override public Type<Status> type() { return TYPE; }
    }
    public record Apply(CatSkin skin) implements CustomPacketPayload {
        public static final Type<Apply> TYPE = new Type<>(Content.id("skin_apply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Apply> CODEC = StreamCodec.of((b, p) -> write(b, p.skin), b -> new Apply(read(b)));
        @Override public Type<Apply> type() { return TYPE; }
    }
    public record Request(UUID player, boolean texture) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Content.id("skin_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> { b.writeUUID(p.player); b.writeBoolean(p.texture); }, b -> new Request(b.readUUID(), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Offer(UUID player, String variant, String hash) implements CustomPacketPayload {
        public static final Type<Offer> TYPE = new Type<>(Content.id("skin_offer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Offer> CODEC = StreamCodec.of((b, p) -> {
            b.writeUUID(p.player); b.writeUtf(p.variant, 24); b.writeUtf(p.hash, 64);
        }, b -> new Offer(b.readUUID(), b.readUtf(24), b.readUtf(64)));
        @Override public Type<Offer> type() { return TYPE; }
    }
    public record Update(UUID player, CatSkin skin) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(Content.id("skin_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = StreamCodec.of((b, p) -> { b.writeUUID(p.player); write(b, p.skin); }, b -> new Update(b.readUUID(), read(b)));
        @Override public Type<Update> type() { return TYPE; }
    }
    private static void write(RegistryFriendlyByteBuf buffer, CatSkin skin) { buffer.writeUtf(skin.variant(), 24); buffer.writeByteArray(skin.png()); }
    private static CatSkin read(RegistryFriendlyByteBuf buffer) { return new CatSkin(buffer.readUtf(24), buffer.readByteArray(CatSkin.MAX_BYTES)); }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("cat_skins_1").optional();
        registrar.playToServer(Hello.TYPE, Hello.CODEC, (p, c) -> SkinServer.hello(c.player(), p.account));
        registrar.playToServer(Proof.TYPE, Proof.CODEC, (p, c) -> SkinServer.proof(c.player()));
        registrar.playToServer(Apply.TYPE, Apply.CODEC, (p, c) -> SkinServer.apply(c.player(), p.skin));
        registrar.playToServer(Request.TYPE, Request.CODEC, (p, c) -> SkinServer.request(c.player(), p.player, p.texture));
        registrar.playToClient(Offer.TYPE, Offer.CODEC, (p, c) -> io.github.nekogenix.client.skin.CatSkins.offer(p.player, p.variant, p.hash));
        registrar.playToClient(Challenge.TYPE, Challenge.CODEC, (p, c) -> io.github.nekogenix.client.skin.CatSkins.challenge(p.nonce));
        registrar.playToClient(Status.TYPE, Status.CODEC, (p, c) -> io.github.nekogenix.client.skin.CatSkins.status(p.allowed, p.message));
        registrar.playToClient(Update.TYPE, Update.CODEC, (p, c) -> io.github.nekogenix.client.skin.CatSkins.receive(p.player, p.skin));
    }
    private SkinPackets() {}
}
