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
    private CatSkin selected = CatSkin.DEFAULT;

    public SkinLibrary(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        var index = directory.resolve("library.json");
        if (!Files.isRegularFile(index) || Files.size(index) > 64_000) return;
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
                    custom.add(new Entry(cleanName(item.get("name").getAsString()), new CatSkin("custom", Files.readAllBytes(image)).validated()));
                } catch (IOException | RuntimeException ignored) { /* Keep the remaining library usable. */ }
            }
            String active = json.has("selected") ? json.get("selected").getAsString() : "default";
            if (active.matches("[0-9a-f]{64}")) selected = custom.stream().filter(e -> e.skin.hash().equals(active)).map(Entry::skin).findFirst().orElse(CatSkin.DEFAULT);
            else selected = new CatSkin(active, new byte[0]);
        } catch (RuntimeException e) { selected = CatSkin.DEFAULT; }
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
    public void select(CatSkin skin) throws IOException { selected = skin; save(); }
    public void remove(CatSkin skin) throws IOException {
        custom.removeIf(entry -> entry.skin.hash().equals(skin.hash()));
        if (selected.hash().equals(skin.hash()) && selected.variant().equals("custom")) selected = CatSkin.DEFAULT;
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
}
