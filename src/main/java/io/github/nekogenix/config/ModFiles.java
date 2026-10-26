package io.github.nekogenix.config;

import io.github.nekogenix.Nekogenix;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

public final class ModFiles {
    public static final String CLIENT_CONFIG = "nekogenix/client.toml";

    public static Path directory() { return FMLPaths.CONFIGDIR.get().resolve(Nekogenix.ID); }

    public static void prepare() {
        try { Files.createDirectories(directory()); }
        catch (IOException e) { throw new UncheckedIOException("Could not create the Nekogenix configuration directory", e); }
    }

    private ModFiles() {}
}
