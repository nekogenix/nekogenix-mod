package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatPostureSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catPostureSmokeTest");

    @SubscribeEvent public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20 || age == 200) {
            if (age == 20) CatTransformationTests.cat(player);
            else { TransformationTests.setState(player, 4); TransformationHandler.refreshEffects(player); }
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.setDeltaMovement(0, 0, 0);
        }
        if (age == 145 || age == 325) {
            check(player.onGround() && player.isShiftKeyDown() && !player.isSprinting(), "Server accepts ordinary crouching after a sprint jump");
            check(!player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Content.id("feline_sprint")), "Server removes the sprint boost while crouching");
        }
        if (age == 350) Nekogenix.LOGGER.info("Nekogenix cat posture server smoke passed: cat and neko sprint jump transitions");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static double crouchSpeed;
        private static double jumpY;
        private static Preview preview;
        private static boolean capture;
        private static final boolean[] rendered = new boolean[7];

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            mc.getToasts().clear();
            int time = age < 200 ? age : age - 180;
            mc.options.keyUp.setDown(time >= 40 && time < 160 && age < 380);
            mc.options.keySprint.setDown(time >= 80 && time < 160 && age < 380);
            mc.options.keyShift.setDown((time >= 40 && time < 65 || time >= 104 && time < 160) && age < 380);
            mc.options.keyJump.setDown(time == 100 && age < 380);
            if (age < 380 && time == 60) crouchSpeed = player.getDeltaMovement().horizontalDistance();
            if (age < 380 && time == 99) check(player.isSprinting(), "The reproduction begins with sprinting");
            if (age < 380 && time == 100) jumpY = player.getY();
            if (age < 380 && time == 103) check(player.getY() > jumpY + .1, "The sprinting player actually jumps");
            if (age < 380 && time == 140) {
                check(player.onGround() && player.isCrouching() && !player.isSprinting(), "The player lands crouched without sprinting");
                double actual = player.getDeltaMovement().horizontalDistance();
                check(crouchSpeed > .01 && Math.abs(actual - crouchSpeed) < .005,
                        "Crouch speed after a sprint jump matches ordinary crouching: " + actual + " vs " + crouchSpeed);
            }
            if (age == 390) { preview = new Preview(); mc.setScreen(preview); }
            if (preview != null) {
                for (var actor : preview.actors) actor.tickCount = age;
                if (age == 420) {
                    for (boolean seen : rendered) check(seen, "All riding and pose scenarios render");
                    preview.actors[1].stopRiding();
                }
                if (age == 440) {
                    capture = true;
                    preview.actors[1].startRiding(new net.minecraft.world.entity.vehicle.Boat(mc.level, 0, 0, 0), true);
                }
            }
            if (age == 450) {
                Nekogenix.LOGGER.info("Nekogenix cat posture client smoke passed: crouch speed cat and neko vanilla seated bones boat minecart mod seats explicit sitting dismount and equipment");
                mc.stop();
            }
        }

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void input(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
            if (!ENABLED || !event.getInput().shiftKeyDown || !event.getEntity().hasEffect(Content.AGILITY)) return;
            check(!event.getEntity().isSprinting(), "Sneaking immediately stops sprinting before movement");
            check(!event.getEntity().getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Content.id("feline_sprint")),
                    "No synchronized sprint modifier can leave a crouch speed spike");
            check(Math.abs(event.getInput().forwardImpulse) <= event.getEntity().getAttributeValue(Attributes.SNEAKING_SPEED) + .0001,
                    "The first sneak press already has crouching input speed");
        }

        @SubscribeEvent public static void model(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) throws ReflectiveOperationException {
            if (!ENABLED || preview == null || !(event.getRenderer().getModel() instanceof net.minecraft.client.model.CatModel<?>)) return;
            int index = -1;
            for (int i = 0; i < preview.actors.length; i++) if (preview.actors[i].getUUID().equals(event.getEntity().getUUID())) index = i;
            if (index < 0) return;
            rendered[index] = true;
            var model = event.getRenderer().getModel();
            boolean sitting = index >= 1 && (index != 1 || preview.actors[1].isPassenger());
            if (index == 4) sitting = false;
            check(Math.abs(part(model, "body").xRot - (sitting ? Math.PI / 4 : Math.PI / 2)) < .0001,
                    "Sitting and standing body rotation follow the actual seat at " + index);
            if (sitting) {
                for (String name : new String[] {"body", "leftHindLeg", "rightHindLeg", "tail1", "tail2"}) {
                    var actual = part(model, name);
                    var expected = part(preview.vanilla, name);
                    check(Math.abs(actual.x - expected.x) + Math.abs(actual.y - expected.y) + Math.abs(actual.z - expected.z)
                                    + Math.abs(actual.xRot - expected.xRot) + Math.abs(actual.yRot - expected.yRot) + Math.abs(actual.zRot - expected.zRot) < .0001,
                            "Seated " + name + " exactly matches the vanilla cat at " + index);
                }
            }
        }

        private static net.minecraft.client.model.geom.ModelPart part(Object model, String name) throws ReflectiveOperationException {
            var field = net.minecraft.client.model.OcelotModel.class.getDeclaredField(name);
            field.setAccessible(true);
            return (net.minecraft.client.model.geom.ModelPart)field.get(model);
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || !capture) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var path = mc.gameDirectory.toPath().resolve("screenshots/cat-sitting.png");
            java.nio.file.Files.createDirectories(path.getParent());
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
            capture = false;
        }

        private static final class Preview extends net.minecraft.client.gui.screens.Screen {
            private final net.minecraft.client.player.RemotePlayer[] actors = new net.minecraft.client.player.RemotePlayer[7];
            private final net.minecraft.world.entity.animal.Cat cat;
            private final net.minecraft.client.model.CatModel<net.minecraft.world.entity.animal.Cat> vanilla;

            private Preview() {
                super(net.minecraft.network.chat.Component.literal("Cat sitting"));
                var mc = net.minecraft.client.Minecraft.getInstance();
                cat = net.minecraft.world.entity.EntityType.CAT.create(mc.level);
                cat.setInSittingPose(true);
                vanilla = new net.minecraft.client.model.CatModel<>(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.CAT));
                vanilla.prepareMobModel(cat, 0, 0, 0); vanilla.setupAnim(cat, 0, 0, 0, 0, 0);
                var team = new net.minecraft.world.scores.PlayerTeam(mc.level.getScoreboard(), "posture");
                team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
                for (int i = 0; i < actors.length; i++) {
                    var actor = new net.minecraft.client.player.RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Sitting" + i)) {
                        @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
                            return io.github.nekogenix.gameplay.CatForm.dimensions(pose);
                        }
                        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return team; }
                    };
                    actor.setData(Content.CAT_FORM, true);
                    actor.refreshDimensions();
                    actor.setOnGround(true);
                    if (i == 1 || i == 6) actor.startRiding(new net.minecraft.world.entity.vehicle.Boat(mc.level, 0, 0, 0), true);
                    if (i == 2) actor.startRiding(new net.minecraft.world.entity.vehicle.Minecart(mc.level, 0, 0, 0), true);
                    if (i == 3 || i == 4) {
                        final boolean sit = i == 3;
                        actor.startRiding(new net.minecraft.world.entity.vehicle.Boat(mc.level, 0, 0, 0) {
                            @Override public boolean shouldRiderSit() { return sit; }
                        }, true);
                    }
                    if (i == 5) actor.setPose(net.minecraft.world.entity.Pose.SITTING);
                    if (i == 6) {
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.Items.DIAMOND_SWORD.getDefaultInstance());
                    }
                    actors[i] = actor;
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                graphics.fill(0, 0, width, height, 0xff75828b);
                String[] labels = {"Standing", "Boat", "Minecart", "Mod seat", "Standing vehicle", "Sitting pose", "Equipment", "Vanilla cat"};
                int w = width / 4, h = height / 2;
                for (int i = 0; i < 8; i++) {
                    int left = i % 4 * w, top = i / 4 * h;
                    graphics.drawString(font, labels[i], left + 3, top + 4, 0xffffff, false);
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                            left + 2, top + 18, left + w - 2, top + h, (int)(h * .75), 0, .8F, .12F, i < 7 ? actors[i] : cat);
                }
            }
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Cat posture smoke failed: " + message);
    }
}
