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
                    CentrifugeRenderer.LID, CentrifugeRenderer.ROTOR, CentrifugeRenderer.VIAL, CentrifugeRenderer.LIQUID, CentrifugeRenderer.CORK}) event.register(model);
        }
    }

    private CentrifugeClient() {}
}
