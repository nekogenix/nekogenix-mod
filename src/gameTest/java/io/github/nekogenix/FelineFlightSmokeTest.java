package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineFlightSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.felineFlightSmokeTest");
    private static int airJumps;
    private static boolean airJumpUsed;
    private static boolean elytraSeen;

    @SubscribeEvent
    public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        boolean used = io.github.nekogenix.gameplay.FelineJump.canStartFlight(player);
        if (used && !airJumpUsed) airJumps++;
        airJumpUsed = used;
        int age = player.tickCount;
        if (age == 20 || age == 180 || age == 320 || age == 460) {
            player.stopFallFlying();
            player.setGameMode(age >= 320 ? GameType.CREATIVE : GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20);
            player.setItemSlot(EquipmentSlot.CHEST, age == 180 ? Items.ELYTRA.getDefaultInstance() : ItemStack.EMPTY);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (age == 460) {
                player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
                TransformationHandler.refreshEffects(player);
            } else CatTransformationTests.cat(player);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age >= 220 && age < 320 && player.isFallFlying()) elytraSeen = true;
        if (age == 260) check(elytraSeen, "The server accepts elytra flight after the double jump");
        if (age == 400) check(player.getAbilities().flying, "The server accepts creative flight on the third press");
        if (age == 535) {
            check(airJumps == 3, "The server authorizes exactly one air jump in each feline scenario");
            check(player.getAbilities().flying, "Ordinary human creative flight remains available");
            Nekogenix.LOGGER.info("Nekogenix feline flight server smoke passed: three authorized jumps elytra and creative flight accepted human flight unchanged");
        }
        if (age == 540) {
            CatTransformationTests.cat(player);
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.serverLevel().setBlockAndUpdate(new net.minecraft.core.BlockPos(0, 39, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.teleportTo(player.serverLevel(), .5, 40, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age == 635) {
            check(airJumps == 3, "The server rejects the jump after more than twenty airborne ticks");
            Nekogenix.LOGGER.info("Nekogenix feline flight deadline server smoke passed: late request rejected without a jump sound");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static net.minecraft.world.phys.Vec3 momentum;
        private static int predictions;

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            mc.getToasts().clear();
            mc.options.keyUp.setDown(age >= 40 && age < 125 || age >= 200 && age < 285 || age >= 340 && age < 415);
            mc.options.keySprint.setDown(mc.options.keyUp.isDown());
            if (age >= 580 && age < 630) mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(age == 60 || age >= 68 && age < 76 || age == 82
                    || age == 220 || age >= 228 && age < 236 || age == 238
                    || age == 360 || age >= 368 && age < 376 || age >= 384 && age < 392
                    || age == 500 || age == 505 || age == 580 || age == 605);
            if (age == 70 || age == 230 || age == 370) {
                check(!player.getAbilities().flying && !player.isFallFlying() && player.getDeltaMovement().y > 0,
                        "The second press performs a jump instead of flight at " + age);
            }
            if (age >= 229 && age < 238 || age >= 369 && age < 384) {
                check(!player.isFallFlying() && !player.getAbilities().flying, "Holding or releasing the second press cannot activate flight");
            }
            if (age >= 69 && age <= 76 || age >= 229 && age <= 236 || age >= 369 && age <= 376) {
                var velocity = player.getDeltaMovement();
                double forward = (velocity.x * momentum.x + velocity.z * momentum.z) / Math.sqrt(momentum.horizontalDistanceSqr());
                check(forward > Math.sqrt(momentum.horizontalDistanceSqr()) * .4, "Horizontal momentum survives server synchronization at " + age);
            }
            if (age == 239) check(player.isFallFlying(), "The third press deploys the elytra");
            if (age == 385) check(player.getAbilities().flying, "The third press starts creative flight after the vanilla double tap timer expires");
            if (age == 390) check(player.getAbilities().flying && player.getDeltaMovement().y > 0, "Holding the third press ascends without toggling flight back off");
            if (age == 506) check(player.getAbilities().flying && !player.hasEffect(Content.AGILITY), "Human creative flight still uses the vanilla double tap");
            if (age == 607) check(!player.onGround() && player.getDeltaMovement().y < 0,
                    "A jump press after one second cannot reverse a long fall");
            if (age == 640) {
                check(predictions == 3, "All three second jumps were predicted exactly once");
                Nekogenix.LOGGER.info("Nekogenix feline flight client smoke passed: second press jumps third press flies momentum survives synchronization and late jumps are rejected");
                mc.stop();
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void before(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
            if (!ENABLED) return;
            int age = event.getEntity().tickCount;
            if (age == 69 || age == 229 || age == 369) {
                momentum = event.getEntity().getDeltaMovement();
                check(momentum.horizontalDistanceSqr() > .01, "The second jump starts with meaningful sprint momentum");
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void after(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
            if (!ENABLED) return;
            int age = event.getEntity().tickCount;
            if (age == 69 || age == 229 || age == 369) {
                var actual = event.getEntity().getDeltaMovement();
                var up = io.github.nekogenix.integration.sable.FelineOrientation.up(event.getEntity());
                check(Math.abs(actual.dot(up) - .74722065) < .001
                                && actual.subtract(up.scale(actual.dot(up))).distanceTo(momentum.subtract(up.scale(momentum.dot(up)))) < 1.0E-8,
                        "The air jump changes only velocity along the local up axis");
                check(!event.getInput().jumping, "Vanilla flight handling does not consume the second press");
                predictions++;
            }
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Feline flight smoke failed: " + message);
    }
}
