package io.github.nekogenix;

import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class AdvancementTests {
    @GameTest(template = "empty", timeoutTicks = 210)
    public static void nekoInjectionsAwardVanillaProgressFromEitherHandAndPairedDoses(GameTestHelper helper) {
        var right = GameplayTests.player(helper);
        var left = GameplayTests.player(helper);
        var paired = GameplayTests.player(helper);
        var observer = GameplayTests.player(helper);
        right.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        left.setItemInHand(InteractionHand.MAIN_HAND, Items.SHIELD.getDefaultInstance());
        left.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        paired.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        paired.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        helper.assertTrue(right.server.getAdvancements().getAllAdvancements().stream()
                .filter(advancement -> advancement.id().getNamespace().equals(Nekogenix.ID)
                        && advancement.value().display().isPresent() && !advancement.id().equals(Content.id("root"))).count() == 4,
                "The mod adds four playable advancements plus its silent tab root");
        for (int tick : new int[] {65, 85, 105, 125, 145}) helper.runAfterDelay(tick, () -> {
            InjectionHandler.input(right, true, false);
            InjectionHandler.input(left, false, true);
            InjectionHandler.input(paired, true, true);
        });
        helper.runAfterDelay(150, () -> {
            for (var player : new ServerPlayer[] {right, left, paired}) {
                helper.assertFalse(done(player, "what_could_go_wrong"), "Possession and an unfinished injection cannot award the advancement");
                helper.assertFalse(done(player, "dual_injection"), "Both doses must finish before awarding the paired advancement");
            }
        });
        helper.runAfterDelay(165, () -> {
            for (var player : new ServerPlayer[] {right, left, paired}) {
                helper.assertTrue(done(player, "what_could_go_wrong") == (player != paired), "The single-dose advancement is exclusive to a session with one completed dose");
                helper.assertFalse(done(player, "weeb"), "A Neko Injection cannot grant the stabilizing advancement");
                helper.assertTrue(done(player, "dual_injection") == (player == paired), "Only a completed pair awards the dual injection advancement");
                synchronizes(helper, player, player == paired ? "dual_injection" : "what_could_go_wrong");
                var advancement = advancement(player, player == paired ? "dual_injection" : "what_could_go_wrong");
                String criterion = player == paired ? "both_doses" : "injected";
                var obtained = player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterion).getObtained();
                CriteriaTriggers.CONSUME_ITEM.trigger(player, Content.INJECTION.toStack());
                helper.assertTrue(obtained.equals(player.getAdvancements().getOrStartProgress(advancement).getCriterion(criterion).getObtained()),
                        "Repeated doses cannot replace the first completion timestamp");
                helper.assertTrue(done(player, "dual_injection") == (player == paired), "Repeated consumption notifications cannot substitute for a completed pair");
                helper.assertTrue(done(player, "what_could_go_wrong") == (player != paired), "Consumption notifications cannot grant a second advancement for the same session");
                if (player == paired) {
                    player.getAdvancements().save();
                    player.getAdvancements().reload(player.server.getAdvancements());
                    helper.assertTrue(done(player, "dual_injection"), "Paired injection progress persists through vanilla save and reload");
                }
                GameplayTests.remove(player);
            }
            helper.assertFalse(done(observer, "what_could_go_wrong") || done(observer, "weeb") || done(observer, "dual_injection"), "Advancements belong to the injecting player only");
            GameplayTests.remove(observer);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 210)
    public static void aLateCancelledSecondDoseAwardsOnlyTheSingleDose(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        player.setItemInHand(InteractionHand.OFF_HAND, Content.INJECTION.toStack());
        for (int tick : new int[] {65, 85, 105, 125, 145}) helper.runAfterDelay(tick,
                () -> InjectionHandler.input(player, true, tick >= 85));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(player.getMainHandItem().is(Content.USED_SYRINGE), "The first hand has already completed its dose");
            helper.assertFalse(done(player, "what_could_go_wrong") || done(player, "dual_injection"),
                    "Awarding waits until the second hand has resolved");
            InjectionHandler.input(player, false, false);
        });
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(done(player, "what_could_go_wrong") && !done(player, "dual_injection"),
                    "Cancelling the remaining hand after the first dose awards only the single-dose advancement");
            helper.assertTrue(player.getOffhandItem().is(Content.INJECTION), "The interrupted partner remains filled");
            synchronizes(helper, player, "what_could_go_wrong");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 210)
    public static void cancelledAndFailedInjectionsDoNotGrantAdvancements(GameTestHelper helper) {
        var cancelled = GameplayTests.player(helper);
        var cancelledStabilizer = GameplayTests.player(helper);
        var failed = GameplayTests.player(helper);
        var failedStabilizer = GameplayTests.player(helper);
        var players = new ServerPlayer[] {cancelled, cancelledStabilizer, failed, failedStabilizer};
        for (var player : players) {
            boolean stabilizing = player == cancelledStabilizer || player == failedStabilizer;
            player.setItemInHand(InteractionHand.MAIN_HAND, stabilizing ? Content.STABILIZING_INJECTION.toStack() : Content.INJECTION.toStack());
            if (stabilizing) player.getData(Content.NEKO).setPhase(NekoState.Phase.CAT_TRANSFORMING);
        }
        for (int tick : new int[] {65, 85, 105, 125, 145}) helper.runAfterDelay(tick, () -> {
            for (var player : players) {
                if (tick < 105 || player == failed || player == failedStabilizer) InjectionHandler.input(player, true);
            }
        });
        helper.runAfterDelay(100, () -> {
            InjectionHandler.input(cancelled, false);
            InjectionHandler.input(cancelledStabilizer, false);
            failed.getData(Content.NEKO).setPhase(NekoState.Phase.NEKO);
            failedStabilizer.getData(Content.NEKO).setPhase(NekoState.Phase.CAT);
        });
        helper.runAfterDelay(165, () -> {
            for (var player : players) {
                helper.assertFalse(done(player, "what_could_go_wrong") || done(player, "weeb"),
                        "Cancelled and rejected completions cannot award either advancement");
                helper.assertTrue(InjectionHandler.isInjection(player.getMainHandItem()), "A cancelled or failed injection is not consumed");
                GameplayTests.remove(player);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 210)
    public static void stoppingCatTransformationAwardsAndPersistsWeeb(GameTestHelper helper) {
        var right = GameplayTests.player(helper);
        var left = GameplayTests.player(helper);
        var commandOnly = GameplayTests.player(helper);
        for (var phase : NekoState.Phase.values()) {
            if (phase == NekoState.Phase.CAT_TRANSFORMING) continue;
            right.getData(Content.NEKO).setPhase(phase);
            right.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
            InjectionHandler.input(right, true);
            helper.assertFalse(InjectionHandler.active(right, InteractionHand.MAIN_HAND) || done(right, "weeb"),
                    "A stabilizer cannot be started or earn its advancement outside Cat Transformation");
            InjectionHandler.input(right, false);
        }
        commandOnly.getData(Content.NEKO).setPhase(NekoState.Phase.CAT_TRANSFORMING);
        commandOnly.getData(Content.NEKO).stabilize();
        CriteriaTriggers.CONSUME_ITEM.trigger(commandOnly, Items.APPLE.getDefaultInstance());
        helper.assertFalse(done(commandOnly, "weeb"), "A command-like phase change and unrelated consumption cannot substitute for an injection");
        for (var player : new ServerPlayer[] {right, left}) player.getData(Content.NEKO).setPhase(NekoState.Phase.CAT_TRANSFORMING);
        left.setItemInHand(InteractionHand.MAIN_HAND, Items.SHIELD.getDefaultInstance());
        left.setItemInHand(InteractionHand.OFF_HAND, Content.STABILIZING_INJECTION.toStack());
        for (int tick : new int[] {65, 85, 105, 125, 145}) helper.runAfterDelay(tick, () -> {
            InjectionHandler.input(right, true, false);
            InjectionHandler.input(left, false, true);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertFalse(done(right, "weeb") || done(left, "weeb"), "Starting the injection or piercing the skin does not yet stop transformation");
        });
        helper.runAfterDelay(165, () -> {
            for (var player : new ServerPlayer[] {right, left}) {
                helper.assertTrue(player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO && done(player, "weeb"),
                        "Successful stabilization awards Weeb from either hand");
                helper.assertFalse(done(player, "dual_injection"), "Weeb remains obtainable without the optional paired injection");
                helper.assertFalse(done(player, "what_could_go_wrong"), "Weeb does not fabricate a prior Neko Injection on an existing save");
                synchronizes(helper, player, "weeb");
                player.getData(Content.NEKO).setPhase(NekoState.Phase.HUMAN);
                player.getAdvancements().save();
                player.getAdvancements().reload(player.server.getAdvancements());
                helper.assertTrue(done(player, "weeb"), "Vanilla saving and loading preserve earned progress after changing back to human");
                GameplayTests.remove(player);
            }
            GameplayTests.remove(commandOnly);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void tabUsesTheModNameAndTheTwoEndingsAreSeparateBranches(GameTestHelper helper) {
        var player = GameplayTests.player(helper);
        var root = advancement(player, "root").value();
        var display = root.display().orElseThrow();
        helper.assertTrue(root.parent().isEmpty() && display.getTitle().getString().equals("Nekogenix"),
                "The root title used by vanilla and replacement screens is the mod name");
        helper.assertFalse(display.shouldShowToast() || display.shouldAnnounceChat(), "The tab root cannot produce a completion notification or chat announcement");
        helper.assertTrue(display.getIcon().is(Content.INJECTION) && display.getIcon().get(DataComponents.CUSTOM_MODEL_DATA).value() == 2,
                "The root and its tab use the mod logo through vanilla model data");
        var injectionIcon = advancement(player, "what_could_go_wrong").value().display().orElseThrow().getIcon();
        helper.assertTrue(injectionIcon.is(Content.INJECTION) && !injectionIcon.has(DataComponents.CUSTOM_MODEL_DATA),
                "The first injection achievement retains the ordinary syringe icon");
        helper.assertTrue(advancement(player, "what_could_go_wrong").value().parent().orElseThrow().equals(Content.id("root")),
                "The first injection sits under the mod tab root");
        helper.assertTrue(advancement(player, "dual_injection").value().parent().orElseThrow().equals(Content.id("what_could_go_wrong")),
                "The extra paired injection connects directly to the first injection");
        var first = advancement(player, "what_could_go_wrong").value().display().orElseThrow();
        var paired = advancement(player, "dual_injection").value().display().orElseThrow();
        helper.assertTrue(first.getX() == paired.getX() && paired.getY() < first.getY(),
                "The paired injection is above the first injection in the same column");
        for (var name : new String[] {"weeb", "finish_what_you_started"}) {
            helper.assertTrue(advancement(player, name).value().parent().orElseThrow().equals(Content.id("what_could_go_wrong")),
                    "Both endings branch directly from the first injection instead of depending on each other");
            var ending = advancement(player, name).value().display().orElseThrow();
            helper.assertTrue(ending.getX() > first.getX(), "The existing outcomes sit beyond the injection choice");
        }
        var catIcon = advancement(player, "finish_what_you_started").value().display().orElseThrow().getIcon();
        helper.assertTrue(catIcon.is(Content.INJECTION) && catIcon.get(DataComponents.CUSTOM_MODEL_DATA).value() == 1,
                "The cat artwork uses the standard model-data component on its advancement icon");
        helper.assertFalse(Content.INJECTION.toStack().has(DataComponents.CUSTOM_MODEL_DATA),
                "Ordinary Neko Injections keep their normal model and injection animations");
        var manager = player.server.getAdvancements();
        var vanilla = java.util.Objects.requireNonNull(manager.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("story/root")))
                .value().display().orElseThrow();
        float vanillaX = vanilla.getX(), vanillaY = vanilla.getY();
        net.minecraft.advancements.TreeNodePosition.run(java.util.Objects.requireNonNull(manager.tree().get(Content.id("root"))));
        var sync = new net.neoforged.neoforge.event.OnDatapackSyncEvent(player.server.getPlayerList(), null);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(sync);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(sync);
        helper.assertTrue(first.getX() == 1.5F && first.getY() == 1.5F && paired.getX() == 1.5F && paired.getY() == 0,
                "Datapack synchronization restores the layout after vanilla recalculation and repeated synchronization cannot move it");
        helper.assertTrue(vanilla.getX() == vanillaX && vanilla.getY() == vanillaY, "Other advancement tabs keep their original positions");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(done(player, "root"), "The mod tab opens through the standard vanilla tick criterion");
            helper.assertFalse(done(player, "what_could_go_wrong") || done(player, "weeb") || done(player, "finish_what_you_started") || done(player, "dual_injection"),
                    "Opening the tab does not grant its playable achievements");
            synchronizes(helper, player, "root");
            GameplayTests.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void completingTheLastCatStageAwardsOnlyTheFullTransformationBranch(GameTestHelper helper) {
        for (boolean accelerated : new boolean[] {false, true}) {
            var player = GameplayTests.player(helper);
            var state = player.getData(Content.NEKO);
            state.setPhase(NekoState.Phase.CAT_TRANSFORMING);
            state.setAccelerated(accelerated);
            state.setElapsed(state.stageDuration() - 2);
            TransformationHandler.tick(new PlayerTickEvent.Post(player));
            helper.assertFalse(done(player, "finish_what_you_started"), "The penultimate tick of either final stage cannot award completion early");
            TransformationHandler.tick(new PlayerTickEvent.Post(player));
            helper.assertTrue(state.isCat() && done(player, "finish_what_you_started"),
                    "Both regular and accelerated final-stage completion award the cat ending");
            helper.assertFalse(done(player, "dual_injection"), "The cat ending does not require paired doses or award them from an admin acceleration flag");
            helper.assertFalse(done(player, "weeb"), "Completing the cat stage cannot award its alternative stabilized ending");
            synchronizes(helper, player, "finish_what_you_started");
            state.setPhase(NekoState.Phase.HUMAN);
            player.getAdvancements().save();
            player.getAdvancements().reload(player.server.getAdvancements());
            helper.assertTrue(done(player, "finish_what_you_started"), "The full transformation ending persists through vanilla saving and reloading");
            GameplayTests.remove(player);
        }
        var player = GameplayTests.player(helper);
        var state = player.getData(Content.NEKO);
        state.setPhase(NekoState.Phase.CAT_TRANSFORMING);
        state.stabilize();
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        helper.assertFalse(done(player, "finish_what_you_started"), "Stopping before becoming a cat cannot award the completed-transformation ending");
        state.setPhase(NekoState.Phase.CAT);
        TransformationHandler.tick(new PlayerTickEvent.Post(player));
        helper.assertFalse(done(player, "finish_what_you_started"), "An existing cat state or admin phase assignment does not pretend a natural final-stage completion occurred");
        GameplayTests.remove(player);
        helper.succeed();
    }

    private static AdvancementHolder advancement(ServerPlayer player, String name) {
        return java.util.Objects.requireNonNull(player.server.getAdvancements().get(Content.id(name)), "Missing advancement: " + name);
    }

    private static boolean done(ServerPlayer player, String name) {
        return player.getAdvancements().getOrStartProgress(advancement(player, name)).isDone();
    }

    private static void synchronizes(GameTestHelper helper, ServerPlayer player, String name) {
        player.getAdvancements().flushDirty(player);
        var channel = (EmbeddedChannel)player.connection.getConnection().channel();
        channel.runPendingTasks();
        boolean synchronizedProgress = false;
        boolean synchronizedDisplay = false;
        Object message;
        while ((message = channel.readOutbound()) != null) {
            if (message instanceof ClientboundUpdateAdvancementsPacket packet) {
                var progress = packet.getProgress().get(Content.id(name));
                synchronizedProgress |= progress != null && progress.isDone();
                synchronizedDisplay |= packet.getAdded().stream().anyMatch(added -> added.id().equals(Content.id(name)) && added.value().display().isPresent());
                if (name.equals("dual_injection")) for (var added : packet.getAdded()) if (added.id().equals(Content.id(name))) {
                    var display = added.value().display().orElseThrow();
                    helper.assertTrue(display.getX() == 1.5F && display.getY() == 0,
                            "The standard advancement packet sends the paired injection above the first injection");
                }
            }
            ReferenceCountUtil.release(message);
        }
        helper.assertTrue(synchronizedProgress && synchronizedDisplay,
                "The standard advancement packet carries the display and completed progress used by vanilla and replacement advancement screens");
    }
}
