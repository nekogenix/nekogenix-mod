package io.github.nekogenix.client;

import io.github.nekogenix.gameplay.CatForm;
import io.github.nekogenix.gameplay.EntityTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class CameraTargeting {
    public static void update(float partial) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null || mc.getCameraEntity() != player || mc.hitResult == null
                || player.isSpectator() || !player.isAlive() || player.isSleeping()) return;
        var eye = player.getEyePosition(partial);
        var origin = mc.options.getCameraType().isFirstPerson()
                ? new Vec3(Mth.lerp(partial, player.xo, player.getX()),
                        CameraHeight.position(player, partial, eye.y, player.getEyeHeight()), Mth.lerp(partial, player.zo, player.getZ()))
                : eye;
        var direction = player.getViewVector(partial);
        boolean adjusted = mc.options.getCameraType().isFirstPerson() && (CatForm.active(player) || Math.abs(origin.y - eye.y) >= .02);
        var result = adjusted ? EntityTargeting.pick(player, origin, direction) : mc.hitResult;
        result = EntityTargeting.throughPlants(player, origin, direction, result);
        if (result == mc.hitResult) return;
        mc.hitResult = result;
        mc.crosshairPickEntity = result instanceof EntityHitResult hit ? hit.getEntity() : null;
    }

    private CameraTargeting() {}
}
