package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.state.NekoState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class FelineVisionControl {
    public static final class Session {
        private boolean automatic = true;
        private boolean enabled, changed;
    }

    public static void receive(Player player, boolean automatic, boolean enabled) {
        if (!(player instanceof ServerPlayer)) return;
        Session session = player.getData(Content.VISION_CONTROL);
        enabled = !automatic && enabled && player.isAlive() && player.getData(Content.NEKO).has(NekoState.VISION);
        if (session.automatic == automatic && session.enabled == enabled) return;
        session.automatic = automatic;
        session.enabled = enabled;
        session.changed = true;
    }

    public static boolean active(ServerPlayer player) {
        Session session = player.getData(Content.VISION_CONTROL);
        return session.automatic ? TransformationHandler.inDarkness(player) : session.enabled;
    }

    static boolean changed(ServerPlayer player) {
        if (!player.hasData(Content.VISION_CONTROL)) return false;
        Session session = player.getData(Content.VISION_CONTROL);
        boolean changed = session.changed;
        session.changed = false;
        return changed;
    }

    private FelineVisionControl() {}
}
