package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NekoSicknessSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.nekoSicknessSmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount == 20) {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(10);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            var state = player.getData(Content.NEKO);
            state.begin();
            for (int i = 0; i < state.sicknessDuration() - 160; i++) state.tick(player.getRandom());
            TransformationHandler.refreshEffects(player);
        }
        if (player.tickCount == 150) check(player.hasEffect(Content.SHOCK) && !player.hasEffect(MobEffects.POISON)
                && player.getHealth() < 20, "Sickness poisons the real server player without a second effect");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static float recoveredHealth;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            if (age == 60) {
                check(mc.player.hasEffect(Content.SHOCK) && !mc.player.hasEffect(MobEffects.POISON),
                        "Only Neko Sickness is synchronized, with no poison icon for any effect renderer");
                mc.options.screenEffectScale().set(0.0);
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            }
            if (age == 100) capture = "neko-sickness-inventory.png";
            if (age == 110) mc.setScreen(null);
            if (age == 140) {
                check(!mc.player.hasEffect(MobEffects.POISON) && mc.player.getHealth() < 20,
                        "Sickness damage reaches the client without creating vanilla poison");
                capture = "neko-sickness-hud.png";
            }
            if (age == 200) {
                check(!mc.player.hasEffect(Content.SHOCK) && !mc.player.hasEffect(MobEffects.POISON),
                        "The illness ends without a hidden poison status");
                recoveredHealth = mc.player.getHealth();
            }
            if (age == 230) {
                check(mc.player.getHealth() == recoveredHealth, "Damage stops with the sickness");
                Nekogenix.LOGGER.info("Nekogenix sickness smoke passed: native poisoning within one visible sickness effect inventory HUD and recovery");
                mc.stop();
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(capture);
            java.nio.file.Files.createDirectories(path.getParent());
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(path); }
            capture = null;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Neko sickness smoke failed: " + message);
    }
}
