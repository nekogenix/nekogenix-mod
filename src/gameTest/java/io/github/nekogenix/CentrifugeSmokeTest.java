package io.github.nekogenix;

import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.centrifuge.CentrifugeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Opt-in centrifuge client and server check, excluded from the released jar. */
@EventBusSubscriber(modid = Nekogenix.ID)
public final class CentrifugeSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.centrifugeSmokeTest");
    private static final BlockPos MACHINE = new BlockPos(8, -60, 8);
    private static final BlockPos BREWER = new BlockPos(10, -60, 8);

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var level = player.serverLevel();
        if (player.tickCount == 40) {
            level.setDayTime(6000);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, player.server);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 8.5, -58.7, 6.2, 0, 43);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (level.getBlockEntity(MACHINE) instanceof CentrifugeBlockEntity previous) previous.clearContent();
            level.setBlockAndUpdate(MACHINE, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(MACHINE.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(MACHINE, Content.CENTRIFUGE.get().defaultBlockState());
            var machine = (CentrifugeBlockEntity) level.getBlockEntity(MACHINE);
            for (int i : new int[] {0, 2, 4}) machine.setItem(i, Content.SALIVA_BOTTLE.toStack());
            machine.setItem(6, Content.BLANK_NUCLEOCARD.toStack());
            machine.setItem(7, new ItemStack(Items.REDSTONE, 2));
        }
        if (player.tickCount == 140) {
            var machine = (CentrifugeBlockEntity) level.getBlockEntity(MACHINE);
            for (int i : new int[] {1, 3, 5}) machine.setItem(i, Content.SALIVA_BOTTLE.toStack());
        }
        if (player.tickCount == 810) {
            var machine = (CentrifugeBlockEntity) level.getBlockEntity(MACHINE);
            ItemStack card = machine.removeItem(6, 1);
            check(card.is(Content.CAT_NUCLEOCARD), "Completed card can be removed");
            level.setBlockAndUpdate(BREWER.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(BREWER, Blocks.BREWING_STAND.defaultBlockState());
            var stand = (BrewingStandBlockEntity) level.getBlockEntity(BREWER);
            for (int i = 0; i < 3; i++) stand.setItem(i, Content.SYRINGE.toStack());
            stand.setItem(3, card);
            stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException("Nekogenix centrifuge smoke failed: " + message);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean brewSound;

        @SubscribeEvent
        public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (ENABLED && event.getSound() != null
                    && event.getSound().getLocation().equals(net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW.getLocation())) brewSound = true;
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 45) mc.options.fov().set(35);
            if (age == 60) {
                var machine = (CentrifugeBlockEntity) mc.level.getBlockEntity(MACHINE);
                check(machine != null && machine.getItem(0).is(Content.SALIVA_BOTTLE) && machine.getItem(1).isEmpty(),
                        "World rendering receives the actual partial inventory");
                capture(mc, "centrifuge-world-idle.png");
            }
            if (age == 80 || age == 200) open(mc, MACHINE);
            if (age == 100) { check(mc.screen instanceof io.github.nekogenix.client.CentrifugeScreen, "Registered menu opens through normal interaction"); capture(mc, "centrifuge-menu-idle.png"); }
            if (age == 120) mc.player.closeContainer();
            if (age == 130) capture(mc, "centrifuge-world-open.png");
            if (age == 180) capture(mc, "centrifuge-world-running.png");
            if (age == 240) {
                if (Boolean.getBoolean("nekogenix.centrifugePreview")) {
                    capture(mc, "centrifuge-menu-preview.png");
                    Nekogenix.LOGGER.info("Nekogenix centrifuge preview passed");
                    mc.stop();
                    return;
                }
                var menu = (CentrifugeMenu) mc.player.containerMenu;
                check(menu.running(), "Processing state is synchronized to the open menu");
                mc.gameMode.handleInventoryMouseClick(menu.containerId, 0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
            }
            if (age == 320 || age == 520) {
                var menu = (CentrifugeMenu) mc.player.containerMenu;
                check(menu.running() && menu.getSlot(0).getItem().is(Content.SALIVA_BOTTLE) && menu.getCarried().isEmpty(),
                        "Client cannot extract an active sample");
                capture(mc, age == 320 ? "centrifuge-menu-progress.png" : "centrifuge-menu-progress-late.png");
            }
            if (age == 780) {
                var menu = (CentrifugeMenu) mc.player.containerMenu;
                check(!menu.running() && menu.getSlot(6).getItem().is(Content.CAT_NUCLEOCARD), "Filled card appears in menu");
                for (int i = 0; i < 6; i++) check(menu.getSlot(i).getItem().is(Items.GLASS_BOTTLE), "Empty bottle outputs arrive on client");
                capture(mc, "centrifuge-menu-complete.png");
                Nekogenix.LOGGER.info("Nekogenix centrifuge cycle passed: real menu, locked samples, completed card, six empty bottles");
            }
            if (age == 790) mc.player.closeContainer();
            if (age == 805) capture(mc, "centrifuge-world-complete.png");
            if (age == 840) open(mc, BREWER);
            if (age == 900) capture(mc, "centrifuge-brewing.png");
            if (age == 1250) {
                var menu = mc.player.containerMenu;
                check(menu instanceof net.minecraft.world.inventory.BrewingStandMenu && menu.getSlot(0).getItem().is(Content.INJECTION)
                        && menu.getSlot(1).getItem().is(Content.SYRINGE) && menu.getSlot(2).getItem().is(Content.SYRINGE)
                        && menu.getSlot(3).getItem().is(Content.BLANK_NUCLEOCARD) && brewSound,
                        "Brewing returns blank card fills exactly one syringe and plays vanilla sound");
                capture(mc, "centrifuge-brewing-complete.png");
                Nekogenix.LOGGER.info("Nekogenix centrifuge smoke passed: world inventory and menu synchronized, complete extraction and single syringe brewing with reusable blank card");
            }
            if (age >= 1280) mc.stop();
        }

        private static void open(net.minecraft.client.Minecraft mc, BlockPos pos) {
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
        }

        private static void capture(net.minecraft.client.Minecraft mc, String name) {
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
        }
    }
}
