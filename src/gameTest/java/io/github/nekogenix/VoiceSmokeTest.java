package io.github.nekogenix;

import com.mojang.authlib.GameProfile;
import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.FelineSounds;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class VoiceSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.voiceSmokeTest");
    private static ServerPlayer guest;

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player == guest) return;
        if (player.tickCount == 170) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "VoiceGuest"), false);
            guest = new ServerPlayer(player.server, player.serverLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            player.server.getPlayerList().placeNewPlayer(connection, guest, cookie);
            guest.teleportTo(player.getX() + 3, player.getY(), player.getZ());
            guest.getData(Content.NEKO).begin();
            FelineSounds.sync(guest);
        }
        if ((player.tickCount == 205 || player.tickCount == 235) && guest != null) {
            player.level().playSound(null, player, VoiceSounds.MEOW.get(), SoundSource.PLAYERS, 0.5F, 1F);
            for (var sound : new net.minecraft.sounds.SoundEvent[] {
                    VoiceSounds.STRAY_MEOW.get(), VoiceSounds.BEG.get(), VoiceSounds.PURR.get(), VoiceSounds.PURREOW.get()}) {
                player.level().playSound(null, guest, sound, SoundSource.PLAYERS, 0.5F, 1F);
            }
            for (var sound : new net.minecraft.sounds.SoundEvent[] {SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_DEATH,
                    SoundEvents.GENERIC_EAT, SoundEvents.GENERIC_DRINK, SoundEvents.PLAYER_BURP}) guest.playSound(sound, 0.8F, 1F);
            player.level().playSound(null, guest, SoundEvents.CAT_HISS, SoundSource.PLAYERS, 0.6F, 1F);
            player.level().playSound(null, guest, SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.5F, 1F);
        }
        if (player.tickCount == 280 && guest != null) {
            player.server.getPlayerList().remove(guest);
            guest = null;
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double originalVoice;
        private static double originalPlayers;
        private static boolean breath;
        private static int mutedVoices;
        private static int halfVoices;
        private static int actions;
        private static boolean own;
        private static boolean other;
        private static net.minecraft.client.gui.components.AbstractSliderButton slider;

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (!ENABLED || event.getSound() == null) return;
            var sound = event.getSound();
            if (sound.getLocation().equals(SoundEvents.PLAYER_BREATH.getLocation())) {
                sound.resolve(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                breath |= Math.abs(sound.getPitch() - 0.65F) < 0.001F;
            }
            if (slider == null) return;
            if (VoiceSounds.isVoice(sound.getLocation())) {
                sound.resolve(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                double volume = ClientConfig.NEKO_VOICE_VOLUME.get();
                check(sound.getSource() == SoundSource.MASTER, "Ambient voices have an independent volume");
                if (volume == 0) {
                    check(sound.getVolume() == 0, "Zero voice volume mutes ambient sounds");
                    mutedVoices++;
                    if (sound.getLocation().equals(VoiceSounds.MEOW.getId())) own = true;
                    else other = true;
                } else if (volume == 0.5) {
                    check(sound.getVolume() > 0 && sound.getVolume() <= 0.5F, "Half volume remains audible with Players muted");
                    halfVoices++;
                }
            } else if (sound.getSource() == SoundSource.PLAYERS || sound.getSource() == SoundSource.NEUTRAL) {
                check(sound == event.getOriginalSound(), "Action and ordinary cat sounds are not modified");
                actions++;
            }
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            if (age == 185) {
                originalVoice = ClientConfig.NEKO_VOICE_VOLUME.get();
                originalPlayers = mc.options.getSoundSourceOptionInstance(SoundSource.PLAYERS).get();
                mc.options.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(1.0);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.setScreen(new net.minecraft.client.gui.screens.options.SoundOptionsScreen(null, mc.options));
            }
            if (age == 195) {
                var list = (net.minecraft.client.gui.components.OptionsList) mc.screen.children().stream()
                        .filter(child -> child instanceof net.minecraft.client.gui.components.OptionsList).findFirst().orElseThrow();
                var anchor = list.findOption(mc.options.getSoundSourceOptionInstance(SoundSource.VOICE));
                for (int i = 0; i < list.children().size(); i++) {
                    net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> row = list.children().get(i);
                    if (row.children().contains(anchor)) {
                        check(row.children().size() == 2, "Neko Voice fills the empty right hand slot");
                        slider = (net.minecraft.client.gui.components.AbstractSliderButton) row.children().get(1);
                    }
                }
                check(slider != null && slider.getX() > anchor.getX() && slider.getY() == anchor.getY(), "Slider uses the correct row");
                move(0);
            }
            if (age == 215) {
                check(breath && mutedVoices >= 5 && own && other && actions >= 7, "Breath, local and remote voices, and action sounds checked");
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "neko-voice-prealpha3.png", mc.getMainRenderTarget(), message -> {});
            }
            if (age == 225) {
                move(0.5);
                mc.options.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(0.0);
            }
            if (age == 250) {
                check(halfVoices >= 5, "Ambient voices remain independent of Players volume");
                mc.screen.onClose();
                try {
                    String saved = java.nio.file.Files.readString(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("nekogenix-client.toml"));
                    check(saved.contains("nekoVoiceVolume = 0.5"), "Voice volume saved to client config");
                } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
                Nekogenix.LOGGER.info("Nekogenix voice smoke passed: slow breath, correct slider position, local and remote voices muted, action sounds preserved, independent volume and client persistence verified");
                ClientConfig.NEKO_VOICE_VOLUME.set(originalVoice);
                ClientConfig.SPEC.save();
                mc.options.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(originalPlayers);
                mc.options.save();
            }
            if (age >= 290) mc.stop();
        }

        private static void move(double value) {
            slider.mouseClicked(slider.getX() + 4 + (slider.getWidth() - 8) * value, slider.getY() + 10, 0);
            slider.mouseReleased(slider.getX(), slider.getY(), 0);
        }

        private static void check(boolean passed, String message) {
            if (!passed) throw new IllegalStateException("Nekogenix voice smoke failed: " + message);
        }
    }
}
