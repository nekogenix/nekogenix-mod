package io.github.nekogenix.integration.sable;

import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Vector3d;

public final class FelineOrientation {
    public static Vec3 up(Player player) {
        return ModList.get().isLoaded("sable") ? Sable.up(player) : new Vec3(0, 1, 0);
    }

    private static final class Sable {
        private static Vec3 up(Player player) {
            var orientation = EntitySubLevelUtil.getCustomEntityOrientation(player, 1);
            if (orientation == null) return new Vec3(0, 1, 0);
            var up = orientation.transform(new Vector3d(0, 1, 0));
            return new Vec3(up.x, up.y, up.z).normalize();
        }
    }

    private FelineOrientation() {}
}
