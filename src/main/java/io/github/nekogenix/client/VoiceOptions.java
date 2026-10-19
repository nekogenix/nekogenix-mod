package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.VoiceSounds;
import io.github.nekogenix.config.ClientConfig;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class VoiceOptions {
    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound != null && !(sound instanceof VoiceSound) && VoiceSounds.isVoice(sound.getLocation())) {
            event.setSound(new VoiceSound(sound));
        }
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SoundOptionsScreen)) return;
        var mc = Minecraft.getInstance();
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof OptionsList list)) continue;
            var voice = mc.options.getSoundSourceOptionInstance(SoundSource.VOICE);
            var anchor = list.findOption(voice);
            if (anchor == null) continue;
            var option = ClientOptions.voiceVolume();
            for (int i = 0; i < list.children().size(); i++) {
                ContainerObjectSelectionList.Entry<?> row = list.children().get(i);
                if (!row.children().contains(anchor)) continue;
                if (row.children().size() == 1) {
                    list.addSmall(voice, option);
                    Collections.swap(list.children(), i, list.children().size() - 1);
                    list.children().removeLast();
                } else {
                    list.addSmall(option);
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void close(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof SoundOptionsScreen) ClientConfig.SPEC.save();
    }

    private VoiceOptions() {}
}
