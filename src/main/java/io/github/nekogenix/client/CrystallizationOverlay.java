package io.github.nekogenix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CrystallizationEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CrystallizationOverlay {
    private static LocalPlayer observer;
    private static float previous, strength;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (observer != mc.player) {
            observer = mc.player;
            previous = strength = 0;
        }
        if (observer == null || mc.isPaused()) return;
        previous = strength;
        var effect = observer.getEffect(Content.CRYSTALLIZATION);
        strength = effect == null ? Math.max(0, strength - 1F / CrystallizationEffect.BLEND_TICKS) : effect.getBlendFactor(observer, 1);
    }

    public static float strength(float partial) {
        return Mth.lerp(Mth.clamp(partial, 0, 1), previous, strength);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Content.id("crystallization"), (graphics, delta) -> {
                var mc = Minecraft.getInstance();
                if (mc.player != observer || observer == null || !observer.isAlive() || observer.isSpectator()
                        || !mc.options.getCameraType().isFirstPerson()) return;
                float alpha = strength(delta.getGameTimeDeltaPartialTick(false));
                if (alpha <= 0) return;
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.setColor(1, 1, 1, alpha);
                graphics.blit(Content.id("textures/misc/crystallization_outline.png"), 0, 0, -90, 0F, 0F,
                        graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
                graphics.setColor(1, 1, 1, 1);
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
            });
        }
    }

    private CrystallizationOverlay() {}
}
