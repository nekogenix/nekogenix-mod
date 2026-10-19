package io.github.nekogenix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatMotionSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catMotionSmokeTest");

    @SubscribeEvent public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            player.serverLevel().setDayTime(6000);
            player.setGameMode(GameType.CREATIVE);
            CatTransformationTests.cat(player);
            player.teleportTo(player.serverLevel(), 128.5, -60, 125, 0, 0);
        }
        if (player.tickCount == 100) Nekogenix.LOGGER.info("Nekogenix motion server smoke passed: player cat form and creative inventory");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static Preview preview;
        private static String capture;
        private static Mesh steadyTail;
        private static final java.util.Map<java.util.UUID, Mesh> meshes = new java.util.HashMap<>();

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 40) {
                preview = new Preview();
                mc.setScreen(preview);
            }
            if (preview == null) return;
            preview.age = age;
            for (var actor : preview.actors) {
                actor.tickCount = age;
                actor.walkAnimation.update(.8F, 1);
                actor.walkDistO = actor.walkDist; actor.walkDist += .2F;
            }
            if (age == 85) steadyTail = meshes.get(preview.actors[0].getUUID());
            if (age == 100) {
                var right = meshes.get(preview.actors[0].getUUID());
                var left = meshes.get(preview.actors[1].getUUID());
                var helmet = meshes.get(preview.actors[11].getUUID());
                check(right != null && left != null && helmet != null, "All player cat models render");
                check(right.vertices.size() - helmet.vertices.size() == 48, "Equipping headgear hides exactly the two ear cubes");
                check(right.center(72).distance(left.center(72)) > .02F, "Creative flight paws lean in opposite movement directions");
                check(right.center(48).distance(left.center(48)) > .02F, "The root tail segment follows flight direction");
                check(steadyTail != null && right.center(24).distance(steadyTail.center(24)) < .005F,
                        "The tip does not inherit the rapid walking oscillation in steady flight");
            }
            if (age == 80 || age == 100 || age == 130 || age == 140) capture = "cat-motion-" + age + ".png";
            if (age == 150) {
                Nekogenix.LOGGER.info("Nekogenix motion client smoke passed: creative direction tail inertia hidden ears flight swimming capes and elytra");
                mc.stop();
            }
        }

        @SubscribeEvent public static void rendered(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
            if (!ENABLED || preview == null) return;
            for (var actor : preview.actors) if (actor.getUUID().equals(event.getEntity().getUUID())) {
                var mesh = new Mesh();
                event.getRenderer().getModel().renderToBuffer(new com.mojang.blaze3d.vertex.PoseStack(), mesh, 15728880, 0);
                meshes.put(actor.getUUID(), mesh);
                break;
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(capture));
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            capture = null;
        }

        private static void check(boolean condition, String message) {
            if (!condition) throw new IllegalStateException("Feline motion smoke failed: " + message);
        }

        private static final class Preview extends net.minecraft.client.gui.screens.Screen {
            private final Actor[] actors = new Actor[12];
            private int age;
            private Preview() {
                super(net.minecraft.network.chat.Component.literal("Feline motion"));
                var mc = net.minecraft.client.Minecraft.getInstance();
                for (int i = 0; i < actors.length; i++) {
                    var actor = new Actor(i, mc.level);
                    actor.setData(Content.CAT_FORM, true);
                    actor.refreshDimensions();
                    actor.setOnGround(i == 8 || i == 9 || i == 11);
                    actor.getAbilities().flying = i < 3 || i == 10;
                    actor.setPos(128.5, -60, 125);
                    actor.xo = actor.xCloakO = actor.xCloak = actor.getX();
                    actor.yo = actor.yCloakO = actor.yCloak = actor.getY();
                    actor.zo = actor.getZ(); actor.zCloakO = actor.zCloak = actor.getZ() - .6;
                    actor.oBob = actor.bob = .08F;
                    if (i == 9) actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.IRON_CHESTPLATE.getDefaultInstance());
                    if (i == 3 || i == 4 || i == 10) actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.Items.ELYTRA.getDefaultInstance());
                    if (i == 11) actor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance());
                    actors[i] = actor;
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                graphics.fill(0, 0, width, height, 0xff75828b);
                String[] names = {"Creative right", "Creative left", "Creative up", "Elytra dive", "Elytra climb", "Swim right",
                        "Swim left", "Swim still", "Running cape", "Cape over armor", "Elytra resting", "Helmet hides ears"};
                int cw = width / 4, ch = height / 3;
                for (int i = 0; i < actors.length; i++) {
                    int x = i % 4 * cw, y = i / 4 * ch;
                    graphics.drawString(font, names[i], x + 3, y + 3, 0xffffff, false);
                    net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                            x + 2, y + 13, x + cw - 2, y + ch, (int)(ch * .68), 0,
                            age < 110 ? .8F : 3.3F + (age - 130) * .002F, .15F, actors[i]);
                }
            }
        }

        private static final class Actor extends net.minecraft.client.player.RemotePlayer {
            private final int mode;
            private final net.minecraft.world.scores.PlayerTeam team;
            private Actor(int mode, net.minecraft.client.multiplayer.ClientLevel level) {
                super(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(("cat-motion-"+mode).getBytes(java.nio.charset.StandardCharsets.UTF_8)), "CatMotion"+mode));
                this.mode = mode;
                team = new net.minecraft.world.scores.PlayerTeam(level.getScoreboard(), "motion");
                team.setNameTagVisibility(net.minecraft.world.scores.Team.Visibility.NEVER);
            }
            @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return team; }
            @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
                return net.minecraft.world.entity.EntityType.CAT.getDimensions();
            }
            @Override public net.minecraft.world.phys.Vec3 getDeltaMovement() {
                return switch (mode) {
                    case 0, 5 -> new net.minecraft.world.phys.Vec3(.25, 0, 0);
                    case 1, 6 -> new net.minecraft.world.phys.Vec3(-.25, 0, 0);
                    case 2 -> new net.minecraft.world.phys.Vec3(0, .2, .12);
                    case 3 -> new net.minecraft.world.phys.Vec3(.16, -.25, .3);
                    case 4 -> new net.minecraft.world.phys.Vec3(-.16, .25, .3);
                    case 7, 11 -> net.minecraft.world.phys.Vec3.ZERO;
                    default -> new net.minecraft.world.phys.Vec3(0, 0, .3);
                };
            }
            @Override public boolean isFallFlying() { return mode == 3 || mode == 4; }
            @Override public int getFallFlyingTicks() { return isFallFlying() ? 30 : 0; }
            @Override public boolean isInWater() { return mode >= 5 && mode <= 7; }
            @Override public boolean isInWaterOrBubble() { return isInWater(); }
            @Override public boolean isSwimming() { return isInWater(); }
            @Override public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) { return true; }
            @Override public net.minecraft.client.resources.PlayerSkin getSkin() {
                return new net.minecraft.client.resources.PlayerSkin(
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"), null,
                        mode == 8 || mode == 9 ? net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/entity/elytra.png") : null,
                        null, net.minecraft.client.resources.PlayerSkin.Model.WIDE, false);
            }
        }

        private static final class Mesh implements com.mojang.blaze3d.vertex.VertexConsumer {
            private final java.util.List<org.joml.Vector3f> vertices = new java.util.ArrayList<>();
            private org.joml.Vector3f center(int offset) {
                var center = new org.joml.Vector3f();
                for (int i = vertices.size() - offset; i < vertices.size() - offset + 24; i++) center.add(vertices.get(i));
                return center.div(24);
            }
            @Override public Mesh addVertex(float x, float y, float z) { vertices.add(new org.joml.Vector3f(x,y,z)); return this; }
            @Override public Mesh setColor(int r, int g, int b, int a) { return this; }
            @Override public Mesh setUv(float u, float v) { return this; }
            @Override public Mesh setUv1(int u, int v) { return this; }
            @Override public Mesh setUv2(int u, int v) { return this; }
            @Override public Mesh setNormal(float x, float y, float z) { return this; }
        }
    }
}
