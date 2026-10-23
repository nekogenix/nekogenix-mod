package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class NekoEarShake {
    private static final int DURATION = 30;
    private static final Map<AbstractClientPlayer, Shake> STATES = new WeakHashMap<>();
    private static ClientLevel level;

    public static float progress(AbstractClientPlayer player, float partial) {
        var state = STATES.get(player);
        return state == null ? -1 : state.progress(partial);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != level) { STATES.clear(); level = mc.level; }
        if (level == null || mc.player == null || mc.isPaused()) return;
        STATES.keySet().removeIf(player -> player.isRemoved());
        for (var player : level.players()) {
            if (!player.isAlive() || !player.getData(Content.FELINE_DIET) || CatForm.active(player)
                    || player.isInvisible() || player.isSpectator() || !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    || !(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer)) {
                STATES.remove(player);
                continue;
            }
            var state = STATES.computeIfAbsent(player, key -> new Shake());
            state.tick(headInWater(player));
            if (state.age >= 6 && state.age <= 22
                    && player.distanceToSqr(mc.player) <= 4096) droplets(player);
        }
    }

    private static boolean headInWater(AbstractClientPlayer player) {
        double scale = player.getScale();
        var center = player.position().add(0, player.getDefaultDimensions(player.getPose()).eyeHeight() * scale, 0);
        var head = new net.minecraft.world.phys.AABB(center, center).inflate(.22 * scale);
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                net.minecraft.core.BlockPos.containing(head.minX, head.minY, head.minZ),
                net.minecraft.core.BlockPos.containing(head.maxX, head.maxY, head.maxZ))) {
            var fluid = player.level().getFluidState(pos);
            if (fluid.is(net.minecraft.tags.FluidTags.WATER) && head.intersects(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + fluid.getHeight(player.level(), pos), pos.getZ() + 1)) return true;
        }
        return false;
    }

    private static void droplets(AbstractClientPlayer player) {
        float yaw = player.yHeadRot * Mth.DEG_TO_RAD;
        var right = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        var forward = Vec3.directionFromRotation(player.getXRot(), player.yHeadRot);
        var up = forward.cross(right);
        var center = player.position().add(0, player.getDefaultDimensions(player.getPose()).eyeHeight() * player.getScale(), 0)
                .add(up.scale(.28 * player.getScale())).subtract(forward.scale(.06 * player.getScale()));
        var movement = player.getDeltaMovement().scale(.2);
        var random = player.getRandom();
        for (int side = -1; side <= 1; side += 2) {
            if (random.nextFloat() > .35F) continue;
            var position = center.add(right.scale(side * (.2 + random.nextDouble() * .05) * player.getScale()))
                    .add(up.scale(random.nextDouble() * .08 * player.getScale()));
            double outward = side * (.06 + random.nextDouble() * .06);
            var velocity = right.scale(outward).add(forward.scale((random.nextDouble() - .5) * .08)).add(movement);
            player.level().addParticle(ParticleTypes.SPLASH, position.x, position.y, position.z,
                    velocity.x, random.nextDouble() * .025, velocity.z);
        }
    }

    static final class Shake {
        private boolean wet;
        private int age = -1;

        void tick(boolean inWater) {
            if (inWater) {
                wet = true;
                age = -1;
            } else if (wet) {
                wet = false;
                age = 0;
            } else if (age >= 0 && ++age >= DURATION) age = -1;
        }

        float progress(float partial) {
            return age < 0 ? -1 : Mth.clamp((age + partial) / DURATION, 0, 1);
        }
    }
}
