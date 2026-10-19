package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatForm {
    public static final class Session {
        private boolean cat;
    }

    public static boolean active(Player player) {
        return player.level().isClientSide ? player.getData(Content.CAT_FORM) : player.getData(Content.NEKO).isCat();
    }

    public static void sync(ServerPlayer player) {
        boolean cat = player.getData(Content.NEKO).isCat();
        if (player.getData(Content.CAT_FORM) != cat) player.setData(Content.CAT_FORM, cat);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) sync(serverPlayer);
        boolean cat = active(player);
        Session session = player.getData(Content.CAT_SIZE);
        if (session.cat != cat) {
            session.cat = cat;
            player.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && player.isAddedToLevel() && active(player)) {
            event.setNewSize(EntityType.CAT.getDimensions().scale(player.getScale()));
        }
    }

    private CatForm() {}
}
