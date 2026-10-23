package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CameraPosture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class PassageInput {
    private static boolean held, consumed;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void input(MovementInputUpdateEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player) return;
        if (CameraPosture.passage(event.getEntity())) {
            CameraPosture.prone(event.getEntity(), io.github.nekogenix.config.ClientConfig.cameraHeight());
        }
        var input = event.getInput();
        boolean down = minecraft.screen == null && input.jumping;
        boolean crouching = minecraft.screen == null && input.shiftKeyDown;
        if (!down) consumed = false;
        if ((down && !held || crouching) && input.forwardImpulse > 0 && !CameraPosture.passageJumpPending(event.getEntity())) {
            float yaw = event.getEntity().getYRot() * Mth.DEG_TO_RAD;
            var direction = new Vec3(input.leftImpulse * Mth.cos(yaw) - input.forwardImpulse * Mth.sin(yaw), 0,
                    input.forwardImpulse * Mth.cos(yaw) + input.leftImpulse * Mth.sin(yaw)).normalize();
            if (CameraPosture.preparePassageJump(event.getEntity(), direction, down && !held)) {
                if (!CameraPosture.passageNeedsJump(event.getEntity())) {
                    consumed = true;
                    minecraft.player.connection.send(new ServerboundPlayerCommandPacket(event.getEntity(),
                            ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
                }
                PacketDistributor.sendToServer(new io.github.nekogenix.network.PassageInput((float)direction.x, (float)direction.z, false));
            }
        }
        if (CameraPosture.passageJumpPending(event.getEntity())) {
            if (minecraft.screen != null || input.forwardImpulse <= 0
                    || !CameraPosture.passageNeedsJump(event.getEntity()) && !crouching) {
                CameraPosture.cancelPassageJump(event.getEntity());
                PacketDistributor.sendToServer(new io.github.nekogenix.network.PassageInput(0, 0, false));
            } else if (CameraPosture.enterPassage(event.getEntity())) {
                consumed = true;
                PacketDistributor.sendToServer(new io.github.nekogenix.network.PassageInput(0, 0, true));
            }
        }
        held = down;
        if (consumed) input.jumping = false;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { held = consumed = false; }

    private PassageInput() {}
}
