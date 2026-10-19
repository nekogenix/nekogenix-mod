package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.FelineJump;
import io.github.nekogenix.network.AirJumpInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class FelineJumpInput {
    private static boolean held;
    private static LocalPlayer restoreJump;

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player) return;
        boolean down = mc.screen == null && event.getInput().jumping;
        var player = mc.player;
        if (down && !held) {
            boolean consumed = FelineJump.jump(player);
            if (consumed) {
                PacketDistributor.sendToServer(new AirJumpInput());
            } else if (player.mayFly() && FelineJump.canStartFlight(player)) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                consumed = true;
            }
            if (consumed) {
                event.getInput().jumping = false;
                restoreJump = player;
            }
        }
        held = down;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = restoreJump;
        restoreJump = null;
        var mc = Minecraft.getInstance();
        if (player != null && player == mc.player && player.isAlive() && mc.screen == null && held) player.input.jumping = true;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { held = false; restoreJump = null; }

    private FelineJumpInput() {}
}
