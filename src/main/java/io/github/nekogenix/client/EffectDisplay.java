package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

final class EffectDisplay implements IClientMobEffectExtensions {
    static void register(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new EffectDisplay(), Content.SHOCK, Content.TRANSFORMING, Content.CAT_TRANSFORMING);
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) { return false; }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) { return false; }
        }, Content.AGILITY, Content.VISION, Content.PAWS);
        if (!event.isMobEffectRegistered(MobEffects.NIGHT_VISION.value())) {
            event.registerMobEffect(new IClientMobEffectExtensions() {
                private boolean visible() {
                    var player = Minecraft.getInstance().player;
                    return player == null || !player.hasEffect(Content.VISION);
                }

                @Override
                public boolean isVisibleInInventory(MobEffectInstance instance) { return visible(); }

                @Override
                public boolean isVisibleInGui(MobEffectInstance instance) { return visible(); }
            }, MobEffects.NIGHT_VISION);
        }
        if (!event.isMobEffectRegistered(MobEffects.CONFUSION.value())) {
            event.registerMobEffect(new IClientMobEffectExtensions() {
                private boolean visible(MobEffectInstance instance) {
                    var player = Minecraft.getInstance().player;
                    return player == null || !player.hasEffect(Content.SHOCK) || instance.isVisible() || instance.showIcon();
                }

                @Override
                public boolean isVisibleInInventory(MobEffectInstance instance) { return visible(instance); }

                @Override
                public boolean isVisibleInGui(MobEffectInstance instance) { return visible(instance); }
            }, MobEffects.CONFUSION);
        }
    }

    @Override
    public boolean renderInventoryText(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
            GuiGraphics graphics, int x, int y, int blitOffset) {
        var mc = Minecraft.getInstance();
        var name = instance.getEffect().value().getDisplayName().copy();
        if (instance.is(Content.TRANSFORMING) || instance.getAmplifier() > 0) {
            name.append(" ").append(Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)));
        }
        float scale = Math.min(1F, 86F / Math.max(1, mc.font.width(name)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 28, y + 6, blitOffset);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(mc.font, name, 0, 0, 0xFFFFFF);
        graphics.pose().popPose();
        float tickRate = mc.level == null ? 20F : mc.level.tickRateManager().tickrate();
        graphics.drawString(mc.font, MobEffectUtil.formatDuration(instance, 1F, tickRate), x + 28, y + 16, 0x7F7F7F);
        return true;
    }

    private EffectDisplay() {}
}
