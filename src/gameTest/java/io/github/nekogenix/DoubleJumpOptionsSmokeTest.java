package io.github.nekogenix;

import io.github.nekogenix.config.ClientConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class DoubleJumpOptionsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.doubleJumpOptionsSmokeTest");
    private static int jumps;
    private static boolean airJumpUsed;
    private static boolean elytraSeen;

    @SubscribeEvent
    public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        boolean used = io.github.nekogenix.gameplay.FelineJump.canStartFlight(player);
        if (used && !airJumpUsed) jumps++;
        airJumpUsed = used;
        int age = player.tickCount;
        if (age == 20 || age == 140 || age == 260 || age == 380) {
            CatTransformationTests.cat(player);
            player.stopFallFlying();
            player.setGameMode(age == 260 ? GameType.CREATIVE : GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.getFoodData().setFoodLevel(20);
            player.setItemSlot(EquipmentSlot.CHEST, age == 140 ? Items.ELYTRA.getDefaultInstance() : ItemStack.EMPTY);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age >= 180 && age < 240 && player.isFallFlying()) elytraSeen = true;
        if (age == 240) check(elytraSeen && jumps == 0, "Disabled double jump allows vanilla elytra activation without an air jump");
        if (age == 330) check(player.getAbilities().flying && jumps == 0, "Disabled double jump allows vanilla creative flight");
        if (age == 470) {
            check(jumps == 1, "The server receives no disabled air jumps and authorizes one after enabling");
            Nekogenix.LOGGER.info("Nekogenix double jump options server smoke passed: disabled requests absent vanilla flight retained reenabled jump authorized");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            if (age == 30) ClientConfig.NEKO_DOUBLE_JUMP.set(false);
            if (age == 400) ClientConfig.NEKO_DOUBLE_JUMP.set(true);
            mc.options.keyJump.setDown(age == 60 || age == 68 || age == 180 || age == 188
                    || age == 300 || age == 305 || age == 420 || age == 428);
            if (age == 70) check(player.getDeltaMovement().y <= 0, "Disabled double jump cannot reverse descent");
            if (age == 190) check(player.isFallFlying(), "Elytra activate on the second jump press while disabled");
            if (age == 307) check(player.getAbilities().flying, "Creative flight uses the vanilla double tap while disabled");
            if (age == 430) check(player.getDeltaMovement().y > 0, "Enabling the preference restores the air jump immediately");
            if (age == 480) {
                Nekogenix.LOGGER.info("Nekogenix double jump options client smoke passed: disabled jump vanilla elytra and creative flight and immediate reenabling");
                mc.stop();
            }
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Double jump options smoke failed: " + message);
    }
}
