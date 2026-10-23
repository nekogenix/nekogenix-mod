package io.github.nekogenix;

import io.github.nekogenix.command.NekogenixPermissions;
import io.github.nekogenix.state.NekoState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class AdminCommandsTests {
    @GameTest(template = "empty")
    public static void commandPermissionsAreRegisteredAndKeepVanillaFallback(GameTestHelper helper) {
        var registered = net.neoforged.neoforge.server.permission.PermissionAPI.getRegisteredNodes();
        helper.assertTrue(registered.containsAll(NekogenixPermissions.ALL),
                "Every documented Nekogenix command permission is registered with NeoForge");

        var player = GameplayTests.player(helper);
        var root = player.server.getCommands().getDispatcher().getRoot().getChild("nekogenix");
        helper.assertTrue(root != null, "The /nekogenix command root is registered");
        helper.assertTrue(!root.canUse(player.createCommandSourceStack().withPermission(1)),
                "Vanilla non-operators cannot use admin commands without an external permission handler");
        helper.assertTrue(root.canUse(player.createCommandSourceStack().withPermission(2)),
                "Vanilla level-two operators retain access without an external permission handler");
        helper.assertTrue(root.getChild("help") != null,
                "The documented help permission has a matching command literal");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandsChangeAndSynchronizeState(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var source = player.createCommandSourceStack().withPermission(4);
        var commands = player.server.getCommands();

        commands.performPrefixedCommand(source, "nekogenix status");
        helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.HUMAN,
                "Status works without an explicit @s target and does not alter state");
        commands.performPrefixedCommand(source, "nekogenix phase set @s cat");
        var state = player.getData(Content.NEKO);
        helper.assertTrue(state.phase() == NekoState.Phase.CAT && state.has(7) && state.lives() == 9
                && player.getData(Content.CAT_FORM), "Cat phase creates a complete synchronized cat state");

        commands.performPrefixedCommand(source, "nekogenix ability revoke @s vision");
        commands.performPrefixedCommand(source, "nekogenix lives set @s 3");
        helper.assertTrue(!state.has(NekoState.VISION) && state.lives() == 3 && !player.hasEffect(Content.VISION),
                "Ability and lives commands update derived effects and persistent state");

        commands.performPrefixedCommand(source, "nekogenix phase set @s shock");
        commands.performPrefixedCommand(source, "nekogenix accelerate @s true");
        commands.performPrefixedCommand(source, "nekogenix progress set @s 100");
        commands.performPrefixedCommand(source, "nekogenix progress next @s");
        helper.assertTrue(state.phase() == NekoState.Phase.TRANSFORMING && state.elapsed() == 0 && state.accelerated(),
                "Advancing from shock preserves the accelerated transformation mode");

        commands.performPrefixedCommand(source, "nekogenix phase set @s cat_transforming");
        commands.performPrefixedCommand(source, "nekogenix stabilize @s");
        helper.assertTrue(state.phase() == NekoState.Phase.NEKO && !player.getData(Content.CAT_FORM),
                "Stabilize command returns Cat Transformation to synchronized neko form");

        commands.performPrefixedCommand(source, "nekogenix reset @s");
        helper.assertTrue(state.phase() == NekoState.Phase.HUMAN && !state.has(7) && state.lives() == 0,
                "Reset command restores the canonical human state");

        commands.performPrefixedCommand(source, "nekogenix kit @s");
        helper.assertTrue(player.getInventory().contains(Content.INJECTION.toStack())
                && player.getInventory().contains(Content.STABILIZING_INJECTION.toStack())
                && player.getInventory().contains(Content.CENTRIFUGE_ITEM.toStack()),
                "Test kit supplies transformation items and machines");
        GameplayTests.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void administrativeStateMutationsStayValid(GameTestHelper helper) {
        var state = new NekoState();
        state.setPhase(NekoState.Phase.CAT);
        helper.assertTrue(state.isCat() && state.has(7) && state.lives() == 9,
                "Direct cat selection produces all abilities and nine lives");
        state.setLives(0);
        helper.assertTrue(state.lives() == 1, "Admin life count cannot create a broken zero-life feline");
        state.setPhase(NekoState.Phase.TRANSFORMING);
        state.setAccelerated(true);
        state.setElapsed(Integer.MAX_VALUE);
        helper.assertTrue(state.elapsed() == 3 * state.stageDuration() - 1,
                "Progress is clamped before the next automatic phase boundary");
        state.advance(helper.getLevel().random);
        helper.assertTrue(state.phase() == NekoState.Phase.CAT_TRANSFORMING && state.has(7) && state.lives() == 9,
                "Advancing at the final boundary enters Cat Transformation safely");
        helper.succeed();
    }

    private AdminCommandsTests() {}
}
