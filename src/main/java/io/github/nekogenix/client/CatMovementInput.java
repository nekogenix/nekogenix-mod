package io.github.nekogenix.client;

import io.github.nekogenix.Content;
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
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        var input = event.getInput();
        if (io.github.nekogenix.gameplay.FelineWetness.active(player)) {
            var sprint = player.getAttribute(Attributes.MOVEMENT_SPEED);
            var sneak = player.getAttribute(Attributes.SNEAKING_SPEED);
            if (sprint != null) sprint.removeModifier(Content.id("feline_sprint"));
            if (sneak != null) sneak.removeModifier(Content.id("feline_sneak"));
        }
        if (input.shiftKeyDown && player.hasEffect(Content.AGILITY) && !player.isSwimming()
                && !player.getAbilities().flying && !player.isFallFlying()) {
            player.setSprinting(false);
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(Content.id("feline_sprint"));
            if (!player.isMovingSlowly()) {
                float sneak = (float)player.getAttributeValue(Attributes.SNEAKING_SPEED);
                input.forwardImpulse *= sneak;
                input.leftImpulse *= sneak;
            }
        }
        if (!CatForm.active(player) && !io.github.nekogenix.gameplay.CameraPosture.freeProne(player)) return;
        if (input.shiftKeyDown || !player.isMovingSlowly() || player.isSwimming()) return;
        float factor = (float)player.getAttributeValue(Attributes.SNEAKING_SPEED);
        if (factor > 0) {
            input.forwardImpulse = Mth.clamp(input.forwardImpulse / factor, -1, 1);
            input.leftImpulse = Mth.clamp(input.leftImpulse / factor, -1, 1);
        }
    }

    private CatMovementInput() {}
}
