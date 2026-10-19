package io.github.nekogenix;

import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraHeightSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.cameraHeightSmokeTest");
    private static int arrows, snowballs, tridents;

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            for (int x = -4; x <= 18; x++) for (int z = -3; z <= 3; z++) {
                player.serverLevel().setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                for (int y = 80; y <= 86; y++) player.serverLevel().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                if (x >= 4 && x <= 7) player.serverLevel().setBlockAndUpdate(new BlockPos(x, 81, z), Blocks.STONE.defaultBlockState());
                if (x >= 12 && x <= 15) player.serverLevel().setBlockAndUpdate(new BlockPos(x, 80, z), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            }
            player.serverLevel().setBlockAndUpdate(new BlockPos(0, 80, 3), Blocks.STONE.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(new BlockPos(0, 81, 3), Blocks.STONE.defaultBlockState());
            for (int y = 80; y <= 82; y++) for (int z = -1; z <= 1; z++) {
                player.serverLevel().setBlockAndUpdate(new BlockPos(2, y, z), Blocks.STONE.defaultBlockState());
            }
            player.setNoGravity(true);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.BOW.getDefaultInstance());
            player.getInventory().setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 64));
            player.teleportTo(.5, 80, .5);
        }
        if (age == 400) {
            var crossbow = net.minecraft.world.item.Items.CROSSBOW.getDefaultInstance();
            crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                    net.minecraft.world.item.component.ChargedProjectiles.of(net.minecraft.world.item.Items.ARROW.getDefaultInstance()));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crossbow);
        }
        if (age == 450) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SNOWBALL, 16));
        if (age == 490) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.TRIDENT.getDefaultInstance());
        if (age == 600) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.BOW.getDefaultInstance());
        if (age == 120 || age == 760) {
            var saved = new CompoundTag();
            saved.putInt("phase", (age == 120 ? NekoState.Phase.CAT : NekoState.Phase.HUMAN).ordinal());
            saved.putInt("abilities", age == 120 ? 7 : 0); saved.putInt("lives", 9);
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), saved);
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            if (age == 120) player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1200, 1));
        }
        if (age == 740) {
            if (arrows != 3 || snowballs != 1 || tridents != 1) throw new IllegalStateException("Missing actual weapon shots: " + arrows + "/" + snowballs + "/" + tridents);
            Nekogenix.LOGGER.info("Nekogenix camera weapon smoke passed: bow crossbow snowball trident and bow during dynamic tunnel clearance");
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void projectile(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!ENABLED || event.getLevel().isClientSide || !(event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile)
                || !(projectile.getOwner() instanceof ServerPlayer player)) return;
        double expected;
        if (projectile instanceof net.minecraft.world.entity.projectile.Arrow) expected = ++arrows == 3 ? Double.NaN : 1.52;
        else if (projectile instanceof net.minecraft.world.entity.projectile.Snowball) { snowballs++; expected = 1.52; }
        else if (projectile instanceof net.minecraft.world.entity.projectile.ThrownTrident) { tridents++; expected = 1.52; }
        else return;
        double actual = projectile.getY() - player.getY();
        Nekogenix.LOGGER.info("Camera weapon runtime {}: origin={} expected={}", projectile.getType(), actual, expected);
        if (Double.isNaN(expected) ? actual <= .74 || actual >= 1.46 : Math.abs(actual - expected) > .04) {
            throw new IllegalStateException("Weapon origin disagrees with the visible camera: " + actual + " expected " + expected);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static boolean approachChecked;
        private static boolean smoothCrouch;
        private static int oneBlockFrames, halfBlockFrames;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            player.setNoGravity(true);
            player.setYRot(0); player.setXRot(0); player.yRotO = 0; player.xRotO = 0;
            mc.getToasts().clear();
            if (age >= 35) {
                double x = age < 190 ? .5 : age < 215 ? 3.5 : age < 240 ? 4.5 : age < 280 ? 9.5 : age < 315 ? 12.5
                        : age >= 640 && age < 645 ? 3.5 : age >= 645 && age < 655 ? 4.5 : .5;
                player.setPos(x, 80, .5);
                player.xo = x; player.yo = 80; player.zo = .5;
                player.setDeltaMovement(age >= 170 && age <= 185 ? new net.minecraft.world.phys.Vec3(.5, 0, 0)
                        : age >= 190 && age < 215 || age >= 640 && age < 645 ? new net.minecraft.world.phys.Vec3(.15, 0, 0) : net.minecraft.world.phys.Vec3.ZERO);
            }
            if (age == 40) {
                mc.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(null, mc.options));
                var sliders = mc.screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.AbstractSliderButton).map(e -> (net.minecraft.client.gui.components.AbstractSliderButton)e).toList();
                check(sliders.size() == 2, "Exactly one additional slider");
                var fov = sliders.getFirst(); var camera = sliders.getLast();
                check(camera.getX() == fov.getX() && camera.getY() == fov.getY() + fov.getHeight() + 4
                        && camera.getWidth() == fov.getWidth() && camera.getHeight() == fov.getHeight(), "Camera slider is immediately below FOV with native dimensions");
                check(camera.getMessage().getString().endsWith("Human"), "Human endpoint label");
                camera.mouseClicked(camera.getX() + 4, camera.getY() + 10, 0);
                camera.mouseReleased(camera.getX() + 4, camera.getY() + 10, 0);
                check(io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 0 && camera.getMessage().getString().endsWith("Cat"), "A human can select Cat");
                capture = "camera-height-options.png";
            }
            if (age == 60) {
                mc.setScreen(null);
                check(height() > .40 && height() < .46, "Human selected cat height");
                check(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().getY() == 80, "Low camera targets the visible lower block");
                io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            }
            if (age == 45) { mc.options.guiScale().set(4); mc.resizeDisplay(); }
            if (age == 50) {
                var sliders = mc.screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.AbstractSliderButton).map(e -> (net.minecraft.client.gui.components.AbstractSliderButton)e).toList();
                check(sliders.getLast().getX() == sliders.getFirst().getX()
                        && sliders.getLast().getY() == sliders.getFirst().getY() + sliders.getFirst().getHeight() + 4, "Slider follows native layout after resize");
                mc.options.guiScale().set(2); mc.resizeDisplay();
            }
            if (age == 100) check(Math.abs(height() - 1.62) < .02, "Human endpoint is vanilla height");
            if (age == 145) {
                check(player.getData(Content.CAT_FORM) && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 100, "Cat transformation preserves the Human setting");
                check(Math.abs(height() - 1.62) < .03, "Human remains the default cat camera");
                io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(50);
                mc.options.fov().set(110);
                mc.options.fovEffectScale().set(1.0);
                mc.options.bobView().set(true);
            }
            if (age == 170) {
                check(Math.abs(height() - 1.025) < .03, "Custom standing height settles");
                mc.options.keyShift.setDown(true);
            }
            if (age == 185) {
                check(Math.abs(height() - .81) < .03 && smoothCrouch, "Custom crouching transitions through intermediate heights");
                mc.options.keyShift.setDown(false);
                io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            }
            if (age == 210) {
                check(height() < 1.1 && player.getX() < 4, "Camera lowers before entering one block tunnel");
                approachChecked = true;
            }
            if (age == 235) {
                check(height() <= .765 && height() > .7, "Camera stays farther below tunnel roof");
                check(player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED), "Speed potion is active during high FOV clearance checks");
                capture = "camera-ceiling-one-block.png";
            }
            if (age == 275) check(height() > 1.59, "Camera restores after leaving tunnel");
            if (age == 280) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(70);
            if (age == 310) {
                mc.gameRenderer.getMainCamera().setup(mc.level, player, false, false, 1);
                check(height() <= .265 && height() > .2, "Half block clearance includes room for view bobbing");
                capture = "camera-ceiling-half-block.png";
            }
            if (age == 315) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            if (age == 350) {
                check(height() > 1.59 && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 100, "Temporary clearance keeps the saved preference");
            }
            if (age == 335 || age == 430 || age == 470 || age == 510 || age == 620) mc.options.keyUse.setDown(true);
            if (age == 365 || age == 431 || age == 471 || age == 540 || age == 641) mc.options.keyUse.setDown(false);
            if (age == 680) io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(25);
            if (age == 785) {
                check(!player.getData(Content.CAT_FORM) && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 25, "A form change preserves a manual preference");
                check(approachChecked, "Approach scenario executed");
                check(oneBlockFrames > 5 && halfBlockFrames > 5, "Rendered near plane stays below both ceilings during high FOV sprint bobbing");
                io.github.nekogenix.config.ClientConfig.SPEC.save();
                Nekogenix.LOGGER.info("Nekogenix camera height smoke passed: default Human native slider smooth crouching reachable clearance actual weapon input and manual preference across form changes");
                mc.stop();
            }
        }

        @SubscribeEvent public static void bob(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
            if (!ENABLED) return;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.tickCount >= 190 && player.tickCount < 315) {
                player.bob = player.oBob = .1F;
                player.setSprinting(true);
                player.setXRot((float)Math.sin(player.tickCount * .3) * 80);
                player.xRotO = player.getXRot();
            }
        }

        @SubscribeEvent public static void ceiling(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
            if (!ENABLED || event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;
            int age = player.tickCount;
            double cameraX = event.getCamera().getPosition().x;
            double roof = age >= 225 && age < 240 && cameraX >= 4.25 && cameraX <= 7.75 ? 81
                    : age >= 300 && age < 315 && cameraX >= 12.25 && cameraX <= 15.75 ? 80.5 : Double.NaN;
            if (Double.isNaN(roof)) return;
            var inverse = new org.joml.Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
            for (int x : new int[] {-1, 1}) for (int y : new int[] {-1, 1}) {
                var corner = inverse.transform(new org.joml.Vector4f(x, y, -1, 1));
                double actual = event.getCamera().getPosition().y + corner.y / corner.w;
                check(actual < roof - .01, "Rendered near plane cannot see through the ceiling: " + actual + " roof " + roof
                        + " tick " + age + " camera " + event.getCamera().getPosition());
            }
            if (roof == 81) oneBlockFrames++;
            else halfBlockFrames++;
        }

        private static double height() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            double height = mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
            Nekogenix.LOGGER.info("Camera height runtime tick {}: {}", mc.player.tickCount, height);
            return height;
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player.isCrouching() && mc.player.tickCount >= 170 && mc.player.tickCount < 185) {
                double height = mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
                if (height > .84 && height < 1.0) smoothCrouch = true;
            }
            if (capture == null) return;
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                java.nio.file.Files.createDirectories(mc.gameDirectory.toPath().resolve("screenshots"));
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/" + capture));
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            capture = null;
        }

        private static void check(boolean valid, String message) {
            if (!valid) throw new IllegalStateException("Camera height smoke failed: " + message);
        }
    }
}
