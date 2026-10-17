package io.github.nekogenix.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue NEKO_VOICE_VOLUME;

    static {
        var builder = new ModConfigSpec.Builder();
        NEKO_VOICE_VOLUME = builder.comment("Volume of ambient neko player voices. Action sounds use their normal volume settings.")
                .defineInRange("nekoVoiceVolume", 1.0, 0.0, 1.0);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
