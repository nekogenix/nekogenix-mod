package io.github.nekogenix;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.nekogenix.state.NekoState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatActionSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catActionSmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount != 20) return;
        CatTransformationTests.set(player, NekoState.Phase.CAT, 0, 9);
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(player.serverLevel(), 128.5, -55, 125, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Items.STONE.getDefaultInstance());
        player.setItemInHand(InteractionHand.OFF_HAND, Items.STONE.getDefaultInstance());
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static Preview preview;
        private static final java.util.Map<java.util.UUID, Mesh> meshes = new java.util.HashMap<>();
        private static final Mesh[] resting = new Mesh[8];
        private static final java.util.Set<InteractionHand> swings = java.util.EnumSet.noneOf(InteractionHand.class);
        private static String capture;
        private static int canceledLandings;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 35) {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_SMALL_FALL, 1, 1);
                mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BIG_FALL, 1, 1);
                check(canceledLandings == 2, "Client predicted landing sounds are suppressed for feline players");
            }
            if (age == 40) { preview = new Preview(); mc.setScreen(preview); }
            if (preview == null) return;
            for (int i = 0; i < preview.actors.length; i++) {
                var actor = preview.actors[i];
                actor.tickCount = age;
                actor.swingingArm = i % 4 == 2 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                actor.attackAnim = actor.oAttackAnim = age >= 85 && i % 4 != 0 ? .5F : 0;
                if (age == 80) resting[i] = meshes.get(actor.getUUID());
            }
            if (age == 100) {
                for (int i = 0; i < 8; i++) {
                    var current = meshes.get(preview.actors[i].getUUID());
                    check(current != null && resting[i] != null, "Every airborne player cat renders");
                    float right = current.center(72).distance(resting[i].center(72));
                    float left = current.center(96).distance(resting[i].center(96));
                    check(i % 4 == 1 || i % 4 == 3 ? right > .02F : right < .001F, "Only the acting right paw changes pose in mode " + i);
                    check(i % 4 == 2 ? left > .02F : left < .001F, "Only the acting left paw changes pose in mode " + i);
                }
                capture = "cat-air-actions.png";
            }
            if (age == 105) { mc.setScreen(null); mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); }
            if (age == 110) mc.player.swing(InteractionHand.OFF_HAND);
            if (age == 113) capture = "cat-air-offhand-action.png";
            if (age == 125) mc.player.swing(InteractionHand.MAIN_HAND);
            if (age == 128) capture = "cat-air-mainhand-action.png";
            if (age == 140) {
                check(swings.size() == 2, "Native first person actions reach both paws in creative flight");
                Nekogenix.LOGGER.info("Nekogenix action smoke passed: both paws and empty hand attacks animate in flight and falling with native first person swings");
                mc.stop();
            }
        }

        @SubscribeEvent(receiveCanceled = true) public static void hand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
            if (ENABLED && Minecraft.getInstance().screen == null && event.getSwingProgress() > .1F) swings.add(event.getHand());
        }

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void landing(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity event) {
            if (ENABLED && event.getEntity() == Minecraft.getInstance().player && event.isCanceled() && event.getSound() != null
                    && (event.getSound().value() == net.minecraft.sounds.SoundEvents.PLAYER_SMALL_FALL
                    || event.getSound().value() == net.minecraft.sounds.SoundEvents.PLAYER_BIG_FALL)) canceledLandings++;
        }

        @SubscribeEvent public static void rendered(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
            if (!ENABLED || preview == null) return;
            for (var actor : preview.actors) if (actor.getUUID().equals(event.getEntity().getUUID())) {
                var mesh = new Mesh();
                event.getRenderer().getModel().renderToBuffer(new PoseStack(), mesh, 15728880, 0);
                meshes.put(actor.getUUID(), mesh);
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || capture == null) return;
            var mc = Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(capture));
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
            capture = null;
        }

        private static void check(boolean condition, String message) {
            if (!condition) throw new IllegalStateException("Feline action smoke failed: " + message);
        }

        private static final class Preview extends Screen {
            private final RemotePlayer[] actors = new RemotePlayer[8];
            private Preview() {
                super(Component.literal("Airborne paw actions"));
                var mc = Minecraft.getInstance();
                for (int i = 0; i < actors.length; i++) {
                    var actor = new RemotePlayer(mc.level, new com.mojang.authlib.GameProfile(
                            java.util.UUID.randomUUID(), "CatAction" + i));
                    actor.setData(Content.CAT_FORM, true);
                    actor.getAbilities().flying = i < 4;
                    actor.setOnGround(false);
                    actor.setDeltaMovement(0, i < 4 ? 0 : -.5, .05);
                    actor.setItemInHand(InteractionHand.MAIN_HAND, i % 4 == 3 ? net.minecraft.world.item.ItemStack.EMPTY : Items.STONE.getDefaultInstance());
                    actor.setItemInHand(InteractionHand.OFF_HAND, Items.STONE.getDefaultInstance());
                    actors[i] = actor;
                }
            }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                graphics.fill(0, 0, width, height, 0xff75828b);
                String[] actions = {"Rest", "Right action", "Left action", "Empty hand attack"};
                for (int i = 0; i < actors.length; i++) {
                    int cw = width / 4, ch = height / 2, x = i % 4 * cw, y = i / 4 * ch;
                    graphics.drawString(font, (i < 4 ? "Creative: " : "Falling: ") + actions[i % 4], x + 3, y + 3, 0xffffff, false);
                    InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x, y + 15, x + cw, y + ch,
                            (int)(ch * .55), 0, 1.6F, .2F, actors[i]);
                }
            }
        }

        private static final class Mesh implements com.mojang.blaze3d.vertex.VertexConsumer {
            private final java.util.List<org.joml.Vector3f> vertices = new java.util.ArrayList<>();
            private org.joml.Vector3f center(int offset) {
                var center = new org.joml.Vector3f();
                for (int i = vertices.size() - offset; i < vertices.size() - offset + 24; i++) center.add(vertices.get(i));
                return center.div(24);
            }
            @Override public Mesh addVertex(float x, float y, float z) { vertices.add(new org.joml.Vector3f(x, y, z)); return this; }
            @Override public Mesh setColor(int r, int g, int b, int a) { return this; }
            @Override public Mesh setUv(float u, float v) { return this; }
            @Override public Mesh setUv1(int u, int v) { return this; }
            @Override public Mesh setUv2(int u, int v) { return this; }
            @Override public Mesh setNormal(float x, float y, float z) { return this; }
        }
    }
}
