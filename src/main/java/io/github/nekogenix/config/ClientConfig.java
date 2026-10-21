package io.github.nekogenix.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue NEKO_VOICE_VOLUME;
    public static final ModConfigSpec.IntValue CAMERA_HEIGHT;
    public static final ModConfigSpec.BooleanValue NEKO_DOUBLE_JUMP;
    public static final ModConfigSpec.BooleanValue AUTOMATIC_FELINE_VISION;
    private static final ModConfigSpec.BooleanValue LEGACY_CAT_CAMERA;

    static {
        var builder = new ModConfigSpec.Builder() {
            @Override public ModConfigSpec.Builder comment(String comment) { return this; }
            @Override public ModConfigSpec.Builder comment(String... comment) { return this; }
        };
        NEKO_VOICE_VOLUME = builder.defineInRange("nekoVoiceVolume", 1.0, 0.0, 1.0);
        CAMERA_HEIGHT = builder.defineInRange("cameraHeight", 100, 0, 100);
        NEKO_DOUBLE_JUMP = builder.define("nekoDoubleJump", true);
        AUTOMATIC_FELINE_VISION = builder.define("automaticFelineVision", true);
        LEGACY_CAT_CAMERA = builder.define("cameraCatForm", false);
        SPEC = builder.build();
    }

    public static int cameraHeight() {
        if (LEGACY_CAT_CAMERA.get()) {
            LEGACY_CAT_CAMERA.set(false);
            CAMERA_HEIGHT.set(100);
            SPEC.save();
        }
        return CAMERA_HEIGHT.get();
    }

    private ClientConfig() {}
}
