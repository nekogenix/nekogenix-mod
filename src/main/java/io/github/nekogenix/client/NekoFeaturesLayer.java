package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.client.skin.CatSkins;
import io.github.nekogenix.gameplay.CatForm;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class NekoFeaturesLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation FALLBACK = ResourceLocation.withDefaultNamespace("textures/entity/cat/tabby.png");
    private final NekoFeaturesModel features = new NekoFeaturesModel();
    private final Map<AbstractClientPlayer, Motion> motions = new WeakHashMap<>();

    private NekoFeaturesLayer(PlayerRenderer renderer) { super(renderer); }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float walk, float speed, float partial, float age, float yaw, float pitch) {
        if (!player.getData(Content.FELINE_DIET) || CatForm.active(player) || player.isInvisible() || player.isSpectator()) return;
        var motion = motions.computeIfAbsent(player, key -> new Motion());
        motion.update(player);
        features.animateEars(age + (player.getUUID().hashCode() & 255),
                Mth.lerp(partial, motion.oldForward, motion.forward), Mth.lerp(partial, motion.oldSideways, motion.sideways),
                Mth.lerp(partial, motion.oldVertical, motion.vertical),
                Mth.wrapDegrees(Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot)
                        - Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot)) * Mth.DEG_TO_RAD,
                motion.pitch.at(partial), motion.yaw.at(partial), motion.roll.at(partial));
        features.shakeEars(NekoEarShake.progress(player, partial));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0);
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            features.renderEars(pose, buffers, CatSkins.texture(player.getUUID(), FALLBACK), light, overlay);
            pose.popPose();
        }
    }

    private static final class Motion {
        private int tick = Integer.MIN_VALUE;
        private Vec3 position;
        private float forward, sideways, vertical;
        private float oldForward, oldSideways, oldVertical;
        private float headYaw, headPitch;
        private final Sway pitch = new Sway(), yaw = new Sway(), roll = new Sway();

        private void update(AbstractClientPlayer player) {
            if (tick == player.tickCount) return;
            boolean reset = tick == Integer.MIN_VALUE || player.tickCount < tick || player.tickCount - tick > 4
                    || position == null || position.distanceToSqr(player.position()) > 16;
            int elapsed = reset ? 1 : player.tickCount - tick;
            Vec3 movement = player.getDeltaMovement();
            if (!reset && player != net.minecraft.client.Minecraft.getInstance().player) {
                movement = player.position().subtract(position).scale(1D / elapsed);
            }
            position = player.position(); tick = player.tickCount;
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            double along = movement.z * Mth.cos(yaw) - movement.x * Mth.sin(yaw);
            double across = movement.x * Mth.cos(yaw) + movement.z * Mth.sin(yaw);
            float targetForward = (float)Math.tanh(along * 2);
            float targetSideways = (float)Math.tanh(across * 2);
            float targetVertical = player.onGround() ? 0 : (float)Math.tanh(movement.y);
            if (reset) {
                forward = targetForward; sideways = targetSideways; vertical = targetVertical;
                headYaw = player.yHeadRot; headPitch = player.getXRot();
                pitch.reset(); this.yaw.reset(); roll.reset();
            }
            oldForward = forward; oldSideways = sideways; oldVertical = vertical;
            float blend = 1 - (float)Math.pow(.68, elapsed);
            forward = Mth.lerp(blend, forward, targetForward);
            sideways = Mth.lerp(blend, sideways, targetSideways);
            vertical = Mth.lerp(blend, vertical, targetVertical);
            float yawChange = Mth.wrapDegrees(player.yHeadRot - headYaw) * Mth.DEG_TO_RAD / elapsed;
            float pitchChange = Mth.wrapDegrees(player.getXRot() - headPitch) * Mth.DEG_TO_RAD / elapsed;
            pitch.tick(Mth.clamp(-(forward - oldForward) * .5F - (vertical - oldVertical) * .25F
                    - pitchChange * .3F, -.16F, .16F), elapsed);
            this.yaw.tick(Mth.clamp(-yawChange * .4F, -.18F, .18F), elapsed);
            roll.tick(Mth.clamp(-(sideways - oldSideways) * .4F, -.12F, .12F), elapsed);
            headYaw = player.yHeadRot; headPitch = player.getXRot();
        }
    }

    private static final class Sway {
        private float previous, value, velocity;

        private void tick(float target, int elapsed) {
            previous = value;
            for (int i = 0; i < elapsed; i++) {
                velocity = (velocity + (target - value) * .32F) * .65F;
                value = Mth.clamp(value + velocity, -.22F, .22F);
            }
        }

        private float at(float partial) { return Mth.lerp(partial, previous, value); }
        private void reset() {
            previous = value = velocity = 0;
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins()) {
                var renderer = event.getSkin(skin);
                if (renderer instanceof PlayerRenderer playerRenderer) playerRenderer.addLayer(new NekoFeaturesLayer(playerRenderer));
            }
        }
    }
}
