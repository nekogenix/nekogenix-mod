package io.github.nekogenix;

import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.CameraPosture;
import io.github.nekogenix.gameplay.CatForm;
import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraPostureSmokeTest {
    private static final boolean PASSAGE_ONLY = Boolean.getBoolean("nekogenix.legacyPassageSmokeTest");
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.cameraPostureSmokeTest") || PASSAGE_ONLY;
    private static int catPassage, humanPassage;
    private static final java.util.UUID REMOTE = java.util.UUID.fromString("8637a52f-7841-4f65-8719-351d8695c610");
    private static ServerPlayer remote;

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player.getUUID().equals(REMOTE)) return;
        int age = player.tickCount;
        if (age == 20) {
            var level = player.serverLevel();
            for (var pos : BlockPos.betweenClosed(-2, -61, -2, 2, -57, 80)) level.setBlockAndUpdate(pos,
                    (pos.getY() == -61 || pos.getY() == -59 && pos.getZ() >= 4 && pos.getZ() <= 8 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            level.setDayTime(6000); CatTransformationTests.cat(player);
            player.setInvulnerable(true);
            player.teleportTo(level, .5, -60, .5, 0, 0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(20);
        }
        if (age > 60 && age < 210 && player.getZ() > 4 && player.getZ() < 9) {
            check(player.getBbHeight() <= 1.001, "The server accepts a lowered cat body in the passage"); catPassage++;
        }
        if (age == 205) {
            check(catPassage > 5 && player.getZ() > 9, "The cat physically walks through the low tunnel");
            check(Math.abs(player.getEyeHeight() - 1.62) < .03, "The server restores the raised body after leaving the tunnel");
        }
        if (age == 210) {
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), new net.minecraft.nbt.CompoundTag());
            TransformationHandler.refreshEffects(player); CatForm.sync(player); CameraPosture.refresh(player);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0); player.setDeltaMovement(0, 0, 0);
        }
        if (age > 240 && age < 490 && player.getZ() > 4 && player.getZ() < 9) {
            check(player.hasPose(Pose.SWIMMING) && player.getBbHeight() < .7, "The human has a real crawl hitbox inside the passage"); humanPassage++;
        }
        if (age == 495) {
            check(humanPassage > 5 && player.getZ() > 9, "The human physically crawls through the low tunnel");
            Nekogenix.LOGGER.info("Nekogenix camera posture server smoke passed: synchronized head hitbox and real cat and human tunnel movement");
        }
        if (age == 515) {
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(REMOTE, "RemoteCat"), false);
            remote = new ServerPlayer(player.server, player.serverLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            player.server.getPlayerList().placeNewPlayer(connection, remote, cookie);
            CatTransformationTests.cat(remote); CatForm.sync(remote);
            remote.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ() + 3, 180, 0);
            remote.setNoGravity(true);
        }
        if (remote != null) {
            io.github.nekogenix.gameplay.CameraProjectiles.receive(remote, age < 525 ? .43F : age < 570 ? 1.62F : .8F, 100);
            remote.doTick();
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double openSpeed, tunnelSpeed, exitSpeed;
        private static int openSamples, tunnelSamples, exitSamples;
        private static String capture;
        private static Preview preview;
        private static int rendered;
        private static int uiTicks = -1;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null) return;
            int age = player.tickCount; mc.getToasts().clear();
            player.setYRot(0); player.yRotO = 0; player.setXRot(0); player.xRotO = 0;
            mc.options.keyUp.setDown(age >= 70 && age < 200 || age >= 240 && age < 490);
            mc.options.keySprint.setDown(false); mc.options.keyShift.setDown(false);
            if (age == 30 || age == 510) ClientConfig.CAMERA_HEIGHT.set(100);
            if (age == 60) check(Math.abs(player.getEyeHeight() - 1.62) < .03 && player.getBbHeight() > 1.76,
                    "Raised cat camera exposes a full height hitbox on the client");
            if (age == 220) ClientConfig.CAMERA_HEIGHT.set(0);
            if (age >= 240 && age < 490 && player.onGround()) {
                double speed = player.getDeltaMovement().horizontalDistance();
                if (player.getZ() > 1 && player.getZ() < 3) { openSpeed += speed; openSamples++; }
                if (player.getZ() > 5 && player.getZ() < 8) { tunnelSpeed += speed; tunnelSamples++; }
                if (player.getZ() > 11 && player.getZ() < 15) { exitSpeed += speed; exitSamples++; }
            }
            if (age == 500) {
                check(openSamples > 3 && tunnelSamples > 3 && exitSamples > 3,
                        "Movement samples cover open ground, the tunnel and the exit: " + openSamples + "/" + tunnelSamples + "/" + exitSamples + " z=" + player.getZ());
                double open = openSpeed / openSamples, tunnel = tunnelSpeed / tunnelSamples, exit = exitSpeed / exitSamples;
                check(open > tunnel * 2 && Math.abs(open - exit) < .015,
                        "Only the low passage applies crawl slowdown: " + open + "/" + tunnel + "/" + exit);
                check(player.isVisuallyCrawling() && CameraPosture.freeProne(player), "Open space restores walking speed while keeping the selected prone pose");
                if (PASSAGE_ONLY) {
                    Nekogenix.LOGGER.info("Nekogenix legacy passage client smoke passed: floor level cat movement low human camera native crawl speed and open space recovery");
                    mc.stop();
                }
            }
            if (age == 535) check(player.hasPose(Pose.STANDING) && player.getForcedPose() == null, "Raising the preference restores normal human standing");
            if (age == 537 || age == 578) {
                var other = mc.level.getPlayerByUUID(REMOTE);
                float expected = age == 537 ? 1.62F : .8F;
                check(other != null && CatForm.active(other) && Math.abs(other.getEyeHeight() - expected) < .015
                        && other.getBbHeight() >= expected + .17F, "A tracked remote player's real eyes and hitbox follow synchronized posture data");
                if (age == 537) capture = "camera-remote-upright-cat.png";
            }
            if (age == 540) { preview = new Preview(); mc.setScreen(preview); }
            if (preview != null) for (var actor : preview.actors) actor.tickCount = age;
            if (age == 560) { check(rendered == 255, "Every cat posture and equipment scenario was rendered"); capture = "camera-cat-postures.png"; }
            if (age >= 580 && uiTicks < 0) { mc.setScreen(new io.github.nekogenix.client.skin.CatSkinScreen(null)); uiTicks = 0; }
            if (uiTicks >= 0) uiTicks++;
            if (uiTicks == 15) { pose(mc.screen, "four_paws").onPress(); capture = "skin-preview-upright.png"; }
            if (uiTicks == 25) { pose(mc.screen, "upright").onPress(); capture = "skin-preview-sitting.png"; }
            if (uiTicks == 35) {
                pose(mc.screen, "sitting"); mc.options.guiScale().set(4); mc.resizeDisplay();
                pose(mc.screen, "sitting"); capture = "skin-preview-small.png";
            }
            if (uiTicks == 45) mc.setScreen(new io.github.nekogenix.client.skin.NewCatSkinScreen((io.github.nekogenix.client.skin.CatSkinScreen)mc.screen));
            if (uiTicks == 55) { pose(mc.screen, "four_paws").onPress(); capture = "skin-import-upright.png"; }
            if (uiTicks == 65) {
                pose(mc.screen, "upright").onPress(); capture = "skin-import-sitting.png";
            }
            if (uiTicks == 75) {
                check(ClientConfig.cameraHeight() == 100, "Preview poses do not change gameplay posture or camera preferences");
                Nekogenix.LOGGER.info("Nekogenix camera posture client smoke passed: hitbox tunnel movement prone speed restoration standing equipment and both skin preview pose selectors");
                mc.stop();
            }
        }

        private static net.minecraft.client.gui.components.Button pose(net.minecraft.client.gui.screens.Screen screen, String name) {
            var label = net.minecraft.network.chat.Component.translatable("skins.nekogenix.pose." + name).getString();
            return screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(label))
                    .map(e -> (net.minecraft.client.gui.components.Button)e).findFirst().orElseThrow();
        }

        @SubscribeEvent public static void model(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) throws ReflectiveOperationException {
            if (!ENABLED || preview == null || !(event.getRenderer().getModel() instanceof net.minecraft.client.model.CatModel<?>)) return;
            for (int i = 0; i < preview.actors.length; i++) if (preview.actors[i].getUUID().equals(event.getEntity().getUUID())) {
                rendered |= 1 << i;
                var field = net.minecraft.client.model.OcelotModel.class.getDeclaredField("head"); field.setAccessible(true);
                var head = (net.minecraft.client.model.geom.ModelPart)field.get(event.getRenderer().getModel());
                float eye = CameraPosture.modelEye(preview.actors[i]);
                check(Math.abs((15 - head.y) * .05F + .43F - eye) < .015, "The rendered cat head follows its real eye height");
                if (eye > .44F) {
                    var bodyField = net.minecraft.client.model.OcelotModel.class.getDeclaredField("body"); bodyField.setAccessible(true);
                    var body = (net.minecraft.client.model.geom.ModelPart)bodyField.get(event.getRenderer().getModel());
                    double neckY = body.y + 3 * body.yScale * Math.cos(body.xRot) + 5 * Math.sin(body.xRot);
                    double neckZ = body.z + 3 * body.yScale * Math.sin(body.xRot) - 5 * Math.cos(body.xRot);
                    check(Math.abs(neckY - head.y - 1.75) + Math.abs(neckZ - head.z - 2) < .01,
                            "The head remains connected to the torso at every raised camera height");
                }
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/" + capture));
            }
            capture = null;
        }

        private static final class Preview extends net.minecraft.client.gui.screens.Screen {
            private final net.minecraft.client.player.RemotePlayer[] actors = new net.minecraft.client.player.RemotePlayer[8];
            private final float[] heights = {.43F, .8F, 1.2F, 1.62F};
            private Preview() {
                super(net.minecraft.network.chat.Component.literal("Camera postures"));
                var mc = net.minecraft.client.Minecraft.getInstance();
                var team = new net.minecraft.world.scores.PlayerTeam(mc.level.getScoreboard(), "camera-posture");
                team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
                for (int i = 0; i < actors.length; i++) {
                    var actor = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Posture" + i)) {
                        @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(Pose pose) {
                            return CameraPosture.catDimensions(this, pose);
                        }
                        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return team; }
                    };
                    actor.setData(Content.CAT_FORM, true); actor.setOnGround(true);
                    CameraPosture.accept(actor, heights[i % 4], 100);
                    if (i >= 4) {
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.item.Items.DIAMOND_LEGGINGS.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.Items.DIAMOND_BOOTS.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.Items.DIAMOND_SWORD.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.item.Items.SHIELD.getDefaultInstance());
                    }
                    actors[i] = actor;
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                graphics.fill(0, 0, width, height, 0xff75828b);
                int w = width / 4, h = height / 2;
                for (int i = 0; i < actors.length; i++) {
                    int left = i % 4 * w, top = i / 4 * h;
                    graphics.drawString(font, "Eye " + heights[i % 4] + (i >= 4 ? " + equipment" : ""), left + 3, top + 4, 0xffffff, false);
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                            left + 2, top + 18, left + w - 2, top + h, (int)(h * .55), 0, .8F, .12F, actors[i]);
                }
            }
        }
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Camera posture smoke failed: " + message);
    }
}
