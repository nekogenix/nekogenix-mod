package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CatMovementInput {
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !CatForm.active(player)) return;
        var input = event.getInput();
        if (input.shiftKeyDown || !player.isMovingSlowly() || player.isSwimming()) return;
        float factor = (float)player.getAttributeValue(Attributes.SNEAKING_SPEED);
        if (factor > 0) {
            input.forwardImpulse = Mth.clamp(input.forwardImpulse / factor, -1, 1);
            input.leftImpulse = Mth.clamp(input.leftImpulse / factor, -1, 1);
        }
    }

    private CatMovementInput() {}
}
