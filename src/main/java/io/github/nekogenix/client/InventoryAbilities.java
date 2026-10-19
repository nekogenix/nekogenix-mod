package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class InventoryAbilities {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        var abilities = new ArrayList<Holder<MobEffect>>(4);
        for (var effect : java.util.List.of(Content.AGILITY, Content.VISION, Content.PAWS)) {
            if (mc.player.hasEffect(effect)) abilities.add(effect);
        }
        if (CatForm.active(mc.player)) abilities.add(MobEffects.CONDUIT_POWER);
        if (abilities.isEmpty()) return;
        int top = screen.getGuiTop() + screen.getYSize() + (screen instanceof CreativeModeInventoryScreen ? 28 : 0) + 2;
        int size = Math.min(32, screen.height - top - 2);
        if (size < 18) return;
        int width = abilities.size() * (size + 4) - 4;
        int left = Mth.clamp(screen.getGuiLeft() + screen.getXSize() / 2 - width / 2, 2, Math.max(2, screen.width - width - 2));
        var graphics = event.getGuiGraphics();
        for (int i = 0; i < abilities.size(); i++) {
            int x = left + i * (size + 4);
            var ability = abilities.get(i);
            graphics.blitSprite(BACKGROUND, x, top, size, size);
            graphics.blit(x + (size - 18) / 2, top + (size - 18) / 2, 0, 18, 18, mc.getMobEffectTextures().get(ability));
        }
        for (int i = 0; i < abilities.size(); i++) {
            int x = left + i * (size + 4);
            if (event.getMouseX() >= x && event.getMouseX() < x + size && event.getMouseY() >= top && event.getMouseY() < top + size) {
                var ability = abilities.get(i);
                Component name = ability.equals(MobEffects.CONDUIT_POWER) ? Component.translatable("ability.nekogenix.feline_scent")
                        : ability.value().getDisplayName();
                graphics.renderTooltip(mc.font, name, event.getMouseX(), event.getMouseY());
                break;
            }
        }
    }

    private InventoryAbilities() {}
}
