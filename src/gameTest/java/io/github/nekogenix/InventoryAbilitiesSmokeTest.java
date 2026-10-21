package io.github.nekogenix;

import io.github.nekogenix.client.InventoryAbilities;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class InventoryAbilitiesSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.inventoryAbilitiesSmokeTest");

    @SubscribeEvent
    public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            player.setGameMode(GameType.SURVIVAL);
            CatTransformationTests.cat(player);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 12000, 0, true, false, true));
        }
        if (player.tickCount == 170) player.setGameMode(GameType.CREATIVE);
        if (player.tickCount == 240) {
            var tag = new CompoundTag();
            tag.putInt("phase", NekoState.Phase.HUMAN.ordinal());
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), tag);
            TransformationHandler.refreshEffects(player);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static net.minecraft.client.gui.components.Button done;

        @SubscribeEvent
        public static void init(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
            if (!ENABLED || !(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen
                    || event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)) return;
            if (net.neoforged.fml.ModList.get().isLoaded("inventoryprofilesnext")) return;
            var screen = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)event.getScreen();
            int tabs = screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen ? 28 : 0;
            done = net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.CommonComponents.GUI_DONE, button -> {})
                    .bounds(screen.width / 2 - 100, Math.min(screen.height - 24, screen.getGuiTop() + screen.getYSize() + tabs + 2), 200, 20).build();
            event.addListener(done);
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            mc.getToasts().clear();
            int age = mc.player.tickCount;
            if (age == 60) {
                io.github.nekogenix.client.InventoryAbilityLayoutChecks.verify();
                for (var effect : java.util.List.of(Content.AGILITY, Content.VISION, Content.PAWS)) {
                    var instance = mc.player.getEffect(effect);
                    var extension = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(effect.value());
                    check(instance != null && !extension.isVisibleInGui(instance) && !extension.isVisibleInInventory(instance),
                            "Permanent abilities are hidden from both vanilla effect lists");
                }
                for (var effect : java.util.List.of(Content.SHOCK, Content.TRANSFORMING, Content.CAT_TRANSFORMING, MobEffects.FIRE_RESISTANCE, MobEffects.POISON)) {
                    var instance = new MobEffectInstance(effect, 1200);
                    var extension = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(effect.value());
                    check(extension.isVisibleInGui(instance) && extension.isVisibleInInventory(instance), "Temporary effects stay visible");
                }
                check(mc.player.getData(Content.CAT_FORM), "Cat form is synchronized from the dedicated server");
                check(net.minecraft.network.chat.Component.translatable("ability.nekogenix.feline_scent").getString().equals("Feline Scent"), "Scent name is translated");
                mc.player.addEffect(new MobEffectInstance(Content.SHOCK, 150));
                mc.player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 0, false, false, false));
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 150, 0, true, false, false));
            }
            if (age == 70 || age == 105 || age == 140) {
                mc.options.guiScale().set(age == 70 ? 2 : age == 105 ? 3 : 4);
                mc.resizeDisplay();
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            }
            if (age == 80) capture = "abilities-scale2";
            if (age == 115) capture = "abilities-scale3";
            if (age == 125 && done != null) done.setY(done.getY() + 8);
            if (age == 130 && mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
                io.github.nekogenix.client.InventoryAbilityLayoutChecks.verify(screen);
            }
            if (age == 135 && done != null) done.visible = false;
            if (age == 150) capture = "abilities-scale4";
            if (age == 185) {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
                        mc.level.enabledFeatures(), mc.player.canUseGameMasterBlocks()));
            }
            if (age == 200) capture = "abilities-creative-scale4";
            if (age == 220) { mc.setScreen(null); capture = "abilities-hud"; }
            if (age == 260) {
                check(!mc.player.getData(Content.CAT_FORM) && !mc.player.hasEffect(Content.AGILITY)
                        && !mc.player.hasEffect(Content.VISION) && !mc.player.hasEffect(Content.PAWS), "Human state removes all ability indicators");
                Nekogenix.LOGGER.info("Nekogenix inventory abilities smoke passed: permanent indicators hidden temporary effects visible scent available only in cat form three GUI scales and creative inventory rendered");
                mc.stop();
            }
        }

        @SubscribeEvent
        public static void tooltip(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
            if (!ENABLED || capture == null || !capture.startsWith("abilities-")
                    || !(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) return;
            io.github.nekogenix.client.InventoryAbilityLayoutChecks.verify(screen);
            if (!"abilities-scale2".equals(capture)) return;
            var cell = InventoryAbilities.bounds(screen).getLast();
            InventoryAbilities.render(new net.neoforged.neoforge.client.event.ScreenEvent.Render.Post(screen, event.getGuiGraphics(),
                    cell.left() + cell.width() / 2, cell.top() + cell.height() / 2, event.getPartialTick()));
        }

        @SubscribeEvent
        public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var path = mc.gameDirectory.toPath().resolve("screenshots");
                java.nio.file.Files.createDirectories(path);
                image.writeToFile(path.resolve(capture + ".png"));
            } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
            capture = null;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Inventory abilities smoke failed: " + message);
    }
}
