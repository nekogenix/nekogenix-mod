package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatForm {
    public static final float STANDING_EYE_HEIGHT = .43F;
    public static final float CROUCHING_EYE_HEIGHT = .35F;

    public static final class Session {
        boolean cat, prone, upright, passage, passageCompressed, passageRaised;
        int passageUntil;
        int passageJumpUntil;
        double passageLedge;
        float passageEye;
        net.minecraft.world.phys.Vec3 passageDirection;
        float eye = -1;
    }

    public static boolean active(Player player) {
        return player.level().isClientSide ? player.getData(Content.CAT_FORM) : player.getData(Content.NEKO).isCat();
    }

    public static EntityDimensions dimensions(Pose pose) {
        return EntityType.CAT.getDimensions().withEyeHeight(pose == Pose.CROUCHING
                ? CROUCHING_EYE_HEIGHT : STANDING_EYE_HEIGHT);
    }

    public static void sync(ServerPlayer player) {
        boolean cat = player.getData(Content.NEKO).isCat();
        if (player.getData(Content.CAT_FORM) != cat) player.setData(Content.CAT_FORM, cat);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) sync(serverPlayer);
        CameraPosture.refresh(player);
    }

    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && player.isAddedToLevel() && active(player)) {
            event.setNewSize(CameraPosture.catDimensions(player, event.getPose()).scale(player.getScale()));
        } else if (event.getEntity() instanceof Player player && player.isAddedToLevel() && CameraPosture.cameraProne(player)) {
            event.setNewSize(CameraPosture.proneDimensions(player, event.getNewSize()));
        }
    }

    private CatForm() {}
}
