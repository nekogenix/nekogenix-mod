package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CentrifugeClient {
    private static int menuId = -1;
    private static int filled;
    private static boolean card;

    @SubscribeEvent
    public static void brewingSound(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.containerMenu instanceof BrewingStandMenu menu)) { menuId = -1; return; }
        int current = 0;
        for (int slot = 0; slot < 3; slot++) if (menu.getSlot(slot).getItem().is(Content.INJECTION)) current++;
        if (menu.containerId == menuId && card && current == filled + 1 && menu.getSlot(3).getItem().is(Content.BLANK_NUCLEOCARD)) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, .8F, 1F, false);
        }
        menuId = menu.containerId;
        filled = current;
        card = menu.getSlot(3).getItem().is(Content.CAT_NUCLEOCARD);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) { event.register(Content.CENTRIFUGE_MENU.get(), CentrifugeScreen::new); }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Content.CENTRIFUGE_ENTITY.get(), CentrifugeRenderer::new); }
        @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
            for (var model : new net.minecraft.client.resources.model.ModelResourceLocation[] {
                    CentrifugeRenderer.LID, CentrifugeRenderer.GLASS, CentrifugeRenderer.ROTOR, CentrifugeRenderer.VIAL, CentrifugeRenderer.LIQUID, CentrifugeRenderer.CORK}) event.register(model);
        }
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                var property = Content.id("brewing_progress");
                net.minecraft.client.renderer.item.ItemProperties.register(Content.CAT_NUCLEOCARD.get(), property,
                        (stack, level, entity, seed) -> brewingFrame(stack, true));
                net.minecraft.client.renderer.item.ItemProperties.register(Content.SYRINGE.get(), property,
                        (stack, level, entity, seed) -> brewingFrame(stack, false));
            });
        }
    }

    private static float brewingFrame(net.minecraft.world.item.ItemStack stack, boolean card) {
        var mc = Minecraft.getInstance();
        if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.BrewingStandScreen screen)) return 0;
        var menu = screen.getMenu();
        int remaining = menu.getBrewingTicks();
        if (remaining <= 0 || !menu.getSlot(3).getItem().is(Content.CAT_NUCLEOCARD)) return 0;
        for (int slot = 0; slot < 3; slot++) {
            var syringe = menu.getSlot(slot).getItem();
            if (syringe.is(Content.SYRINGE)) {
                return stack == (card ? menu.getSlot(3).getItem() : syringe)
                        ? Math.min(8, (400 - remaining) / 50) : 0;
            }
        }
        return 0;
    }

    private CentrifugeClient() {}
}
