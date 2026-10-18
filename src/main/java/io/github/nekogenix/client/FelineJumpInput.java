package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.FelineJump;
import io.github.nekogenix.network.AirJumpInput;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class FelineJumpInput {
    private static boolean held;

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;
        boolean down = mc.screen == null && event.getInput().jumping;
        if (down && !held && FelineJump.jump(event.getEntity())) {
            PacketDistributor.sendToServer(new AirJumpInput());
        }
        held = down;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { held = false; }

    private FelineJumpInput() {}
}
