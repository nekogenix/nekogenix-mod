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
            player.teleportTo(.5, 80, .5);
        }
        if (age == 120 || age == 360) {
            var saved = new CompoundTag();
            saved.putInt("phase", (age == 120 ? NekoState.Phase.CAT : NekoState.Phase.HUMAN).ordinal());
            saved.putInt("abilities", age == 120 ? 7 : 0); saved.putInt("lives", 9);
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), saved);
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean capture;
        private static boolean approachChecked;

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
                double x = age < 190 ? .5 : age < 215 ? 3.5 : age < 240 ? 4.5 : age < 280 ? 9.5 : age < 315 ? 12.5 : .5;
                player.setPos(x, 80, .5);
                player.xo = x; player.yo = 80; player.zo = .5;
                player.setDeltaMovement(age >= 170 && age <= 185 ? new net.minecraft.world.phys.Vec3(.5, 0, 0)
                        : age >= 190 && age < 215 ? new net.minecraft.world.phys.Vec3(.15, 0, 0) : net.minecraft.world.phys.Vec3.ZERO);
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
                capture = true;
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
                check(player.getData(Content.CAT_FORM) && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 0, "Cat transformation automatically selects Cat");
                check(Math.abs(height() - .43) < .03, "Automatic cat height");
                io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(100);
            }
            if (age == 185) {
                check(Math.abs(height() - 1.62) < .02, "Cat can manually select Human");
                check(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit && hit.getBlockPos().getY() == 81, "High cat camera targets the visible upper block");
            }
            if (age == 210) {
                check(height() < 1.1 && player.getX() < 4, "Camera lowers before entering one block tunnel");
                approachChecked = true;
            }
            if (age == 235) check(height() <= .875 && height() > .7, "Camera stays below tunnel roof");
            if (age == 275) check(height() > 1.59, "Camera restores after leaving tunnel");
            if (age == 310) {
                mc.gameRenderer.getMainCamera().setup(mc.level, player, false, false, 1);
                check(height() <= .375 && height() > .2, "Half block clearance is protected");
            }
            if (age == 350) {
                check(height() > 1.59 && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 100, "Temporary clearance keeps the saved preference");
                io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.set(25);
            }
            if (age == 385) {
                check(!player.getData(Content.CAT_FORM) && io.github.nekogenix.config.ClientConfig.CAMERA_HEIGHT.get() == 100, "Returning to human restores Human endpoint");
                check(approachChecked, "Approach scenario executed");
                io.github.nekogenix.config.ClientConfig.SPEC.save();
                Nekogenix.LOGGER.info("Nekogenix camera height smoke passed: native slider manual endpoints form changes predictive one block and half block clearance smooth recovery and saved preference");
                mc.stop();
            }
        }

        private static double height() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            double height = mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
            Nekogenix.LOGGER.info("Camera height runtime tick {}: {}", mc.player.tickCount, height);
            return height;
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || !capture) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/camera-height-options.png"));
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            capture = false;
        }

        private static void check(boolean valid, String message) {
            if (!valid) throw new IllegalStateException("Camera height smoke failed: " + message);
        }
    }
}
