package io.github.nekogenix.skin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkinStore {
    public static final long RETENTION = Duration.ofDays(365).toMillis();
    public static final int MAX_PLAYERS = 4096;
    private final Path directory;
    private final Map<UUID, Entry> entries = new HashMap<>();
    private record Entry(String variant, String hash, long seen) {}

    public SkinStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        Path index = directory.resolve("players.json");
        if (Files.isRegularFile(index) && Files.size(index) < 2_000_000) {
            try (var reader = Files.newBufferedReader(index)) {
                var json = new Gson().fromJson(reader, JsonObject.class);
                for (var item : json.entrySet()) {
                    if (entries.size() >= MAX_PLAYERS) break;
                    try {
                        var value = item.getValue().getAsJsonObject();
                        String variant = value.get("variant").getAsString(), hash = value.get("hash").getAsString();
                        if (!CatSkin.VARIANTS.contains(variant) && !variant.equals("custom") && !variant.equals("default")) continue;
                        if (variant.equals("custom") && !hash.matches("[0-9a-f]{64}")) continue;
                        entries.put(UUID.fromString(item.getKey()), new Entry(variant, hash, value.get("seen").getAsLong()));
                    } catch (RuntimeException ignored) {}
                }
            } catch (RuntimeException e) { throw new IOException("Invalid cat skin index", e); }
        }
    }

    public CatSkin get(UUID owner) throws IOException {
        var entry = entries.get(owner);
        if (entry == null) return CatSkin.DEFAULT;
        Path texture = directory.resolve(entry.hash + ".png");
        if (entry.variant.equals("custom")) {
            if (!Files.isRegularFile(texture) || Files.size(texture) > CatSkin.MAX_BYTES) return CatSkin.DEFAULT;
            return new CatSkin("custom", Files.readAllBytes(texture)).validated();
        }
        return new CatSkin(entry.variant, new byte[0]);
    }

    public void put(UUID owner, CatSkin skin, long now) throws IOException {
        if (!entries.containsKey(owner) && entries.size() >= MAX_PLAYERS) throw new IOException("server_full");
        skin = skin.validated();
        if (skin.variant().equals("custom")) atomicWrite(directory.resolve(skin.hash() + ".png"), skin.png());
        var previous = entries.put(owner, new Entry(skin.variant(), skin.variant().equals("custom") ? skin.hash() : "", now));
        save();
        if (previous != null && !previous.hash.isEmpty() && entries.values().stream().noneMatch(e -> e.hash.equals(previous.hash)))
            Files.deleteIfExists(directory.resolve(previous.hash + ".png"));
    }

    public void seen(UUID owner, long now) throws IOException {
        var entry = entries.get(owner);
        if (entry != null) { entries.put(owner, new Entry(entry.variant, entry.hash, now)); save(); }
    }

    public void seen(java.util.Collection<UUID> owners, long now) throws IOException {
        for (UUID id : owners) {
            var entry = entries.get(id);
            if (entry != null) entries.put(id, new Entry(entry.variant, entry.hash, now));
        }
        save();
    }

    public void clean(long now) throws IOException {
        entries.values().removeIf(entry -> now - entry.seen > RETENTION);
        save();
        removeUnusedTextures();
    }

    private void removeUnusedTextures() throws IOException {
        var used = new java.util.HashSet<String>();
        entries.values().forEach(entry -> used.add(entry.hash + ".png"));
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.png")).toList())
                if (!used.contains(file.getFileName().toString())) Files.deleteIfExists(file);
        }
    }

    private void save() throws IOException {
        var root = new JsonObject();
        entries.forEach((id, entry) -> {
            var json = new JsonObject();
            json.addProperty("variant", entry.variant); json.addProperty("hash", entry.hash); json.addProperty("seen", entry.seen);
            root.add(id.toString(), json);
        });
        atomicWrite(directory.resolve("players.json"), new Gson().toJson(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path staging = Files.createTempFile(target.getParent(), ".skin-", ".part");
        try {
            Files.write(staging, bytes);
            try { Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(staging); }
    }
}
