package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class InventoryAbilities {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private static final ResourceLocation SCENT_ICON = Content.id("textures/mob_effect/feline_scent.png");

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        var abilities = abilities();
        var cells = InventoryAbilityLayout.locate(screen, abilities.size());
        if (cells.isEmpty()) return;
        var graphics = event.getGuiGraphics();
        for (int i = 0; i < abilities.size(); i++) {
            var cell = cells.get(i);
            var ability = abilities.get(i);
            graphics.blitSprite(BACKGROUND, cell.left(), cell.top(), cell.width(), cell.height());
            int x = cell.left() + (cell.width() - 18) / 2;
            int y = cell.top() + (cell.height() - 18) / 2;
            if (ability.equals(MobEffects.CONDUIT_POWER)) graphics.blit(SCENT_ICON, x, y, 0, 0, 18, 18, 18, 18);
            else graphics.blit(x, y, 0, 18, 18, mc.getMobEffectTextures().get(ability));
        }
        for (int i = 0; i < abilities.size(); i++) {
            if (cells.get(i).containsPoint(event.getMouseX(), event.getMouseY())) {
                var ability = abilities.get(i);
                Component name = ability.equals(MobEffects.CONDUIT_POWER) ? Component.translatable("ability.nekogenix.feline_scent")
                        : ability.value().getDisplayName();
                graphics.renderTooltip(mc.font, name, event.getMouseX(), event.getMouseY());
                break;
            }
        }
    }

    public static List<ScreenRectangle> bounds(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) return List.of();
        return InventoryAbilityLayout.locate(screen, abilities().size());
    }

    private static List<Holder<MobEffect>> abilities() {
        var abilities = new ArrayList<Holder<MobEffect>>(4);
        var mc = Minecraft.getInstance();
        if (mc.player == null) return abilities;
        for (var effect : java.util.List.of(Content.AGILITY, Content.VISION, Content.PAWS)) {
            if (mc.player.hasEffect(effect)) abilities.add(effect);
        }
        if (CatForm.active(mc.player)) abilities.add(MobEffects.CONDUIT_POWER);
        return abilities;
    }

    private InventoryAbilities() {}
}
