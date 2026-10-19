package io.github.nekogenix.skin;

import io.github.nekogenix.Nekogenix;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class SkinServer {
    private static State state;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final class Session {
        UUID account;
        String nonce;
        long challengeExpires, retryAfter, nextApply;
        boolean verified, verifying;
        int requests;
        long requestWindow;
        CatSkin skin = CatSkin.DEFAULT;
    }
    private static final class State {
        final MinecraftServer server;
        final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
            var thread = new Thread(runnable, "Cat skin storage"); thread.setDaemon(true); return thread;
        });
        final Map<UUID, Session> sessions = new HashMap<>();
        SkinStore store;
        long nextClean;
        State(MinecraftServer server) { this.server = server; }
        boolean work(Runnable task) {
            try { worker.execute(task); return true; }
            catch (java.util.concurrent.RejectedExecutionException ignored) { return false; }
        }
    }

    @SubscribeEvent public static void start(ServerStartedEvent event) {
        var current = new State(event.getServer()); state = current;
        current.work(() -> {
            try {
                current.store = new SkinStore(current.server.getWorldPath(LevelResource.ROOT).resolve("data/nekogenix/cat_skins"));
                current.store.clean(System.currentTimeMillis());
            } catch (java.io.IOException e) { Nekogenix.LOGGER.warn("Cat skin storage unavailable: {}", e.getClass().getSimpleName()); }
        });
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        var current = state;
        if (current == null || current.server != event.getServer()) return;
        state = null; current.worker.shutdown();
        try { if (!current.worker.awaitTermination(3, TimeUnit.SECONDS)) current.worker.shutdownNow(); }
        catch (InterruptedException e) { current.worker.shutdownNow(); Thread.currentThread().interrupt(); }
        current.sessions.clear();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (state != null) state.sessions.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void clean(ServerTickEvent.Post event) {
        var current = state;
        if (current == null || System.currentTimeMillis() < current.nextClean) return;
        current.nextClean = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24);
        var active = current.sessions.values().stream().filter(s -> s.verified).map(s -> s.account).toList();
        current.work(() -> {
            if (current.store == null) return;
            try {
                long now = System.currentTimeMillis();
                current.store.seen(active, now);
                current.store.clean(now);
            } catch (java.io.IOException e) { Nekogenix.LOGGER.warn("Cat skin cleanup could not complete"); }
        });
    }

    public static void hello(Player entity, UUID account) {
        if (!(entity instanceof ServerPlayer player) || state == null) return;
        var session = state.sessions.computeIfAbsent(player.getUUID(), id -> new Session());
        long now = System.currentTimeMillis();
        if (session.verified || session.verifying || now < session.retryAfter) return;
        session.retryAfter = now + TimeUnit.SECONDS.toMillis(30);
        session.account = account;
        byte[] random = new byte[20]; RANDOM.nextBytes(random);
        session.nonce = HexFormat.of().formatHex(random);
        session.challengeExpires = now + TimeUnit.SECONDS.toMillis(90);
        PacketDistributor.sendToPlayer(player, new SkinPackets.Challenge(session.nonce));
    }

    public static void proof(Player entity) {
        if (!(entity instanceof ServerPlayer player) || state == null) return;
        var current = state;
        var session = current.sessions.get(player.getUUID());
        if (session == null || session.nonce == null || session.verifying || session.verified
                || System.currentTimeMillis() > session.challengeExpires) return;
        String nonce = session.nonce, name = player.getGameProfile().getName();
        session.nonce = null; session.verifying = true;
        if (!current.work(() -> {
            boolean verified = false;
            CatSkin saved = CatSkin.DEFAULT;
            try {
                var result = current.server.getSessionService().hasJoinedServer(name, nonce, null);
                verified = result != null && result.profile().getId().equals(session.account)
                        && result.profile().getName().equalsIgnoreCase(name);
                if (verified && current.store != null) {
                    current.store.seen(session.account, System.currentTimeMillis());
                    saved = current.store.get(session.account);
                }
            } catch (Exception ignored) {}
            boolean allowed = verified; CatSkin skin = saved;
            current.server.execute(() -> {
                if (state != current || current.sessions.get(player.getUUID()) != session || player.hasDisconnected()) return;
                session.verifying = false; session.verified = allowed; session.skin = skin;
                PacketDistributor.sendToPlayer(player, new SkinPackets.Status(allowed, allowed ? "ready" : "license_required"));
                if (allowed) broadcast(current, player, session);
            });
        })) {
            session.verifying = false;
            PacketDistributor.sendToPlayer(player, new SkinPackets.Status(false, "license_required"));
        }
    }

    public static void apply(Player entity, CatSkin skin) {
        if (!(entity instanceof ServerPlayer player) || state == null) return;
        var current = state; var session = current.sessions.get(player.getUUID());
        if (session == null || !session.verified) return;
        if (System.currentTimeMillis() < session.nextApply) {
            PacketDistributor.sendToPlayer(player, new SkinPackets.Status(true, "wait"));
            return;
        }
        session.nextApply = System.currentTimeMillis() + 2000;
        if (!current.work(() -> {
            try {
                if (current.store == null) throw new java.io.IOException("storage_unavailable");
                CatSkin valid = skin.validated();
                current.store.put(session.account, valid, System.currentTimeMillis());
                current.server.execute(() -> {
                    if (state != current || current.sessions.get(player.getUUID()) != session) return;
                    session.skin = valid; broadcast(current, player, session);
                    PacketDistributor.sendToPlayer(player, new SkinPackets.Status(true, "applied"));
                });
            } catch (java.io.IOException e) {
                current.server.execute(() -> {
                    if (state == current && current.sessions.get(player.getUUID()) == session)
                        PacketDistributor.sendToPlayer(player, new SkinPackets.Status(true, "upload_failed"));
                });
            }
        })) PacketDistributor.sendToPlayer(player, new SkinPackets.Status(true, "upload_failed"));
    }

    public static void request(Player entity, UUID target, boolean texture) {
        if (!(entity instanceof ServerPlayer player) || state == null) return;
        var session = state.sessions.get(player.getUUID());
        if (session == null || !session.verified) return;
        long now = System.currentTimeMillis();
        if (now - session.requestWindow >= 1000) { session.requests = 0; session.requestWindow = now; }
        if (++session.requests > 16) return;
        var owner = state.server.getPlayerList().getPlayer(target);
        var skin = state.sessions.get(target);
        if (owner != null && skin != null && skin.verified && owner.serverLevel() == player.serverLevel()
                && owner.distanceToSqr(player) < 192 * 192) {
            if (texture) PacketDistributor.sendToPlayer(player, new SkinPackets.Update(target, skin.skin));
            else PacketDistributor.sendToPlayer(player, offer(target, skin.skin));
        }
    }

    private static void broadcast(State current, ServerPlayer owner, Session skin) {
        for (var player : current.server.getPlayerList().getPlayers()) {
            var viewer = current.sessions.get(player.getUUID());
            if (viewer != null && viewer.verified && player.serverLevel() == owner.serverLevel() && player.distanceToSqr(owner) < 192 * 192)
                PacketDistributor.sendToPlayer(player, offer(owner.getUUID(), skin.skin));
        }
    }
    private static SkinPackets.Offer offer(UUID player, CatSkin skin) { return new SkinPackets.Offer(player, skin.variant(), skin.hash()); }
    private SkinServer() {}
}
