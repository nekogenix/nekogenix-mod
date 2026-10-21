package io.github.nekogenix;

import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.gameplay.CameraPosture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatOrientationSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catOrientationSmokeTest");
    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            CatTransformationTests.cat(player); player.setInvulnerable(true);
            player.serverLevel().setDayTime(6000); player.getFoodData().setFoodLevel(20);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
        }
        if (player.tickCount == 60) {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.ELYTRA.getDefaultInstance());
            player.teleportTo(player.serverLevel(), .5, -30, .5, 0, 0);
            player.setDeltaMovement(0, -.03, .4); player.setOnGround(false);
        }
    }
    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static Grid grid;
        private static String capture;
        private static int checks, actualFlightChecks;
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) throws java.io.IOException {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance(); var player = mc.player;
            if (player == null) return;
            int age = player.tickCount; mc.getToasts().clear();
            if (age == 30) {
                ClientConfig.NEKO_DOUBLE_JUMP.set(true); ClientConfig.AUTOMATIC_FELINE_VISION.set(true);
                mc.setScreen(new net.minecraft.client.gui.screens.options.controls.ControlsScreen(null, mc.options));
                option("options.nekogenix.double_jump").onPress(); option("options.nekogenix.feline_vision").onPress();
            }
            if (age == 35) {
                mc.setScreen(new net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen(null, mc.options));
                check(Boolean.FALSE.equals(option("options.nekogenix.double_jump").getValue())
                        && Boolean.FALSE.equals(option("options.nekogenix.feline_vision").getValue()), "Accessibility shares the exact Controls preferences");
                option("options.nekogenix.double_jump").onPress(); option("options.nekogenix.feline_vision").onPress();
                list().setScrollAmount(0); capture = "nekogenix-accessibility.png";
            }
            if (age == 40) {
                mc.options.guiScale().set(4); mc.resizeDisplay();
                list().setScrollAmount(0);
            }
            if (age == 42) {
                for (net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> entry : list().children()) for (var child : entry.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                    if (widget.getMessage().getString().startsWith(text("options.nekogenix.double_jump")) || widget.getMessage().getString().startsWith(text("options.nekogenix.feline_vision")))
                        check(widget.getX() >= 0 && widget.getRight() <= mc.screen.width, "Mod accessibility options fit the largest GUI scale: " + widget.getMessage().getString() + " " + widget.getX() + ".." + widget.getRight() + " width " + mc.screen.width);
                check(widgets().stream().filter(w -> w.getMessage().getString().startsWith(text("options.nekogenix.double_jump"))).count() == 1,
                        "Reinitializing the list never duplicates the mod options");
                list().setScrollAmount(0); capture = "nekogenix-accessibility-large.png";
            }
            if (age == 45) {
                mc.setScreen(new io.github.nekogenix.client.ModOptionsScreen(null));
                check(Boolean.TRUE.equals(option("options.nekogenix.double_jump").getValue())
                        && Boolean.TRUE.equals(option("options.nekogenix.feline_vision").getValue()), "Config reflects Accessibility changes");
                for (String lang : new String[] {"en_us", "ru_ru", "uk_ua"}) {
                    var resource = mc.getResourceManager().getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Nekogenix.ID, "lang/" + lang + ".json")).orElseThrow();
                    try (var reader = new java.io.InputStreamReader(resource.open(), java.nio.charset.StandardCharsets.UTF_8)) {
                        String actual = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().get("options.nekogenix.camera_height.cat").getAsString();
                        check(actual.equals(switch(lang) { case "ru_ru" -> "Четвероногие"; case "uk_ua" -> "Чотириногі"; default -> "Quadrupeds"; }), "All endpoint translations mean quadrupeds");
                    }
                }
            }
            if (age == 50) {
                mc.options.guiScale().set(3); mc.resizeDisplay(); mc.setScreen(null);
                ClientConfig.CAMERA_HEIGHT.set(100); mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            }
            if (age == 65) {
                check(player.tryToStartFallFlying(), "The airborne player starts actual Elytra flight");
                player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(player,
                        net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            if (age == 85) capture = "upright-cat-elytra-world.png";
            if (age == 90) {
                check(player.isFallFlying() && CameraPosture.upright(player), "The actual player flies on Elytra with the upright build");
                grid = new Grid(); mc.setScreen(grid);
            }
            if (grid != null) for (var actor : grid.actors) actor.tickCount = age;
            if (age == 125) capture = "cat-orientation-front.png";
            if (age == 135) { grid.angle = (float)Math.PI / 2; capture = "cat-orientation-side.png"; }
            if (age == 145) { grid.angle = (float)Math.PI; capture = "cat-orientation-back.png"; }
            if (age == 160) {
                check(checks > 100, "Movement direction back orientation and both tail joints were measured repeatedly");
                check(actualFlightChecks > 3, "The actual player's rendered flight orientation also matches its real movement");
                Nekogenix.LOGGER.info("Nekogenix cat orientation smoke passed: actual Elytra flight level ascent descent turns upright quadruped tail joins armor Accessibility Controls Config and all endpoint languages");
                mc.stop();
            }
        }
        private static String text(String key) { return net.minecraft.network.chat.Component.translatable(key).getString(); }
        private static net.minecraft.client.gui.components.OptionsList list() {
            return net.minecraft.client.Minecraft.getInstance().screen.children().stream().filter(net.minecraft.client.gui.components.OptionsList.class::isInstance)
                    .map(net.minecraft.client.gui.components.OptionsList.class::cast).findFirst().orElseThrow();
        }
        private static java.util.List<net.minecraft.client.gui.components.AbstractWidget> widgets() {
            var result = new java.util.ArrayList<net.minecraft.client.gui.components.AbstractWidget>();
            for (net.minecraft.client.gui.components.ContainerObjectSelectionList.Entry<?> entry : list().children()) for (var child : entry.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) result.add(widget);
            return result;
        }
        private static net.minecraft.client.gui.components.CycleButton<?> option(String key) {
            return widgets().stream().filter(w -> w instanceof net.minecraft.client.gui.components.CycleButton<?> && w.getMessage().getString().startsWith(text(key)))
                    .map(w -> (net.minecraft.client.gui.components.CycleButton<?>)w).findFirst().orElseThrow();
        }
        @SubscribeEvent public static void model(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) throws ReflectiveOperationException {
            if (!ENABLED || !(event.getRenderer().getModel() instanceof net.minecraft.client.model.CatModel<?>)) return;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player.tickCount >= 75 && player.tickCount < 90 && player.isFallFlying() && player.getUUID().equals(event.getEntity().getUUID())) {
                orientation(event, player, .97F); actualFlightChecks++;
            }
            if (grid == null || player.tickCount < 120) return;
            for (var actor : grid.actors) if (actor.getUUID().equals(event.getEntity().getUUID())) {
                boolean upright = CameraPosture.upright(actor);
                orientation(event, actor, .99F);
                if (upright) {
                    Object model = event.getRenderer().getModel();
                    var body = part(model, "body"); var root = part(model, "tail1"); var tip = part(model, "tail2");
                    var cap = point(root, 0, .5F);
                    check(cap.z < body.z - 2 && cap.z > body.z - 8 && cap.y > body.y + 3 * body.yScale && cap.y < body.y + 19 * body.yScale,
                            "The tail root actually intersects the torso instead of floating behind it");
                    check(point(root, 8, .5F).distance(point(tip, 0, .5F)) < .001, "The two animated tail segments remain connected");
                }
                checks++;
            }
        }
        private static void orientation(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event,
                net.minecraft.world.entity.player.Player actor, float tolerance) throws ReflectiveOperationException {
            boolean upright = CameraPosture.upright(actor);
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            var rotations = event.getRenderer().getClass().getDeclaredMethod("setupRotations", net.minecraft.world.entity.animal.Cat.class,
                    com.mojang.blaze3d.vertex.PoseStack.class, float.class, float.class, float.class, float.class);
            rotations.setAccessible(true); rotations.invoke(event.getRenderer(), event.getEntity(), pose, actor.tickCount + 1F, actor.yBodyRot, 1F, 1F);
            var matrix = pose.last().pose(); var velocity = actor.getDeltaMovement().toVector3f().normalize();
            var heading = matrix.transformDirection(new org.joml.Vector3f(0, upright ? 1 : 0, upright ? 0 : -1)).normalize();
            var back = matrix.transformDirection(new org.joml.Vector3f(0, upright ? 0 : 1, upright ? 1 : 0)).normalize();
            var expectedBack = new org.joml.Vector3f(0, 1, 0).sub(new org.joml.Vector3f(velocity).mul(velocity.y)).normalize();
            check(heading.dot(velocity) > tolerance, "The head leads the feet along the actual movement direction: " + heading + " vs " + velocity);
            check(back.dot(expectedBack) > tolerance, "The spine faces up instead of swimming or gliding belly up: " + back + " vs " + expectedBack);
        }
        private static net.minecraft.client.model.geom.ModelPart part(Object model, String name) throws ReflectiveOperationException {
            var field = net.minecraft.client.model.OcelotModel.class.getDeclaredField(name); field.setAccessible(true);
            return (net.minecraft.client.model.geom.ModelPart)field.get(model);
        }
        private static org.joml.Vector3f point(net.minecraft.client.model.geom.ModelPart part, float y, float z) {
            return new org.joml.Vector3f(0, y * part.yScale, z * part.zScale)
                    .rotate(new org.joml.Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot)).add(part.x, part.y, part.z);
        }
        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/" + capture)); }
            capture = null;
        }
        private static final class Grid extends net.minecraft.client.gui.screens.Screen {
            final Actor[] actors = new Actor[18]; float angle;
            Grid() {
                super(net.minecraft.network.chat.Component.literal("Cat travel orientation"));
                var mc = net.minecraft.client.Minecraft.getInstance();
                for (int i = 0; i < actors.length; i++) {
                    var actor = new Actor(i, mc.level); actor.setData(Content.CAT_FORM, true); actor.setOnGround(false);
                    actor.setPos(.5, -60, .5); actor.yBodyRot = actor.yBodyRotO = actor.yHeadRot = actor.yHeadRotO = 180;
                    actor.setYRot(180); actor.yRotO = 180;
                    CameraPosture.accept(actor, .43F, i < 12 ? 100 : 0);
                    if (i < 6) {
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE.getDefaultInstance());
                        actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance());
                    } else actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.ELYTRA.getDefaultInstance());
                    actors[i] = actor;
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                graphics.fill(0, 0, width, height, 0xff77868d); int w = width / 6, h = height / 3;
                for (int i = 0; i < actors.length; i++) {
                    int left = i % 6 * w, top = i / 6 * h;
                    graphics.drawString(font, (i < 6 ? "Swim " : i < 12 ? "Elytra " : "Four paws ") + i % 6, left + 2, top + 2, 0xffffff, false);
                    graphics.enableScissor(left, top + 13, left + w, top + h);
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(graphics, left + w / 2F, top + h / 2F, Math.min(h * .36F, w * .28F),
                            new org.joml.Vector3f(0, .35F, 0), new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateY(angle), new org.joml.Quaternionf(), actors[i]);
                    graphics.disableScissor();
                }
            }
        }
        private static final class Actor extends net.minecraft.client.player.RemotePlayer {
            final int mode;
            final net.minecraft.world.scores.PlayerTeam team;
            Actor(int mode, net.minecraft.client.multiplayer.ClientLevel level) {
                super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Travel" + mode)); this.mode = mode;
                team = new net.minecraft.world.scores.PlayerTeam(level.getScoreboard(), "travel"); team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
            }
            @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return team; }
            @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(Pose pose) { return CameraPosture.catDimensions(this, pose); }
            @Override public boolean isSwimming() { return mode < 6 || mode >= 12 && mode % 2 == 0; }
            @Override public boolean isInWater() { return isSwimming(); }
            @Override public boolean isInWaterOrBubble() { return isSwimming(); }
            @Override public boolean isFallFlying() { return !isSwimming(); }
            @Override public int getFallFlyingTicks() { return 40; }
            @Override public Vec3 getDeltaMovement() {
                return switch(mode % 6) {
                    case 0 -> new Vec3(0, 0, .2); case 1 -> new Vec3(.2, 0, 0); case 2 -> new Vec3(-.2, .08, 0);
                    case 3 -> new Vec3(0, -.08, -.2); case 4 -> new Vec3(0, .2, .04); default -> new Vec3(0, -.2, .04);
                };
            }
        }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new IllegalStateException("Cat orientation smoke failed: " + message); }
}
