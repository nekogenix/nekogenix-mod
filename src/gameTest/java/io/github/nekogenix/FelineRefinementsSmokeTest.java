package io.github.nekogenix;

import io.github.nekogenix.gameplay.FelineSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineRefinementsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.refinementsSmokeTest");
    private static int airJumps;
    private static boolean airJumpUsed;

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        boolean used = io.github.nekogenix.gameplay.FelineJump.canStartFlight(player);
        if (used && !airJumpUsed) airJumps++;
        airJumpUsed = used;
        int age = player.tickCount;
        if (age == 40 || age == 460) {
            player.removeAllEffects();
            player.setGameMode(GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5);
            player.serverLevel().setDayTime(6000);
            TransformationTests.setState(player, 4);
            FelineSounds.sync(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            player.serverLevel().setBlockAndUpdate(new BlockPos(0, -61, 24), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.teleportTo(player.serverLevel(), .5, -60, 24.5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age == 80 || age == 160) {
            player.getFoodData().setFoodLevel(0);
            player.getFoodData().setSaturation(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, (age == 80 ? Items.BEEF : Items.SPIDER_EYE).getDefaultInstance().copyWithCount(2));
        }
        if (age == 140) check(player.getFoodData().getFoodLevel() == 8, "Server beef nutrition is eight");
        if (age == 230) check(player.getFoodData().getFoodLevel() == 1 && !player.hasEffect(MobEffects.POISON), "Spider eye is safe for neko on the server");
        if (age == 250) {
            player.removeAllEffects();
            player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            TransformationTests.setState(player, 0);
        }
        if (age == 380) {
            player.removeAllEffects();
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400));
        }
        if (age == 540) {
            check(airJumps == 1, "The server accepts one air jump and rejects repeated requests");
            Nekogenix.LOGGER.info("Nekogenix refinements server passed: native nutrition, safe spider eye and one authorized air jump");
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException("Nekogenix refinements smoke failed: " + message);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean off, partial, full;
        private static double previousScale;

        @SubscribeEvent
        public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || !mc.player.hasEffect(Content.SHOCK)) return;
            double scale = mc.options.screenEffectScale().get();
            if (scale < 1) {
                check(mc.player.spinningEffectIntensity == 0 && mc.player.oSpinningEffectIntensity == 0,
                        "Reduced distortion cannot render the vanilla green nausea overlay");
                if (scale == 0) off = true;
                else partial = true;
            } else {
                check(mc.player.spinningEffectIntensity == 1.5F, "Full sickness distortion remains available");
                full = true;
            }
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 45) previousScale = mc.options.screenEffectScale().get();
            mc.options.keyUse.setDown(age >= 90 && age < 123 || age >= 180 && age < 213);
            if (age >= 90 && age < 150) {
                int food = mc.player.getFoodData().getFoodLevel();
                check(food == 0 || food == 8, "Client must never predict vanilla beef nutrition before correction");
            }
            if (age == 135) {
                check(mc.player.getFoodData().getFoodLevel() == 8 && mc.player.getMainHandItem().getCount() == 1,
                        "Client eats exactly one beef with neko nutrition");
                check(mc.player.getMainHandItem().get(DataComponents.FOOD).equals(Items.BEEF.getDefaultInstance().get(DataComponents.FOOD)),
                        "Client restores food components after eating");
            }
            if (age == 225) check(mc.player.getFoodData().getFoodLevel() == 1 && !mc.player.hasEffect(MobEffects.POISON), "Client spider eye nutrition and effects match server");
            if (age == 260 || age == 400) mc.options.screenEffectScale().set(0.0);
            if (age == 300) mc.options.screenEffectScale().set(.5);
            if (age == 340) mc.options.screenEffectScale().set(1.0);
            if (age == 280 || age == 320 || age == 360) {
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "sickness-prealpha8-" + age + ".png", mc.getMainRenderTarget(), message -> {});
            }
            if (age == 440) check(!mc.player.hasEffect(Content.SHOCK) && mc.player.hasEffect(MobEffects.CONFUSION)
                    && mc.player.spinningEffectIntensity > 0, "Ordinary nausea retains its vanilla accessibility overlay");
            mc.options.keyJump.setDown(age == 490 || age == 500 || age == 506);
            if (age == 503) check(mc.player.getDeltaMovement().y > 0, "Second jump reverses descent on the client");
            if (age == 505) net.neoforged.neoforge.network.PacketDistributor.sendToServer(new io.github.nekogenix.network.AirJumpInput());
            if (age == 550) {
                check(off && partial && full, "All distortion settings were rendered during sickness");
                mc.options.screenEffectScale().set(previousScale);
                Nekogenix.LOGGER.info("Nekogenix refinements smoke passed: nutrition without rollback, safe spider eye, double jump and accessibility settings");
                mc.stop();
            }
        }
    }
}
