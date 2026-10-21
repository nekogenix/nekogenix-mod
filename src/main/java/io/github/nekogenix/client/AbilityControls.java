package io.github.nekogenix.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.network.FelineVisionInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class AbilityControls {
    public static final KeyMapping DOUBLE_JUMP = new KeyMapping("key.nekogenix.double_jump", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.nekogenix");
    public static final KeyMapping FELINE_VISION = new KeyMapping("key.nekogenix.feline_vision", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.nekogenix");
    private static LocalPlayer observer;
    private static boolean automatic, enabled, sent, sentAutomatic, sentEnabled;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (observer != mc.player) {
            observer = mc.player;
            automatic = ClientConfig.AUTOMATIC_FELINE_VISION.get();
            enabled = sent = false;
        }
        boolean mode = ClientConfig.AUTOMATIC_FELINE_VISION.get();
        if (automatic != mode) {
            automatic = mode;
            enabled = false;
        }
        if (observer == null || !observer.isAlive() || !observer.hasEffect(Content.VISION)) enabled = false;
        boolean inGame = observer != null && observer.isAlive() && mc.screen == null;
        while (DOUBLE_JUMP.consumeClick()) {
            if (!inGame) continue;
            boolean value = !ClientConfig.NEKO_DOUBLE_JUMP.get();
            ClientConfig.NEKO_DOUBLE_JUMP.set(value);
            ClientConfig.SPEC.save();
            status("options.nekogenix.double_jump", value);
        }
        while (FELINE_VISION.consumeClick()) {
            if (!inGame || automatic || !observer.hasEffect(Content.VISION)) continue;
            enabled = !enabled;
            status("effect.nekogenix.feline_vision", enabled);
        }
        if (observer != null && (!sent || automatic != sentAutomatic || enabled != sentEnabled)) {
            PacketDistributor.sendToServer(new FelineVisionInput(automatic, enabled));
            sent = true;
            sentAutomatic = automatic;
            sentEnabled = enabled;
        }
    }

    private static void status(String key, boolean value) {
        observer.displayClientMessage(Options.genericValueLabel(Component.translatable(key),
                value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF), true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        observer = null;
        enabled = sent = false;
        while (DOUBLE_JUMP.consumeClick()) {}
        while (FELINE_VISION.consumeClick()) {}
    }

    private AbilityControls() {}
}
