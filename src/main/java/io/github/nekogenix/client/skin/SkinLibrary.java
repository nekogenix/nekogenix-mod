package io.github.nekogenix.client.skin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.nekogenix.skin.CatSkin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkinLibrary {
    public record Entry(String name, CatSkin skin) {}
    private final Path directory;
    private final List<Entry> custom = new ArrayList<>();
    private CatSkin selected = randomSkin();

    public SkinLibrary(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        var index = directory.resolve("library.json");
        if (!Files.isRegularFile(index)) { save(); return; }
        if (Files.size(index) > 64_000) return;
        boolean persistSelection = false;
        var loaded = new java.util.HashMap<String, CatSkin>();
        try (var reader = Files.newBufferedReader(index)) {
            var json = new Gson().fromJson(reader, JsonObject.class);
            for (var value : json.getAsJsonArray("skins")) {
                if (custom.size() >= 32) break;
                try {
                    var item = value.getAsJsonObject();
                    String hash = item.get("hash").getAsString();
                    if (!hash.matches("[0-9a-f]{64}")) continue;
                    Path image = directory.resolve(hash + ".png");
                    if (!Files.isRegularFile(image) || Files.size(image) > CatSkin.MAX_BYTES) continue;
                    var skin = new CatSkin("custom", Files.readAllBytes(image)).validated();
                    if (!skin.hash().equals(hash)) {
                        Files.write(directory.resolve(skin.hash() + ".png"), skin.png());
                        persistSelection = true;
                    }
                    custom.add(new Entry(cleanName(item.get("name").getAsString()), skin));
                    loaded.put(hash, skin);
                } catch (IOException | RuntimeException ignored) {}
            }
            String active = json.has("selected") ? json.get("selected").getAsString() : "default";
            if (active.matches("[0-9a-f]{64}")) {
                var saved = loaded.get(active);
                if (saved != null) selected = saved; else persistSelection = true;
            } else if (CatSkin.VARIANTS.contains(active)) selected = new CatSkin(active, new byte[0]);
            else persistSelection = true;
        } catch (RuntimeException e) { return; }
        if (persistSelection) save();
    }

    public List<Entry> entries() { return List.copyOf(custom); }
    public CatSkin selected() { return selected; }
    public Entry add(String name, byte[] bytes) throws IOException {
        var skin = new CatSkin("custom", CatSkin.normalize(bytes));
        for (int i = 0; i < custom.size(); i++) if (custom.get(i).skin.hash().equals(skin.hash())) {
            var entry = new Entry(cleanName(name), skin);
            custom.set(i, entry); save(); return entry;
        }
        if (custom.size() >= 32) throw new IOException("library_full");
        var entry = new Entry(cleanName(name), skin);
        Files.write(directory.resolve(skin.hash() + ".png"), skin.png());
        custom.add(entry); save(); return entry;
    }
    public void select(CatSkin skin) throws IOException { selected = skin.variant().equals("default") ? randomSkin() : skin; save(); }
    public void rename(CatSkin skin, String name) throws IOException {
        if (!skin.variant().equals("custom")) throw new IOException("not_custom");
        for (int i = 0; i < custom.size(); i++) if (custom.get(i).skin.hash().equals(skin.hash())) {
            var previous = custom.set(i, new Entry(cleanName(name), custom.get(i).skin));
            try { save(); } catch (IOException e) { custom.set(i, previous); throw e; }
            return;
        }
        throw new IOException("skin_not_found");
    }
    public void remove(CatSkin skin) throws IOException {
        custom.removeIf(entry -> entry.skin.hash().equals(skin.hash()));
        if (selected.hash().equals(skin.hash()) && selected.variant().equals("custom")) selected = randomSkin();
        save(); Files.deleteIfExists(directory.resolve(skin.hash() + ".png"));
    }
    private void save() throws IOException {
        var json = new JsonObject(); var list = new com.google.gson.JsonArray();
        for (var entry : custom) {
            var item = new JsonObject(); item.addProperty("name", entry.name); item.addProperty("hash", entry.skin.hash()); list.add(item);
        }
        json.add("skins", list); json.addProperty("selected", selected.variant().equals("custom") ? selected.hash() : selected.variant());
        Path staging = directory.resolve("library.json.part");
        try {
            Files.writeString(staging, new Gson().toJson(json));
            try { Files.move(staging, directory.resolve("library.json"), java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(staging, directory.resolve("library.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(staging); }
    }
    private static String cleanName(String name) {
        String clean = name.replaceAll("[\\p{Cntrl}§]", "").strip();
        return clean.isEmpty() ? "Custom Skin" : clean.substring(0, Math.min(32, clean.length()));
    }
    private static CatSkin randomSkin() {
        return new CatSkin(CatSkin.VARIANTS.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(CatSkin.VARIANTS.size())), new byte[0]);
    }
}
