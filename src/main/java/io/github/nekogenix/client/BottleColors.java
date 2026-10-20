package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.BottledEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BottleColors {
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, index) -> index == 0 ? 0xFF000000 | BottledEffects.color(stack) : -1,
                Content.LINGERING_SALIVA.get(), Content.SPLASH_SWEET_AMETHYST.get(), Content.LINGERING_SWEET_AMETHYST.get());
    }

    private BottleColors() {}
}
