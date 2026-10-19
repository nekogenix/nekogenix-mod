package io.github.nekogenix;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class SicknessOverlaySmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.overlaySmokeTest");

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 40) {
            var level = player.serverLevel();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    new net.minecraft.world.phys.AABB(-8, -64, 16, 8, -48, 32)).forEach(net.minecraft.world.entity.Entity::discard);
            for (BlockPos pos : BlockPos.betweenClosed(-4, -61, 20, 4, -54, 28)) {
                boolean wall = Math.abs(pos.getX()) == 4 || pos.getY() == -61 || pos.getY() == -54 || pos.getZ() == 20 || pos.getZ() == 28;
                level.setBlockAndUpdate(pos, (wall ? Blocks.GRAY_CONCRETE : Blocks.AIR).defaultBlockState());
            }
            level.setBlockAndUpdate(new BlockPos(0, -54, 24), Blocks.LIGHT.defaultBlockState());
            player.removeAllEffects();
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
            player.teleportTo(level, .5, -60, 24.5, 0, 0);
        }
        if (age == 220) check(player.hasEffect(Content.SHOCK), "Normal injection starts sickness");
        if (age == 1410) {
            check(!player.hasEffect(Content.SHOCK) && player.hasEffect(Content.TRANSFORMING), "Sickness ends naturally after its full duration");
            Nekogenix.LOGGER.info("Nekogenix overlay server passed: full injection and natural sickness completion");
        }
        if (age == 1440) player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400));
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException("Nekogenix overlay smoke failed: " + message);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static int samples;
        private static boolean control;
        private static double previousScale;
        private static boolean previousGui;
        private static int previousFov;

        @SubscribeEvent
        public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            boolean sickness = age >= 200 && age <= 1410;
            if (!(sickness && (age % 5 == 0 || age >= 1340) || age >= 1580)) return;
            boolean green = false;
            try (var picture = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                for (int column = 1; column <= 9; column++) {
                    for (int row = 1; row <= 8; row++) {
                        int x = picture.getWidth() * column / 10, y = picture.getHeight() * row / 10;
                        int pixel = picture.getPixelRGBA(x, y);
                        int red = pixel & 255, g = pixel >> 8 & 255, blue = pixel >> 16 & 255;
                        if (g > Math.max(red, blue) + 8) green = true;
                    }
                }
                if (sickness && green) picture.writeToFile(new java.io.File(mc.gameDirectory, "screenshots/sickness-overlay-failure.png"));
                if (!sickness && green && !control) picture.writeToFile(new java.io.File(mc.gameDirectory, "screenshots/sickness-overlay-ordinary.png"));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            if (sickness) {
                samples++;
                if (green) {
                    check(false, "Green framebuffer overlay at tick " + age + "; sickness=" + mc.player.hasEffect(Content.SHOCK)
                            + "; nausea=" + mc.player.getEffect(MobEffects.CONFUSION) + "; intensity=" + mc.player.spinningEffectIntensity);
                }
            } else if (green) control = true;
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 45) {
                previousScale = mc.options.screenEffectScale().get();
                previousGui = mc.options.hideGui;
                previousFov = mc.options.fov().get();
                mc.options.hideGui = false;
                mc.options.fov().set(70);
                mc.options.screenEffectScale().set(0.0);
            }
            mc.options.keyAttack.setDown(age >= 80 && age < 180);
            if (age == 500) mc.options.screenEffectScale().set(.5);
            if (age == 600 || age == 750) mc.options.screenEffectScale().set(0.0);
            if (age == 700) mc.options.screenEffectScale().set(1.0);
            if (age == 250 || age == 900 || age == 1420) {
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "sickness-overlay-" + age + ".png", mc.getMainRenderTarget(), message -> {});
            }
            if (age == 1640) {
                check(samples > 100 && control, "Framebuffer sampling detects ordinary green nausea but never sickness overlays");
                mc.options.screenEffectScale().set(previousScale);
                mc.options.hideGui = previousGui;
                mc.options.fov().set(previousFov);
                Nekogenix.LOGGER.info("Nekogenix overlay smoke passed: full sickness cycle and transition sampled without green overlay; ordinary nausea detected");
                mc.stop();
            }
        }
    }
}
