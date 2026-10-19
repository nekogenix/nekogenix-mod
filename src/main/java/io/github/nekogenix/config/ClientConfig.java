package io.github.nekogenix.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue NEKO_VOICE_VOLUME;
    public static final ModConfigSpec.IntValue CAMERA_HEIGHT;
    public static final ModConfigSpec.BooleanValue CAMERA_CAT_FORM;

    static {
        var builder = new ModConfigSpec.Builder() {
            @Override public ModConfigSpec.Builder comment(String comment) { return this; }
            @Override public ModConfigSpec.Builder comment(String... comment) { return this; }
        };
        NEKO_VOICE_VOLUME = builder.defineInRange("nekoVoiceVolume", 1.0, 0.0, 1.0);
        CAMERA_HEIGHT = builder.defineInRange("cameraHeight", 100, 0, 100);
        CAMERA_CAT_FORM = builder.define("cameraCatForm", false);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
