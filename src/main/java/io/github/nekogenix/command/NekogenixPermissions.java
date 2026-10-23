package io.github.nekogenix.command;

import io.github.nekogenix.Nekogenix;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.handler.DefaultPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NekogenixPermissions {
    public static final PermissionNode<Boolean> HELP = command("help");
    public static final PermissionNode<Boolean> STATUS = command("status");
    public static final PermissionNode<Boolean> PHASE_SET = command("phase.set");
    public static final PermissionNode<Boolean> PROGRESS_SET = command("progress.set");
    public static final PermissionNode<Boolean> PROGRESS_NEXT = command("progress.next");
    public static final PermissionNode<Boolean> ABILITY_GRANT = command("ability.grant");
    public static final PermissionNode<Boolean> ABILITY_REVOKE = command("ability.revoke");
    public static final PermissionNode<Boolean> LIVES_SET = command("lives.set");
    public static final PermissionNode<Boolean> ACCELERATE = command("accelerate");
    public static final PermissionNode<Boolean> STABILIZE = command("stabilize");
    public static final PermissionNode<Boolean> RESET = command("reset");
    public static final PermissionNode<Boolean> KIT = command("kit");

    public static final List<PermissionNode<Boolean>> ALL = List.of(HELP, STATUS, PHASE_SET, PROGRESS_SET,
            PROGRESS_NEXT, ABILITY_GRANT, ABILITY_REVOKE, LIVES_SET, ACCELERATE, STABILIZE, RESET, KIT);

    private static PermissionNode<Boolean> command(String path) {
        return new PermissionNode<>(Nekogenix.ID, "command." + path, PermissionTypes.BOOLEAN,
                (player, uuid, context) -> player != null && player.createCommandSourceStack().hasPermission(2));
    }

    @SubscribeEvent
    public static void register(PermissionGatherEvent.Nodes event) {
        for (var node : ALL) event.addNodes(node);
    }

    public static boolean allowed(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (!(source.getEntity() instanceof ServerPlayer player) || usesVanillaDefaults()) {
            return source.hasPermission(2);
        }
        return PermissionAPI.getPermission(player, node);
    }

    @SafeVarargs
    public static boolean allowedAny(CommandSourceStack source, PermissionNode<Boolean>... nodes) {
        if (!(source.getEntity() instanceof ServerPlayer) || usesVanillaDefaults()) return source.hasPermission(2);
        for (var node : nodes) if (allowed(source, node)) return true;
        return false;
    }

    public static boolean allowedAny(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || usesVanillaDefaults()) return source.hasPermission(2);
        for (var node : ALL) if (PermissionAPI.getPermission(player, node)) return true;
        return false;
    }

    private static boolean usesVanillaDefaults() {
        var handler = PermissionAPI.getActivePermissionHandler();
        return handler == null || handler.equals(DefaultPermissionHandler.IDENTIFIER);
    }

    private NekogenixPermissions() {}
}
