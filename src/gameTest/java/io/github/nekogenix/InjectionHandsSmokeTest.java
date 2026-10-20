package io.github.nekogenix;

import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class InjectionHandsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.injectionHandsSmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20 || age == 150 || age == 280 || age == 500) {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setNoGravity(true);
            player.setHealth(20);
            player.invulnerableTime = 0;
            player.getFoodData().setFoodLevel(17);
            player.getFoodData().setSaturation(0);
            var state = new CompoundTag();
            if (age == 500) {
                state.putInt("phase", NekoState.Phase.CAT_TRANSFORMING.ordinal());
                state.putInt("abilities", 7);
                state.putInt("lives", 9);
            }
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), state);
            player.removeAllEffects();
            TransformationHandler.refreshEffects(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, age == 150 ? Items.SHIELD.getDefaultInstance()
                    : age == 500 ? net.minecraft.world.item.ItemStack.EMPTY : Content.INJECTION.toStack());
            player.setItemInHand(InteractionHand.OFF_HAND, age == 20 ? Items.SHIELD.getDefaultInstance()
                    : age == 500 ? Content.STABILIZING_INJECTION.toStack() : Content.INJECTION.toStack());
            player.inventoryMenu.broadcastChanges();
            if (age == 20) {
                player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
                player.serverLevel().setDayTime(6000);
            }
        }
        if (age == 130) check(player.getMainHandItem().is(Content.INJECTION)
                && player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN && player.getHealth() == 19,
                "Right-click main-hand cancellation retains the dose and needle contact damage");
        if (age == 255) check(player.getOffhandItem().is(Content.INJECTION) && player.getMainHandItem().is(Items.SHIELD)
                && player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN && player.getHealth() == 19,
                "Right-click offhand cancellation preserves the opposite item");
        if (age == 455) check(player.getMainHandItem().is(Content.USED_SYRINGE) && player.getOffhandItem().is(Content.USED_SYRINGE)
                && player.getData(Content.NEKO).stageDuration() == 6000 && player.getHealth() == 18
                && player.getEffect(Content.SHOCK).getDuration() > 3450,
                "Both real input buttons complete a paired dose with accelerated stages and three-minute sickness");
        if (age == 665) check(player.getMainHandItem().isEmpty() && player.getOffhandItem().is(Content.USED_SYRINGE)
                && player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO && player.hasEffect(Content.CRYSTALLIZATION),
                "The left-hand crystallizing injection stabilizes and applies its own effect");
        if (age == 755) Nekogenix.LOGGER.info("Nekogenix injection hands server smoke passed: right click either hand independent cancellation paired dose and left hand stabilization");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean mainReturn, offReturn, mainShown, offHidden, offShown, mainHidden, dualMain, dualOff;
        private static String capture;

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void hand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (!ENABLED || player == null) return;
            int age = player.tickCount;
            boolean main = event.getHand() == InteractionHand.MAIN_HAND;
            if (age >= 65 && age <= 75) {
                if (main && !event.isCanceled()) mainShown = true;
                if (!main && event.isCanceled()) offHidden = true;
            }
            if (age >= 195 && age <= 205) {
                if (!main && !event.isCanceled()) offShown = true;
                if (main && event.isCanceled()) mainHidden = true;
            }
            if (age >= 355 && age <= 375 && !event.isCanceled()) {
                if (main) dualMain = true;
                else dualOff = true;
            }
        }

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            mc.options.fov().set(70);
            mc.options.screenEffectScale().set(0D);
            mc.player.setXRot(0);
            mc.player.xRotO = 0;
            mc.options.keyUse.setDown(age >= 50 && age < 85 || age >= 180 && age < 215
                    || age >= 320 && age < 430 || age >= 530 && age < 640);
            mc.options.keyAttack.setDown(age >= 320 && age < 430);
            mainReturn |= InjectionHandler.returning(mc.player, InteractionHand.MAIN_HAND).started() >= 0;
            offReturn |= InjectionHandler.returning(mc.player, InteractionHand.OFF_HAND).started() >= 0;
            if (age == 70) capture = "injection-main-hand.png";
            if (age == 200) capture = "injection-off-hand.png";
            if (age == 310) capture = "injection-mirror-idle.png";
            if (age == 370) {
                capture = "injection-both-hands.png";
                var mainPose = new com.mojang.blaze3d.vertex.PoseStack();
                var offPose = new com.mojang.blaze3d.vertex.PoseStack();
                var main = mc.player.getMainHandItem();
                var off = mc.player.getOffhandItem();
                var right = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(main);
                var left = net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(off);
                check(right.applyForgeHandTransform(mainPose, mc.player, mc.player.getMainArm(), main, 0, 0, 0)
                        && left.applyForgeHandTransform(offPose, mc.player, mc.player.getMainArm().getOpposite(), off, 0, 0, 0),
                        "Both hands receive their injection transforms");
                check(Math.abs(mainPose.last().pose().m30() + offPose.last().pose().m30()) < .0001
                        && Math.abs(mainPose.last().pose().m31() - offPose.last().pose().m31()) < .0001,
                        "The left-hand injection position mirrors the right hand");
                var mainModel = mc.getItemRenderer().getModel(main, mc.level, mc.player, 0);
                var offModel = mc.getItemRenderer().getModel(off, mc.level, mc.player, 0);
                mainModel.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).apply(false, mainPose);
                offModel.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).apply(true, offPose);
                check(mainPose.last().pose().determinant() > 0 && offPose.last().pose().determinant() > 0,
                        "Both syringe models retain outward-facing geometry for transparent rendering");
                for (float[] point : new float[][] {{.2F, .3F, 0}, {-.4F, .1F, 0}}) {
                    var rightPoint = mainPose.last().pose().transformPosition(new org.joml.Vector3f(point));
                    var leftPoint = offPose.last().pose().transformPosition(new org.joml.Vector3f(point));
                    check(Math.abs(rightPoint.x + leftPoint.x) < .0001 && Math.abs(rightPoint.y - leftPoint.y) < .0001
                            && Math.abs(rightPoint.z - leftPoint.z) < .0001, "The syringe silhouette mirrors without inverting its faces");
                }
                var property = net.minecraft.client.renderer.item.ItemProperties.getProperty(main, Content.id("injection_stage"));
                check(property.call(main, mc.level, mc.player, 0) > 0 && property.call(off, mc.level, mc.player, 0) > 0,
                        "Both syringes animate their own liquid");
            }
            if (age == 406) capture = "injection-mirror-return.png";
            if (age == 520) capture = "crystallizing-mirror-idle.png";
            if (age == 575) capture = "injection-crystallizing-left.png";
            if (age == 616) capture = "crystallizing-mirror-return.png";
            if (age == 730) capture = "crystallization-approved-overlay.png";
            if (age == 780) {
                check(mainReturn && offReturn && mainShown && offShown && mainHidden && offHidden && dualMain && dualOff,
                        "Main and offhand returns render with the opposite hand hidden, while both paired hands remain visible");
                check(mc.player.getOffhandItem().is(Content.USED_SYRINGE) && mc.player.hasEffect(Content.CRYSTALLIZATION),
                        "Offhand stabilization reaches the real client");
                Nekogenix.LOGGER.info("Nekogenix injection hands client smoke passed: right click left hand mirrored transforms dual textures hand visibility independent returns approved overlay");
                mc.stop();
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var directory = mc.gameDirectory.toPath().resolve("screenshots");
                java.nio.file.Files.createDirectories(directory);
                image.writeToFile(directory.resolve(capture));
            }
            capture = null;
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException("Injection hands smoke failed: " + message);
    }
}
