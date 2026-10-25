package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraPosture {
    public static final float UPRIGHT_HEIGHT = 1.8F;
    public static final int QUADRUPED_MAX_CAMERA = 40;

    public static boolean seated(Player player) {
        return player.hasPose(Pose.SITTING) || player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
    }

    public static boolean ordinaryPose(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isFallFlying()
                && !player.isSwimming() && (!player.isInWaterOrBubble() || player.onGround())
                && !player.isAutoSpinAttack() && !player.isPassenger() && !player.hasPose(Pose.SITTING) && !player.onClimbable();
    }

    public static boolean standingCatPose(Player player) {
        return ordinaryPose(player) || CatForm.active(player) && player.isAlive() && !player.isSpectator()
                && (player.isInWaterOrBubble() || player.onClimbable()) && !player.isSwimming() && !player.isSleeping()
                && !player.isFallFlying() && !player.isAutoSpinAttack() && !player.isPassenger() && !player.hasPose(Pose.SITTING);
    }

    public static float catEye(Player player) {
        if (seated(player)) return .595F * player.getScale();
        float height = player.hasData(Content.CAMERA_VIEW) ? player.getData(Content.CAMERA_VIEW).bodyEye : -1;
        float resting = (player.isCrouching() ? CatForm.CROUCHING_EYE_HEIGHT : CatForm.STANDING_EYE_HEIGHT) * player.getScale();
        return height < 0 ? resting : standingCatPose(player) ? height : Math.min(height, resting);
    }

    public static boolean upright(Player player) {
        return CatForm.active(player) && !seated(player) && player.hasData(Content.CAMERA_VIEW) && player.getData(Content.CAMERA_VIEW).upright;
    }

    public static float modelEye(Player player) {
        if (seated(player)) return .595F;
        return upright(player) ? player.isCrouching() ? 1.27F : 1.62F
                : player.isCrouching() ? CatForm.CROUCHING_EYE_HEIGHT : CatForm.STANDING_EYE_HEIGHT;
    }

    public static EntityDimensions catDimensions(Player player, Pose pose) {
        float eye = catEye(player) / player.getScale();
        float body = upright(player) && standingCatPose(player) ? player.isCrouching() ? 1.45F : UPRIGHT_HEIGHT : .7F;
        return CatForm.dimensions(pose).scale(1, Math.max(body, eye + .18F) / .7F).withEyeHeight(eye);
    }

    public static double clearance(Player player, Vec3 feet, double height) {
        double half = player.getBbWidth() / 2 - 1.0E-5;
        var column = new AABB(feet.x - half, feet.y + .001, feet.z - half, feet.x + half, feet.y + height, feet.z + half);
        double limit = height;
        for (var shape : player.level().getBlockCollisions(player, column)) for (var box : shape.toAabbs()) {
            if (box.maxY <= Math.max(feet.y, player.getY()) + .001 || !box.intersects(column)) continue;
            limit = Math.min(limit, Math.max(.08, box.minY - feet.y));
        }
        return limit;
    }

    public static boolean standingRoom(Player player) {
        return !player.level().getBlockCollisions(player, Player.STANDING_DIMENSIONS.scale(player.getScale())
                .makeBoundingBox(player.position()).deflate(1.0E-7)).iterator().hasNext();
    }

    public static void prone(Player player, int preference) {
        var session = player.getData(Content.CAT_SIZE);
        if (session.passage && (!ordinaryPose(player) || player.getAbilities().flying
                || player.tickCount >= session.passageUntil && standingRoom(player))) session.passage = false;
        if (session.prone && player.getForcedPose() != Pose.SWIMMING) session.prone = false;
        boolean wanted = !CatForm.active(player) && ordinaryPose(player) && (CameraGeometry.selected(preference) < 1
                || session.passage || session.passageCompressed && passageJumpPending(player));
        if (wanted && player.getForcedPose() == null && player.level().noCollision(player,
                EntityDimensions.scalable(.6F, .6F).scale(player.getScale()).makeBoundingBox(player.position()).deflate(1.0E-7))) {
            session.prone = true;
            player.setForcedPose(Pose.SWIMMING);
            player.setPose(Pose.SWIMMING);
        } else if (!wanted && session.prone) {
            player.setForcedPose(null);
            session.prone = false;
            if (ordinaryPose(player) && player.hasPose(Pose.SWIMMING) && standingRoom(player)) {
                player.setPose(player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);
            }
        }
        if (session.prone && player.getForcedPose() == Pose.SWIMMING && !player.hasPose(Pose.SWIMMING)) {
            player.setPose(Pose.SWIMMING);
        }
    }

    public static boolean passage(Player player) {
        return player.getData(Content.CAT_SIZE).passage;
    }

    public static boolean preparePassageJump(Player player, Vec3 direction) {
        return preparePassageJump(player, direction, true);
    }

    public static boolean preparePassageJump(Player player, Vec3 direction, boolean jumping) {
        if (direction.equals(Vec3.ZERO)) { cancelPassageJump(player); return false; }
        if (!Double.isFinite(direction.x) || !Double.isFinite(direction.z) || direction.y != 0
                || direction.lengthSqr() < .0001 || direction.lengthSqr() > 1.001
                || !ordinaryPose(player) || player.getAbilities().flying || !player.onGround()
                || player.getForcedPose() != null && !(player.getData(Content.CAT_SIZE).prone && player.getForcedPose() == Pose.SWIMMING)
                || player.getBbHeight() <= player.getScale() || passage(player)) return false;
        var offset = direction.normalize().scale(.18 * player.getScale());
        var facing = Vec3.directionFromRotation(0, player.getYRot());
        if (offset.dot(facing) <= .001) return false;
        if (passageFits(player, player.position(), offset)) {
            if (!player.isShiftKeyDown()) return false;
            preparePassage(player, direction, player.getY());
            return true;
        }
        if (!jumping) return false;
        var reach = player.getBoundingBox().expandTowards(offset).deflate(1.0E-7);
        for (var shape : player.level().getBlockCollisions(player, reach)) for (var box : shape.toAabbs()) {
            double rise = box.maxY - player.getY();
            if (!box.intersects(reach) || rise < .25 || rise > 1.25) continue;
            var feet = player.position().add(0, rise, 0);
            if (!passageFits(player, feet, offset)) continue;
            preparePassage(player, direction, box.maxY);
            return true;
        }
        return false;
    }

    public static boolean passageJumpPending(Player player) {
        return player.getData(Content.CAT_SIZE).passageDirection != null;
    }

    public static boolean passageNeedsJump(Player player) {
        return passageJumpPending(player) && player.getData(Content.CAT_SIZE).passageRaised;
    }

    public static void cancelPassageJump(Player player) {
        var session = player.getData(Content.CAT_SIZE);
        boolean compressed = session.passageCompressed;
        session.passageDirection = null;
        session.passageCompressed = false;
        session.passageRaised = false;
        if (compressed) {
            var view = player.getData(Content.CAMERA_VIEW);
            accept(player, view.bodyEye < 0 ? player.getEyeHeight() : view.bodyEye, view.preference);
        }
    }

    private static void preparePassage(Player player, Vec3 direction, double ledge) {
        var session = player.getData(Content.CAT_SIZE);
        session.passageJumpUntil = player.tickCount + 12;
        session.passageLedge = ledge;
        session.passageDirection = direction.normalize();
        session.passageRaised = ledge > player.getY() + .001;
        double required = UPRIGHT_HEIGHT * player.getScale() + ledge - player.getY();
        session.passageCompressed = clearance(player, player.position(), required) < required - .001;
        if (session.passageCompressed) {
            var view = player.getData(Content.CAMERA_VIEW);
            accept(player, view.bodyEye < 0 ? player.getEyeHeight() : view.bodyEye, view.preference);
        }
    }

    private static float passageJumpEye(Player player) {
        if (CatForm.active(player)) return CatForm.STANDING_EYE_HEIGHT * player.getScale();
        double rise = player.onGround() ? player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower()
                : Math.max(0, player.getDeltaMovement().y);
        return (float)Math.max(.08, clearance(player, player.position(), UPRIGHT_HEIGHT * player.getScale())
                - rise - .2 * player.getScale());
    }

    private static boolean passageFits(Player player, Vec3 feet, Vec3 offset) {
        float height = CatForm.active(player) ? .7F : .6F;
        var low = EntityDimensions.scalable(player.getBbWidth(), height * player.getScale()).makeBoundingBox(feet);
        var standing = Player.STANDING_DIMENSIONS.scale(player.getScale()).makeBoundingBox(feet.add(offset));
        return player.level().noCollision(player, low.expandTowards(offset).deflate(1.0E-7))
                && player.level().getBlockCollisions(player, standing.deflate(1.0E-7)).iterator().hasNext();
    }

    public static boolean enterPassage(Player player) {
        var session = player.getData(Content.CAT_SIZE);
        if (session.passageDirection == null) return false;
        if (player.tickCount > session.passageJumpUntil || !ordinaryPose(player) || player.getAbilities().flying
                || player.getForcedPose() != null && !(session.prone && player.getForcedPose() == Pose.SWIMMING)
                || passage(player)) { cancelPassageJump(player); return false; }
        if (session.passageCompressed) {
            var view = player.getData(Content.CAMERA_VIEW);
            accept(player, view.bodyEye < 0 ? player.getEyeHeight() : view.bodyEye, view.preference);
        }
        if (player.getY() < session.passageLedge - .001
                || !session.passageRaised && !player.isShiftKeyDown()
                || !passageFits(player, player.position(), session.passageDirection.scale(.18 * player.getScale()))) return false;
        float eye = (float)Math.max(.08, clearance(player,
                player.position().add(session.passageDirection.scale(.18 * player.getScale())), UPRIGHT_HEIGHT * player.getScale())
                - (CatForm.active(player) ? .24F : .18F) * player.getScale());
        var view = player.getData(Content.CAMERA_VIEW);
        session.passageEye = eye;
        session.passage = true;
        session.passageUntil = player.tickCount + 10;
        cancelPassageJump(player);
        view.tick = player.tickCount;
        accept(player, view.bodyEye < 0 ? player.getEyeHeight() : view.bodyEye, view.preference);
        return true;
    }

    public static boolean freeProne(Player player) {
        return !CatForm.active(player) && player.getData(Content.CAT_SIZE).prone && player.isVisuallyCrawling() && standingRoom(player);
    }

    public static boolean cameraProne(Player player) {
        return player.getData(Content.CAT_SIZE).prone && player.getForcedPose() == Pose.SWIMMING
                || player.level().isClientSide && player.hasPose(Pose.SWIMMING) && player.hasData(Content.CAMERA_VIEW) && player.getData(Content.CAMERA_VIEW).prone;
    }

    public static double limitCatHeight(Player player, Vec3 feet, double height, int preference) {
        if (passage(player)) height = Math.min(height, player.getData(Content.CAT_SIZE).passageEye);
        double scale = player.getScale();
        double body = player.isCrouching() ? 1.45 : UPRIGHT_HEIGHT;
        double threshold = CameraGeometry.selected(QUADRUPED_MAX_CAMERA) * scale - CameraGeometry.crouchOffset(player, preference);
        boolean standing = preference > QUADRUPED_MAX_CAMERA && height > threshold + .0001;
        double room = clearance(player, feet, Math.max(height + .18 * scale, standing ? body * scale : 0));
        double limit = room - .18 * scale;
        if (standing && room < body * scale - .0001) limit = Math.min(limit, threshold - .01 * scale);
        return Math.max(.08, Math.min(height, limit));
    }

    public static EntityDimensions proneDimensions(Player player, EntityDimensions vanilla) {
        float eye = player.getData(Content.CAMERA_VIEW).bodyEye;
        if (passage(player)) eye = eye < 0 ? player.getData(Content.CAT_SIZE).passageEye
                : Math.min(eye, player.getData(Content.CAT_SIZE).passageEye);
        if (eye < 0) return vanilla;
        return vanilla.scale(1, Math.max(vanilla.height(), eye + .18F * player.getScale()) / vanilla.height()).withEyeHeight(eye);
    }

    public static float accept(Player player, float height, int preference) {
        prone(player, preference);
        var view = player.getData(Content.CAMERA_VIEW);
        boolean oldUpright = view.upright, oldProne = view.prone;
        float approved = height;
        if (player.getData(Content.CAT_SIZE).passageCompressed && passageJumpPending(player)) {
            approved = Math.min(approved, passageJumpEye(player));
        }
        if (CatForm.active(player)) {
            if (seated(player)) approved = .595F * player.getScale();
            else if (!standingCatPose(player)) approved = Math.min(approved, CatForm.STANDING_EYE_HEIGHT * player.getScale());
            approved = (float)limitCatHeight(player, player.position(), approved, preference);
            view.upright = !seated(player) && preference > QUADRUPED_MAX_CAMERA && (!standingCatPose(player)
                    || approved + CameraGeometry.crouchOffset(player, preference) > CameraGeometry.selected(QUADRUPED_MAX_CAMERA) * player.getScale() + .0001);
        } else if (cameraProne(player)) {
            if (passage(player)) approved = Math.min(approved, player.getData(Content.CAT_SIZE).passageEye);
            approved = Math.max(.08F, Math.min(approved, (float)clearance(player, player.position(), approved + .18 * player.getScale()) - .18F * player.getScale()));
        } else if (player.isVisuallyCrawling()) approved = Math.min(approved, player.getEyeHeight());
        float old = view.bodyEye;
        view.prone = player.getData(Content.CAT_SIZE).prone;
        if (!CatForm.active(player)) view.upright = false;
        view.bodyEye = approved;
        refresh(player);
        if (player instanceof ServerPlayer && (Math.abs(old - approved) > .002F || oldUpright != view.upright || oldProne != view.prone)) player.syncData(Content.CAMERA_VIEW);
        return approved;
    }

    public static void refresh(Player player) {
        var session = player.getData(Content.CAT_SIZE);
        boolean cat = CatForm.active(player);
        float eye = cat ? catEye(player) / player.getScale() : cameraProne(player) ? player.getData(Content.CAMERA_VIEW).bodyEye / player.getScale() : -1;
        boolean upright = upright(player);
        if (session.cat != cat || session.upright != upright || Math.abs(session.eye - eye) > .002F) {
            session.cat = cat;
            session.upright = upright;
            session.eye = eye;
            player.refreshDimensions();
        }
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) enterPassage(player);
        if (passage(player)) prone(player, player.getData(Content.CAMERA_VIEW).preference);
        if (player instanceof ServerPlayer && player.hasData(Content.CAMERA_VIEW)) {
            var view = player.getData(Content.CAMERA_VIEW);
            long age = (long)player.tickCount - view.tick;
            if (age >= 0 && age <= 40) accept(player, view.height, view.preference);
            else if (view.bodyEye >= 0) {
                view.bodyEye = -1;
                view.upright = view.prone = false;
                prone(player, 100);
                refresh(player);
                player.syncData(Content.CAMERA_VIEW);
            }
        } else if (player.level().isClientSide) refresh(player);
    }

    private CameraPosture() {}
}
