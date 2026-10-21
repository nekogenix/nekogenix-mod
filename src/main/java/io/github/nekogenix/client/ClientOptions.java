package io.github.nekogenix.client;

import io.github.nekogenix.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

final class ClientOptions {
    static OptionInstance<Boolean> felineVision() {
        return OptionInstance.createBoolean("options.nekogenix.feline_vision", OptionInstance.noTooltip(),
                (caption, automatic) -> Component.translatable(
                        "options.nekogenix.feline_vision." + (automatic ? "automatic" : "manual")),
                ClientConfig.AUTOMATIC_FELINE_VISION.get(), value -> {
                    ClientConfig.AUTOMATIC_FELINE_VISION.set(value);
                    ClientConfig.SPEC.save();
                });
    }

    static OptionInstance<Boolean> doubleJump() {
        return OptionInstance.createBoolean("options.nekogenix.double_jump", ClientConfig.NEKO_DOUBLE_JUMP.get(), value -> {
            ClientConfig.NEKO_DOUBLE_JUMP.set(value);
            ClientConfig.SPEC.save();
        });
    }

    static OptionInstance<Double> voiceVolume() {
        return new OptionInstance<>("options.nekogenix.neko_voice", OptionInstance.noTooltip(),
                (caption, value) -> value == 0 ? Options.genericValueLabel(caption, CommonComponents.OPTION_OFF)
                        : Component.translatable("options.percent_value", caption, (int)(value * 100)),
                OptionInstance.UnitDouble.INSTANCE, ClientConfig.NEKO_VOICE_VOLUME.get(), value -> {
                    ClientConfig.NEKO_VOICE_VOLUME.set(value);
                    var mc = Minecraft.getInstance();
                    mc.getSoundManager().updateSourceVolume(SoundSource.PLAYERS, mc.options.getSoundSourceVolume(SoundSource.PLAYERS));
                });
    }

    static OptionInstance<Integer> cameraHeight() {
        return new OptionInstance<>("options.nekogenix.camera_height", OptionInstance.noTooltip(),
                (caption, value) -> cameraHeightLabel(value), new OptionInstance.IntRange(0, 100),
                ClientConfig.cameraHeight(), value -> {
                    ClientConfig.CAMERA_HEIGHT.set(value);
                    ClientConfig.SPEC.save();
                });
    }

    static Component cameraHeightLabel(int percent) {
        var caption = Component.translatable("options.nekogenix.camera_height");
        return percent == 0 || percent == 100
                ? Options.genericValueLabel(caption, Component.translatable("options.nekogenix.camera_height." + (percent == 0 ? "cat" : "human")))
                : Component.translatable("options.percent_value", caption, percent);
    }

    private ClientOptions() {}
}
