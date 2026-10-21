package io.github.nekogenix;

import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.CameraGeometry;
import io.github.nekogenix.gameplay.CameraPosture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraPoseRefinementsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.cameraPoseRefinementsSmokeTest");
    private static final int[] SETTINGS = {0, 10, 20, 30, 40, 47, 48, 50, 75, 100};
    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            for (var pos : BlockPos.betweenClosed(8, -61, -4, 16, -57, 45)) player.serverLevel().setBlockAndUpdate(pos,
                    (pos.getY() == -61 ? Blocks.STONE : pos.getY() <= -58 ? Blocks.WATER : Blocks.AIR).defaultBlockState());
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.serverLevel().setDayTime(6000); player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(20);
        }
        if (age == 250) {
            CatTransformationTests.cat(player);
            player.teleportTo(player.serverLevel(), 12.5, -59.8, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age == 300) {
            check(player.isInWaterOrBubble() && player.isSwimming() && CameraPosture.upright(player), "The server keeps the upright cat build during real sprint swimming");
            Nekogenix.LOGGER.info("Nekogenix camera pose refinements server smoke passed: real upright cat swimming");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double previous;
        private static String capture;
        private static Grid grid;
        private static int rendered;
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null) return;
            int age = player.tickCount; mc.getToasts().clear();
            mc.options.keyUp.setDown(age >= 265 && age < 320); mc.options.keySprint.setDown(age >= 265 && age < 320);
            mc.options.keyShift.setDown(false);
            for (int i = 0; i < SETTINGS.length; i++) {
                if (age == 30 + i * 20) ClientConfig.CAMERA_HEIGHT.set(SETTINGS[i]);
                if (age == 48 + i * 20) {
                    double actual = mc.gameRenderer.getMainCamera().getPosition().y - player.getY();
                    double expected = CameraGeometry.selected(SETTINGS[i]);
                    check(Math.abs(actual - expected) < .025 && (i == 0 || actual > previous + .007),
                            "The rendered human camera follows " + SETTINGS[i] + " percent: " + actual + " vs " + expected);
                    previous = actual;
                }
            }
            if (age == 280) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            if (age == 300) {
                check(player.isSwimming() && CameraPosture.upright(player), "The real swimming player preserves the upright build on the client");
                capture = "upright-cat-swimming-world.png";
            }
            if (age == 325) { grid = new Grid(); mc.setScreen(grid); }
            if (grid != null) grid.animate(age);
            if (age == 350) capture = "cat-connected-joints-front.png";
            if (age == 370) { grid.angle = (float)Math.PI / 2; capture = "cat-connected-joints-side.png"; }
            if (age == 390) { grid.angle = (float)Math.PI; capture = "cat-connected-joints-back.png"; }
            if (age == 410) {
                check(rendered == 4095, "All slider boundaries swimming equipment and item poses render");
                Nekogenix.LOGGER.info("Nekogenix camera pose refinements client smoke passed: full human slider two fixed cat builds connected animated joints armor held items and swimming");
                mc.stop();
            }
        }

        @SubscribeEvent public static void model(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) throws ReflectiveOperationException {
            if (!ENABLED || grid == null || !(event.getRenderer().getModel() instanceof net.minecraft.client.model.CatModel<?>)) return;
            for (int i = 0; i < grid.actors.length; i++) if (grid.actors[i].getUUID().equals(event.getEntity().getUUID())) {
                rendered |= 1 << i;
                var actor = grid.actors[i]; var model = event.getRenderer().getModel();
                var head = part(model, "head"); var body = part(model, "body");
                boolean upright = CameraPosture.upright(actor);
                check(Math.abs(body.xRot - (upright ? 0 : Math.PI / 2)) < .0001, "Only two body poses exist at camera " + grid.heights[i % 6]);
                check(Math.abs(head.y - (upright ? -8.8F : 15)) < .0001, "No camera setting stretches the cat into an intermediate model");
                if (upright) {
                    for (String name : new String[] {"leftFrontLeg", "rightFrontLeg", "leftHindLeg", "rightHindLeg"}) {
                        var limb = part(model, name); boolean arm = name.contains("Front"), left = name.startsWith("left");
                        var top = new org.joml.Vector3f(0, 0, (arm ? 1 : 2) * limb.zScale)
                                .rotate(new org.joml.Quaternionf().rotationZYX(limb.zRot, limb.yRot, limb.xRot)).add(limb.x, limb.y, limb.z);
                        float x = (left ? 1 : -1) * (arm ? 2.55F : 1.1F);
                        float y = arm ? head.y + 6 : body.y + 19 * body.yScale - .2F;
                        float z = arm ? head.z + 2.5F : .2F;
                        check(top.distance(x, y, z) < .0001, "The " + name + " joint stays anchored during movement and item use");
                        check(Math.abs(top.x) - 1 < 2 && top.y >= body.y + 3 * body.yScale && top.y < body.y + 19 * body.yScale,
                                "The limb overlaps the torso at its joint instead of leaving a gap");
                    }
                }
            }
        }
        private static net.minecraft.client.model.geom.ModelPart part(Object model, String name) throws ReflectiveOperationException {
            var field = net.minecraft.client.model.OcelotModel.class.getDeclaredField(name); field.setAccessible(true);
            return (net.minecraft.client.model.geom.ModelPart)field.get(model);
        }
        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/" + capture));
            }
            capture = null;
        }
        private static final class Grid extends net.minecraft.client.gui.screens.Screen {
            final net.minecraft.client.player.RemotePlayer[] actors = new net.minecraft.client.player.RemotePlayer[12];
            final float[] heights = {.43F, .906F, .9179F, 1.2F, 1.62F, .43F};
            float angle;
            Grid() {
                super(net.minecraft.network.chat.Component.literal("Cat joint regression"));
                var mc = net.minecraft.client.Minecraft.getInstance();
                var team = new net.minecraft.world.scores.PlayerTeam(mc.level.getScoreboard(), "joint-test");
                team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
                for (int i = 0; i < actors.length; i++) {
                    var actor = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Joints" + i)) {
                        @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(Pose pose) { return CameraPosture.catDimensions(this, pose); }
                        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return team; }
                        @Override public boolean isInWaterOrBubble() { return isSwimming(); }
                    };
                    actor.setData(Content.CAT_FORM, true); actor.setOnGround(true);
                    actor.setPos(.5, -60, .5);
                    if (i % 6 == 5) { actor.setSwimming(true); actor.setPose(Pose.SWIMMING); actor.setOnGround(false); }
                    CameraPosture.accept(actor, heights[i % 6], i % 6 == 0 ? 0 : i % 6 == 1 ? 40 : i % 6 == 2 ? 41 : 100);
                    if (i >= 6) for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                            net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                        actor.setItemSlot(slot, switch (slot) {
                            case HEAD -> net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance();
                            case CHEST -> net.minecraft.world.item.Items.DIAMOND_CHESTPLATE.getDefaultInstance();
                            case LEGS -> net.minecraft.world.item.Items.DIAMOND_LEGGINGS.getDefaultInstance();
                            default -> net.minecraft.world.item.Items.DIAMOND_BOOTS.getDefaultInstance();
                        });
                    }
                    actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.DIAMOND_PICKAXE.getDefaultInstance());
                    actor.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.Items.TORCH.getDefaultInstance());
                    actors[i] = actor;
                }
            }
            void animate(int age) {
                for (var actor : actors) {
                    actor.tickCount = age;
                    actor.setYRot(180); actor.yRotO = actor.yBodyRot = actor.yBodyRotO = actor.yHeadRot = actor.yHeadRotO = 180;
                    actor.swingingArm = age % 24 < 12 ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND;
                    actor.oAttackAnim = actor.attackAnim; actor.attackAnim = (age % 12) / 12F;
                    actor.walkAnimation.update(.5F, 1);
                    actor.setDeltaMovement(0, 0, .14);
                    if (age == 360) {
                        actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.Items.BOW.getDefaultInstance());
                        actor.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                    if (age == 380) {
                        actor.stopUsingItem();
                        actor.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.Items.SHIELD.getDefaultInstance());
                        actor.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                    }
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                graphics.fill(0, 0, width, height, 0xff77868d);
                int w = width / 6, h = height / 2;
                for (int i = 0; i < actors.length; i++) {
                    int left = i % 6 * w, top = i / 6 * h;
                    graphics.drawString(font, i % 6 == 5 ? "Swimming" : "Eye " + heights[i % 6], left + 2, top + 3, 0xffffff, false);
                    graphics.enableScissor(left, top + 14, left + w, top + h);
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(graphics,
                            left + w / 2F, top + (h + 14) / 2F, h * .45F,
                            new org.joml.Vector3f(0, actors[i].getBbHeight() / 2, 0),
                            new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateY(angle), new org.joml.Quaternionf(), actors[i]);
                    graphics.disableScissor();
                }
            }
        }
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Camera pose refinements smoke failed: " + message);
    }
}
