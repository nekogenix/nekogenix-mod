package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.pneumatic.PneumaticStandMenu;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class CentrifugeClient {
    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
            event.register(Content.CENTRIFUGE_MENU.get(), CentrifugeScreen::new);
            event.register(Content.PNEUMATIC_MENU.get(), PneumaticStandScreen::new);
        }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(Content.CENTRIFUGE_ENTITY.get(), CentrifugeRenderer::new); }
        @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
            for (var model : new net.minecraft.client.resources.model.ModelResourceLocation[] {
                    CentrifugeRenderer.LID, CentrifugeRenderer.GLASS, CentrifugeRenderer.ROTOR, CentrifugeRenderer.VIAL, CentrifugeRenderer.LIQUID, CentrifugeRenderer.CORK}) event.register(model);
        }
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                var property = Content.id("brewing_progress");
                net.minecraft.client.renderer.item.ItemProperties.register(Content.CAT_NUCLEOCARD.get(), property,
                        (stack, level, entity, seed) -> brewingFrame(stack, true, false));
                net.minecraft.client.renderer.item.ItemProperties.register(Content.SYRINGE.get(), property,
                        (stack, level, entity, seed) -> brewingFrame(stack, false, false));
                net.minecraft.client.renderer.item.ItemProperties.register(Content.SYRINGE.get(), Content.id("stabilizing_progress"),
                        (stack, level, entity, seed) -> brewingFrame(stack, false, true));
                net.minecraft.client.renderer.item.ItemProperties.register(Content.NEKO_STABILIZER.get(), property,
                        (stack, level, entity, seed) -> brewingFrame(stack, true, true));
            });
        }
    }

    private static float brewingFrame(net.minecraft.world.item.ItemStack stack, boolean card, boolean stabilizing) {
        var mc = Minecraft.getInstance();
        if (!(mc.screen instanceof PneumaticStandScreen screen)) return 0;
        var menu = screen.getMenu();
        if (!menu.running() || !menu.getSlot(PneumaticStandMenu.CARD_SLOT).getItem().is(stabilizing ? Content.NEKO_STABILIZER : Content.CAT_NUCLEOCARD)) return 0;
        boolean active = card ? stack == menu.getSlot(PneumaticStandMenu.CARD_SLOT).getItem()
                : menu.filling(0) && stack == menu.getSlot(0).getItem() || menu.filling(1) && stack == menu.getSlot(1).getItem();
        return active ? Math.min(7, (int) (menu.fraction() * 8)) : 0;
    }

    private CentrifugeClient() {}
}
