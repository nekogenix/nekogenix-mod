package io.github.nekogenix.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NekogenixCommands {
    private static final int ALL_ABILITIES = NekoState.AGILITY | NekoState.VISION | NekoState.PAWS;
    private static final DynamicCommandExceptionType INVALID_PHASE = new DynamicCommandExceptionType(
            value -> Component.translatableEscape("commands.nekogenix.invalid_phase", value));
    private static final DynamicCommandExceptionType INVALID_ABILITY = new DynamicCommandExceptionType(
            value -> Component.translatableEscape("commands.nekogenix.invalid_ability", value));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nekogenix")
                .requires(NekogenixPermissions::allowedAny)
                .executes(context -> help(context.getSource()))
                .then(Commands.literal("help")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.HELP))
                        .executes(context -> help(context.getSource())))
                .then(Commands.literal("status")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.STATUS))
                        .executes(context -> status(context.getSource().getPlayerOrException())))
                .then(Commands.literal("phase")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.PHASE_SET))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("phase", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(NekoState.Phase.values()).map(NekogenixCommands::name), builder))
                                                .executes(context -> phase(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "phase")))))))
                .then(Commands.literal("progress")
                        .requires(source -> NekogenixPermissions.allowedAny(source,
                                NekogenixPermissions.PROGRESS_SET, NekogenixPermissions.PROGRESS_NEXT))
                        .then(Commands.literal("set")
                                .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.PROGRESS_SET))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 72_000))
                                                .executes(context -> progress(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "ticks"))))))
                        .then(Commands.literal("next")
                                .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.PROGRESS_NEXT))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> next(context.getSource(), EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("ability")
                        .requires(source -> NekogenixPermissions.allowedAny(source,
                                NekogenixPermissions.ABILITY_GRANT, NekogenixPermissions.ABILITY_REVOKE))
                        .then(abilityAction("grant", true, NekogenixPermissions.ABILITY_GRANT))
                        .then(abilityAction("revoke", false, NekogenixPermissions.ABILITY_REVOKE)))
                .then(Commands.literal("lives")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.LIVES_SET))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("lives", IntegerArgumentType.integer(1, 9))
                                                .executes(context -> lives(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "lives")))))))
                .then(Commands.literal("accelerate")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.ACCELERATE))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> accelerate(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                BoolArgumentType.getBool(context, "enabled"))))))
                .then(Commands.literal("stabilize")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.STABILIZE))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> stabilize(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("reset")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.RESET))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("kit")
                        .requires(source -> NekogenixPermissions.allowed(source, NekogenixPermissions.KIT))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> kit(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<net.minecraft.commands.CommandSourceStack, ?> abilityAction(
            String action, boolean enabled, net.neoforged.neoforge.server.permission.nodes.PermissionNode<Boolean> permission) {
        return Commands.literal(action)
                .requires(source -> NekogenixPermissions.allowed(source, permission))
                .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                new String[] {"agility", "vision", "paws", "all"}, builder))
                        .executes(context -> ability(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                StringArgumentType.getString(context, "ability"), enabled))));
    }

    private static int help(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.nekogenix.help"), false);
        return 1;
    }

    private static int status(ServerPlayer player) {
        var state = player.getData(Content.NEKO);
        String abilities = abilities(state);
        player.sendSystemMessage(Component.translatable("commands.nekogenix.status", name(state.phase()), state.elapsed(),
                abilities, state.lives(), state.accelerated()));
        return 1;
    }

    private static int phase(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets, String value)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        NekoState.Phase phase;
        try { phase = NekoState.Phase.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw INVALID_PHASE.create(value); }
        for (var player : targets) {
            player.getData(Content.NEKO).setPhase(phase);
            refresh(player);
        }
        return changed(source, targets, "commands.nekogenix.phase", name(phase));
    }

    private static int progress(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets, int ticks) {
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).setElapsed(ticks)) changed++;
            refresh(player);
        }
        return result(source, changed, "commands.nekogenix.progress", ticks);
    }

    private static int next(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets) {
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).advance(player.getRandom())) changed++;
            refresh(player);
        }
        return result(source, changed, "commands.nekogenix.next");
    }

    private static int ability(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets,
            String value, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int ability = switch (value.toLowerCase(Locale.ROOT)) {
            case "agility" -> NekoState.AGILITY;
            case "vision" -> NekoState.VISION;
            case "paws" -> NekoState.PAWS;
            case "all" -> ALL_ABILITIES;
            default -> throw INVALID_ABILITY.create(value);
        };
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).setAbility(ability, enabled)) changed++;
            refresh(player);
        }
        return result(source, changed, enabled ? "commands.nekogenix.ability_granted" : "commands.nekogenix.ability_revoked", value);
    }

    private static int lives(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets, int lives) {
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).setLives(lives)) changed++;
            refresh(player);
        }
        return result(source, changed, "commands.nekogenix.lives", lives);
    }

    private static int accelerate(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).setAccelerated(enabled)) changed++;
            refresh(player);
        }
        return result(source, changed, "commands.nekogenix.accelerated", enabled);
    }

    private static int stabilize(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets) {
        int changed = 0;
        for (var player : targets) {
            if (player.getData(Content.NEKO).stabilize()) changed++;
            refresh(player);
        }
        return result(source, changed, "commands.nekogenix.stabilized");
    }

    private static int reset(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (var player : targets) {
            player.getData(Content.NEKO).reset();
            refresh(player);
        }
        return changed(source, targets, "commands.nekogenix.reset");
    }

    private static int kit(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (var player : targets) {
            for (var item : new net.minecraft.world.level.ItemLike[] {Content.INJECTION.get(), Content.STABILIZING_INJECTION.get(),
                    Content.NEKO_STABILIZER.get(), Content.SALIVA_BOTTLE.get(), Content.SPLASH_SALIVA.get(),
                    Content.LINGERING_SALIVA.get(), Content.BLANK_NUCLEOCARD.get(), Content.CAT_NUCLEOCARD.get(),
                    Content.SYRINGE.get(), Content.USED_SYRINGE.get(), Content.CENTRIFUGE_ITEM.get(), Content.PNEUMATIC_ITEM.get()}) {
                ItemStack stack = new ItemStack(item);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
        return changed(source, targets, "commands.nekogenix.kit");
    }

    private static void refresh(ServerPlayer player) {
        TransformationHandler.refreshEffects(player);
        player.refreshDimensions();
    }

    private static int changed(net.minecraft.commands.CommandSourceStack source, Collection<ServerPlayer> targets,
            String key, Object... arguments) {
        source.sendSuccess(() -> Component.translatable(key, targets.size(), arguments.length == 0 ? "" : arguments[0]), true);
        return targets.size();
    }

    private static int result(net.minecraft.commands.CommandSourceStack source, int changed, String key, Object... arguments) {
        if (changed == 0) {
            source.sendFailure(Component.translatable("commands.nekogenix.no_changes"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(key, changed, arguments.length == 0 ? "" : arguments[0]), true);
        return changed;
    }

    private static String abilities(NekoState state) {
        var names = new java.util.ArrayList<String>();
        if (state.has(NekoState.AGILITY)) names.add("agility");
        if (state.has(NekoState.VISION)) names.add("vision");
        if (state.has(NekoState.PAWS)) names.add("paws");
        return names.isEmpty() ? "none" : String.join(", ", names);
    }

    private static String name(NekoState.Phase phase) {
        return phase.name().toLowerCase(Locale.ROOT);
    }

    private NekogenixCommands() {}
}
