package io.github.nekogenix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.nekogenix.client.CatPlayerRenderer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatHandsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catHandsSmokeTest");
    private static int customCalls;

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount != 20) return;
        CatTransformationTests.cat(player);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.serverLevel().setDayTime(6000);
        player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
    public static final class Client {
        private static final net.minecraft.world.item.Item[] USE = {Items.COOKED_BEEF, Items.POTION, Items.BOW,
                Items.CROSSBOW, Items.SHIELD, Items.TRIDENT, Items.BRUSH, Items.GOAT_HORN};
        private static String capture;
        private static int probes;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) throws ReflectiveOperationException {
            if (!ENABLED) return;
            var mc = Minecraft.getInstance();
            var player = mc.player;
            if (player == null) return;
            int age = player.tickCount;
            mc.getToasts().clear();
            if (age == 45) {
                check(player.getData(Content.CAT_FORM), "The actual player has transformed");
                for (var dominant : HumanoidArm.values()) {
                    player.setMainArm(dominant);
                    for (var hand : InteractionHand.values()) {
                        for (var item : new net.minecraft.world.item.Item[] {Items.STONE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD,
                                Items.APPLE, Items.BOW, Items.CROSSBOW, Items.SHIELD, Items.TRIDENT, Content.INJECTION.get(), Content.COTTON_SWAB.get()}) {
                            player.setItemInHand(hand, item.getDefaultInstance());
                            Capture rest = probe(hand, 0, 0);
                            for (float equip : new float[] {0, .5F, 1}) for (float swing : new float[] {0, .12F, .35F, .7F, .99F}) {
                                Capture moving = probe(hand, swing, equip);
                                if (item != Content.COTTON_SWAB.get()) attached(rest, moving);
                            }
                        }
                    }
                }
                player.setMainArm(HumanoidArm.RIGHT);
                player.setItemInHand(InteractionHand.MAIN_HAND, Items.STONE.getDefaultInstance());
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                for (boolean cat : new boolean[] {false, true}) {
                    player.setData(Content.CAT_FORM, cat);
                    var e = handEvent(InteractionHand.MAIN_HAND, new Capture(), .4F, 0);
                    CatPlayerRenderer.hand(e);
                    check(e.isCanceled() == cat, "Only cat hands replace rendering");
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, Items.FILLED_MAP.getDefaultInstance());
                var map = handEvent(InteractionHand.MAIN_HAND, new Capture(), .4F, 0);
                CatPlayerRenderer.hand(map);
                check(!map.isCanceled(), "Maps keep their native two hand rendering");
                var remaining = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("useItemRemaining");
                remaining.setAccessible(true);
                for (int i = 0; i < USE.length; i++) {
                    var hand = i % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    player.setItemInHand(hand, USE[i].getDefaultInstance());
                    player.startUsingItem(hand);
                    remaining.setInt(player, Math.max(1, player.getUseItemRemainingTicks() - 12));
                    check(player.isUsingItem(), "The item is actually in use: " + USE[i]);
                    probe(hand, .35F, .25F);
                    player.stopUsingItem();
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, Items.STONE.getDefaultInstance());
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (age == 60 || age == 80) {
                player.swing(InteractionHand.MAIN_HAND);
                if (age == 80) player.setItemInHand(InteractionHand.OFF_HAND, Items.DIAMOND_PICKAXE.getDefaultInstance());
            }
            if (age == 62 || age == 82) capture = age == 62 ? "cat-block-attack.png" : "cat-two-hands-attack.png";
            if (age == 100) player.swing(InteractionHand.OFF_HAND);
            if (age == 102) capture = "cat-offhand-attack.png";
            if (age == 125) {
                check(probes > 600, "Both hands dominant arms equip transitions attacks and item uses were checked");
                Nekogenix.LOGGER.info("Nekogenix cat hands smoke passed: {} native item meshes and rigid paw grips across blocks tools weapons food both arms bows crossbows shields tridents brushing horn and custom hand transforms", probes);
                mc.stop();
            }
        }

        private static Capture probe(InteractionHand hand, float swing, float equip) throws ReflectiveOperationException {
            var mc = Minecraft.getInstance();
            var player = mc.player;
            var stack = player.getItemInHand(hand);
            Capture nativeItem = new Capture();
            var method = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredMethod("renderArmWithItem",
                    net.minecraft.client.player.AbstractClientPlayer.class, float.class, float.class, InteractionHand.class,
                    float.class, ItemStack.class, float.class, PoseStack.class, MultiBufferSource.class, int.class);
            method.setAccessible(true);
            int before = customCalls;
            method.invoke(mc.gameRenderer.itemInHandRenderer, player, .5F, 0F, hand, swing, stack, equip, new PoseStack(), nativeItem, 15728880);
            int nativeCalls = customCalls - before;
            Capture combined = new Capture();
            var event = handEvent(hand, combined, swing, equip);
            before = customCalls;
            CatPlayerRenderer.hand(event);
            check(event.isCanceled(), "The cat renders each hand exactly once");
            check(customCalls - before == nativeCalls, "Custom hand hooks run once, matching vanilla");
            check(combined.paw != null && combined.paw.vertices.size() == 24, "Exactly one paw is rendered with the item");
            for (var entry : nativeItem.meshes.entrySet()) {
                var actual = combined.meshes.get(entry.getKey());
                check(actual != null && actual.vertices.size() == entry.getValue().vertices.size(), "Item geometry is preserved for " + stack);
                for (int i = 0; i < actual.vertices.size(); i++) check(actual.vertices.get(i).distance(entry.getValue().vertices.get(i)) < .00002,
                        "Native item motion is preserved for " + stack + " swing " + swing + " using " + player.isUsingItem());
            }
            check(!nativeItem.meshes.isEmpty(), "Native item geometry was captured");
            combined.item = combined.meshes.get(nativeItem.meshes.keySet().iterator().next());
            probes++;
            return combined;
        }

        private static RenderHandEvent handEvent(InteractionHand hand, Capture buffers, float swing, float equip) {
            return new RenderHandEvent(hand, new PoseStack(), buffers, 15728880, .5F, 0, swing, equip, Minecraft.getInstance().player.getItemInHand(hand));
        }

        private static void attached(Capture rest, Capture moving) {
            Matrix4f base = frame(rest.item.vertices).invert();
            Matrix4f current = frame(moving.item.vertices).invert();
            for (int i = 0; i < rest.paw.vertices.size(); i++) {
                var a = base.transformPosition(new Vector3f(rest.paw.vertices.get(i)));
                var b = current.transformPosition(new Vector3f(moving.paw.vertices.get(i)));
                check(a.distance(b) < .0001, "The paw cannot slide rotate or detach relative to the held item during a swing");
            }
        }

        private static Matrix4f frame(List<Vector3f> vertices) {
            var origin = vertices.getFirst();
            Vector3f x = null, y = null;
            for (var point : vertices) {
                var delta = new Vector3f(point).sub(origin);
                if (x == null && delta.lengthSquared() > .00001) x = delta.normalize();
                else if (x != null && new Vector3f(x).cross(delta).lengthSquared() > .00001) {
                    y = delta.sub(new Vector3f(x).mul(delta.dot(x))).normalize();
                    break;
                }
            }
            check(x != null && y != null, "A nondegenerate item surface defines its rigid frame");
            var z = new Vector3f(x).cross(y);
            return new Matrix4f().setColumn(0, new org.joml.Vector4f(x, 0)).setColumn(1, new org.joml.Vector4f(y, 0))
                    .setColumn(2, new org.joml.Vector4f(z, 0)).setColumn(3, new org.joml.Vector4f(origin, 1));
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = Minecraft.getInstance();
            var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(capture);
            java.nio.file.Files.createDirectories(path.getParent());
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
            capture = null;
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Extension {
        @SubscribeEvent public static void register(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            if (!ENABLED) return;
            event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                @Override public boolean applyForgeHandTransform(PoseStack pose, net.minecraft.client.player.LocalPlayer player,
                        HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
                    customCalls++;
                    pose.translate(.3, -.4 - equip, -.7);
                    pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(37 + swing * 53));
                    return true;
                }
            }, Content.COTTON_SWAB.get());
        }
    }

    private static final class Capture implements MultiBufferSource {
        final Map<RenderType, Mesh> meshes = new LinkedHashMap<>();
        Mesh paw, item;
        @Override public VertexConsumer getBuffer(RenderType type) {
            Mesh mesh = meshes.computeIfAbsent(type, key -> new Mesh());
            if (paw == null) paw = mesh;
            return mesh;
        }
    }

    private static final class Mesh implements VertexConsumer {
        final List<Vector3f> vertices = new ArrayList<>();
        @Override public VertexConsumer addVertex(float x, float y, float z) { vertices.add(new Vector3f(x,y,z)); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Cat hands smoke failed: " + message);
    }
}
