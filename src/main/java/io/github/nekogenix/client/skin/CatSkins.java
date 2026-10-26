package io.github.nekogenix.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.config.ModFiles;
import io.github.nekogenix.skin.CatSkin;
import io.github.nekogenix.skin.SkinPackets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CatSkins {
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
        var thread = new Thread(runnable, "Cat skin client"); thread.setDaemon(true); return thread;
    });
    private static final Map<UUID, CatSkin> PLAYERS = new HashMap<>();
    private static final LinkedHashMap<String, ResourceLocation> TEXTURES = new LinkedHashMap<>(16, .75F, true);
    private static final java.util.Set<UUID> REQUESTED = new HashSet<>();
    private static final Map<UUID, String> OFFERS = new HashMap<>();
    private static final java.util.ArrayDeque<UUID> DOWNLOADS = new java.util.ArrayDeque<>();
    private static SkinLibrary library;
    private static List<SkinLibrary.Entry> entries = List.of();
    private static CatSkin selected = new CatSkin("tabby", new byte[0]);
    private static boolean loaded, loading, licensed, checking, serverAllowed, helloSent, pendingSend;
    private static Object connection;
    private static int ticks;
    private static long checkingUntil, nextSend;
    private static String message = "license_required";

    public static List<SkinLibrary.Entry> entries() { return entries; }
    public static CatSkin selected() { return selected; }
    public static boolean loaded() { return loaded; }
    public static boolean licensed() { return licensed; }
    public static boolean checking() { return checking; }
    public static Component message() { return message.isEmpty() ? Component.empty() : Component.translatable("skins.nekogenix." + message); }

    private static Path directory() { return ModFiles.directory().resolve("cat_skins"); }
    public static void load() {
        if (loaded || loading) return;
        loading = true;
        if (!work(() -> {
            try { prune(directory().resolve("cache")); }
            catch (java.io.IOException e) { Nekogenix.LOGGER.warn("Could not clean the cat skin cache", e); }
            try {
                library = new SkinLibrary(directory().resolve("library"));
                var saved = library.selected(); var items = library.entries();
                Minecraft.getInstance().execute(() -> { selected = saved; entries = items; loaded = true; loading = false; if (serverAllowed) send(); });
            } catch (Exception e) { Minecraft.getInstance().execute(() -> { message = "storage_failed"; loading = false; }); }
        })) loading = false;
    }

    public static void verify() {
        if (checking) return;
        var connected = Minecraft.getInstance().getConnection();
        if (connected != null && connected.hasChannel(SkinPackets.Hello.TYPE) && !serverAllowed) {
            PacketDistributor.sendToServer(new SkinPackets.Hello(Minecraft.getInstance().getUser().getProfileId()));
            message = "checking";
            checking = true; checkingUntil = System.currentTimeMillis() + 45_000;
            return;
        }
        if (licensed) return;
        checking = true; checkingUntil = System.currentTimeMillis() + 45_000; message = "checking";
        var mc = Minecraft.getInstance();
        if (!work(() -> {
            try {
                byte[] random = new byte[20]; new SecureRandom().nextBytes(random);
                String nonce = HexFormat.of().formatHex(random);
                mc.getMinecraftSessionService().joinServer(mc.getUser().getProfileId(), mc.getUser().getAccessToken(), nonce);
                var result = mc.getMinecraftSessionService().hasJoinedServer(mc.getUser().getName(), nonce, null);
                boolean valid = result != null && result.profile().getId().equals(mc.getUser().getProfileId());
                mc.execute(() -> { licensed = valid; checking = false; message = valid ? "ready" : "license_required"; });
            } catch (Exception e) { mc.execute(() -> { checking = false; message = "license_required"; }); }
        })) checking = false;
    }

    public static void challenge(String nonce) {
        if (!nonce.matches("[0-9a-f]{40}")) return;
        var mc = Minecraft.getInstance(); var expected = mc.getConnection();
        checking = true; checkingUntil = System.currentTimeMillis() + 45_000; message = "checking";
        if (!work(() -> {
            try {
                mc.getMinecraftSessionService().joinServer(mc.getUser().getProfileId(), mc.getUser().getAccessToken(), nonce);
                mc.execute(() -> { if (mc.getConnection() == expected) PacketDistributor.sendToServer(new SkinPackets.Proof()); });
            } catch (Exception e) { mc.execute(() -> { checking = false; message = "license_required"; }); }
        })) checking = false;
    }

    public static void status(boolean allowed, String reason) {
        serverAllowed = allowed; checking = false;
        if (allowed && reason.equals("wait")) { pendingSend = true; nextSend = System.currentTimeMillis() + 2100; return; }
        message = reason.equals("applied") ? "" : List.of("ready", "license_required", "upload_failed").contains(reason) ? reason : "upload_failed";
        if (allowed) licensed = true;
        if (allowed && reason.equals("ready")) { load(); if (loaded) send(); }
    }

    public static void apply(CatSkin skin) {
        if (!licensed || !loaded) return;
        work(() -> {
            try {
                library.select(skin);
                Minecraft.getInstance().execute(() -> { selected = skin; message = "saved"; if (serverAllowed) send(); });
            } catch (Exception e) { fail("storage_failed"); }
        });
    }

    public static void previewFile(Path path, java.util.function.Consumer<CatSkin> done, java.util.function.Consumer<Component> failed) {
        if (!work(() -> {
            try {
                byte[] bytes;
                try (var input = Files.newInputStream(path)) { bytes = input.readNBytes(CatSkin.MAX_BYTES + 1); }
                CatSkin skin = new CatSkin("custom", CatSkin.normalize(bytes));
                Minecraft.getInstance().execute(() -> done.accept(skin));
            } catch (Exception e) {
                Component error = imageError(e);
                Minecraft.getInstance().execute(() -> failed.accept(error));
            }
        })) failed.accept(Component.translatable("skins.nekogenix.busy"));
    }

    public static void saveDraft(String name, CatSkin skin, java.util.function.Consumer<CatSkin> done, java.util.function.Consumer<Component> failed) {
        load();
        if (!work(() -> {
            try {
                if (library == null) throw new java.io.IOException("storage_unavailable");
                var saved = library.add(name, skin.png()); var items = library.entries();
                Minecraft.getInstance().execute(() -> { entries = items; message = "imported"; done.accept(saved.skin()); });
            } catch (Exception e) {
                Component error = Component.translatable("skins.nekogenix." + ("library_full".equals(e.getMessage()) ? "library_full" : "storage_failed"));
                Minecraft.getInstance().execute(() -> failed.accept(error));
            }
        })) failed.accept(Component.translatable("skins.nekogenix.busy"));
    }

    private static Component imageError(Exception error) {
        if (error instanceof CatSkin.InvalidImageException invalid)
            return invalid.width >= 0 ? Component.translatable("skins.nekogenix.wrong_dimensions", invalid.width, invalid.height)
                    : Component.translatable("skins.nekogenix.invalid_png");
        return Component.translatable("skins.nekogenix.file_read_failed");
    }

    public static void remove(CatSkin skin) {
        if (!loaded || !skin.variant().equals("custom")) return;
        work(() -> {
            try {
                library.remove(skin); var items = library.entries(); var active = library.selected();
                Minecraft.getInstance().execute(() -> { entries = items; selected = active; message = "deleted"; if (serverAllowed) send(); });
            } catch (Exception e) { fail("storage_failed"); }
        });
    }

    public static void rename(CatSkin skin, String name) {
        if (!loaded || !skin.variant().equals("custom")) return;
        work(() -> {
            try {
                library.rename(skin, name); var items = library.entries();
                Minecraft.getInstance().execute(() -> { entries = items; message = "renamed"; });
            } catch (java.io.IOException e) { fail("storage_failed"); }
        });
    }

    private static void send() {
        pendingSend = true;
    }

    public static void receive(UUID player, CatSkin skin) {
        if (!serverAllowed || PLAYERS.size() >= 512 && !PLAYERS.containsKey(player)) return;
        if (skin.variant().equals("custom") && !skin.hash().equals(OFFERS.get(player))) return;
        if (skin.variant().equals("custom")) prepare(skin);
        PLAYERS.put(player, skin);
    }

    public static void offer(UUID player, String variant, String hash) {
        if (!serverAllowed || OFFERS.size() >= 512 && !OFFERS.containsKey(player)
                || PLAYERS.size() >= 512 && !PLAYERS.containsKey(player)) return;
        if (!variant.equals("custom")) {
            if (variant.equals("default") || CatSkin.VARIANTS.contains(variant)) {
                OFFERS.remove(player); PLAYERS.put(player, new CatSkin(variant, new byte[0]));
            }
            return;
        }
        if (!hash.matches("[0-9a-f]{64}")) return;
        if (hash.equals(OFFERS.put(player, hash))) return;
        var expected = Minecraft.getInstance().getConnection();
        if (!work(() -> {
            CatSkin skin = null;
            try {
                Path file = directory().resolve("cache").resolve(hash + ".png");
                if (Files.isRegularFile(file) && Files.size(file) <= CatSkin.MAX_BYTES) {
                    var cached = new CatSkin("custom", Files.readAllBytes(file)).validated();
                    if (cached.hash().equals(hash)) { skin = cached; Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis())); }
                }
            } catch (Exception ignored) {}
            var cached = skin;
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().getConnection() != expected || !hash.equals(OFFERS.get(player))) return;
                if (cached != null) receive(player, cached);
                else if (!DOWNLOADS.contains(player)) DOWNLOADS.add(player);
            });
        })) { OFFERS.remove(player); REQUESTED.remove(player); }
    }

    public static ResourceLocation texture(UUID player, ResourceLocation fallback) {
        if (!licensed) return fallback;
        var mc = Minecraft.getInstance();
        CatSkin skin = PLAYERS.get(player);
        if (skin == null && (player.equals(mc.getUser().getProfileId()) || mc.player != null && player.equals(mc.player.getUUID()))) skin = selected;
        return skin == null ? fallback : texture(skin, fallback);
    }

    public static ResourceLocation texture(CatSkin skin, ResourceLocation fallback) {
        if (skin.variant().equals("default")) return fallback;
        if (!skin.variant().equals("custom")) return ResourceLocation.withDefaultNamespace("textures/entity/cat/" + skin.variant() + ".png");
        String hash = skin.hash();
        var texture = TEXTURES.get(hash);
        if (texture == null) prepare(skin);
        return texture == null ? fallback : texture;
    }

    private static final java.util.Set<String> PENDING = new HashSet<>();
    private static void prepare(CatSkin skin) {
        String hash = skin.hash();
        if (TEXTURES.containsKey(hash) || !PENDING.add(hash)) return;
        if (!work(() -> {
            try {
                byte[] png = skin.validated().png();
                Path cache = directory().resolve("cache"); Files.createDirectories(cache);
                Path file = cache.resolve(hash + ".png");
                if (!Files.isRegularFile(file)) Files.write(file, png);
                var image = NativeImage.read(png);
                Minecraft.getInstance().execute(() -> {
                    PENDING.remove(hash);
                    var id = Content.id("cat_skin/" + hash);
                    Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image)); TEXTURES.put(hash, id);
                    if (TEXTURES.size() > 256) {
                        var first = TEXTURES.firstEntry(); TEXTURES.remove(first.getKey()); Minecraft.getInstance().getTextureManager().release(first.getValue());
                    }
                });
                prune(cache);
            } catch (Exception e) { Minecraft.getInstance().execute(() -> PENDING.remove(hash)); }
        })) PENDING.remove(hash);
    }

    private static void prune(Path cache) throws java.io.IOException {
        if (!Files.isDirectory(cache)) return;
        try (var stream = Files.list(cache)) {
            var files = new ArrayList<>(stream.filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.png")).toList());
            files.sort(java.util.Comparator.comparingLong(p -> p.toFile().lastModified()));
            long total = 0; for (var file : files) total += Files.size(file);
            for (var file : files) if (total > 32 * 1024 * 1024 || System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() > TimeUnit.DAYS.toMillis(90)) {
                total -= Files.size(file); Files.deleteIfExists(file);
            }
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (checking && System.currentTimeMillis() > checkingUntil) { checking = false; message = "license_required"; }
        if (mc.getConnection() == null || mc.player == null) return;
        if (connection != mc.getConnection()) { reset(); connection = mc.getConnection(); load(); }
        if (!helloSent && mc.getConnection().hasChannel(SkinPackets.Hello.TYPE)) {
            helloSent = true; PacketDistributor.sendToServer(new SkinPackets.Hello(mc.getUser().getProfileId()));
        }
        if (serverAllowed && loaded && pendingSend && System.currentTimeMillis() >= nextSend) {
            pendingSend = false; nextSend = System.currentTimeMillis() + 2100;
            PacketDistributor.sendToServer(new SkinPackets.Apply(selected));
        }
        if (!serverAllowed || ++ticks % 20 != 0 || mc.level == null) return;
        int sent = 0;
        while (!DOWNLOADS.isEmpty() && sent < 4) { sent++; PacketDistributor.sendToServer(new SkinPackets.Request(DOWNLOADS.removeFirst(), true)); }
        for (var player : mc.level.players()) if (!REQUESTED.contains(player.getUUID()) && sent < 8) {
            REQUESTED.add(player.getUUID()); sent++; PacketDistributor.sendToServer(new SkinPackets.Request(player.getUUID(), false));
        }
        REQUESTED.removeIf(id -> mc.level.getPlayerByUUID(id) == null);
        PLAYERS.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);
        OFFERS.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    private static void reset() { PLAYERS.clear(); REQUESTED.clear(); OFFERS.clear(); DOWNLOADS.clear(); serverAllowed = helloSent = pendingSend = false; connection = null; ticks = 0; nextSend = 0; }
    private static void fail(String reason) { Minecraft.getInstance().execute(() -> message = reason); }
    private static boolean work(Runnable task) {
        try { WORKER.execute(task); return true; }
        catch (java.util.concurrent.RejectedExecutionException e) { message = "busy"; return false; }
    }
    private CatSkins() {}
}
