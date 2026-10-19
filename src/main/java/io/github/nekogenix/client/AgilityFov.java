package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class AgilityFov {
    @SubscribeEvent
    public static void compute(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (!player.hasEffect(Content.AGILITY) || event.getNewFovModifier() <= 1) return;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (!player.isSprinting() && (speed == null || speed.getModifier(Content.id("feline_sprint")) == null)) return;
        event.setNewFovModifier(1 + (event.getNewFovModifier() - 1) * .3F);
    }

    private AgilityFov() {}
}
