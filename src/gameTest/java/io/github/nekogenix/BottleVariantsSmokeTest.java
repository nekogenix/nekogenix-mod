package io.github.nekogenix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class BottleVariantsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.bottleVariantsSmokeTest");
    private static int shots, clouds;

    @SubscribeEvent public static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!ENABLED || event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof io.github.nekogenix.item.SalivaProjectile) shots++;
        if (event.getEntity() instanceof AreaEffectCloud) clouds++;
    }

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 80);
            var bottles = new net.minecraft.world.item.Item[] {Content.SALIVA_BOTTLE.get(), Content.SPLASH_SALIVA.get(), Content.LINGERING_SALIVA.get(),
                    Content.NEKO_STABILIZER.get(), Content.SPLASH_SWEET_AMETHYST.get(), Content.LINGERING_SWEET_AMETHYST.get()};
            for (int i = 0; i < bottles.length; i++) player.getInventory().setItem(i, bottles[i].getDefaultInstance());
            player.inventoryMenu.broadcastChanges();
        }
        if (age == 400) {
            check(shots == 4 && clouds == 2, "Four actual item uses throw four projectiles and produce two lingering clouds");
            check(player.hasData(Content.EXPOSURE) && player.getData(Content.EXPOSURE) > player.serverLevel().getGameTime()
                    && !player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Splash and lingering saliva keep the delayed illness");
            check(player.hasEffect(Content.SWEETNESS) && !player.hasEffect(Content.CRYSTALLIZATION), "Both sweet amethyst variants deliver their own beneficial effect");
            player.addEffect(new MobEffectInstance(Content.CRYSTALLIZATION, 1200, 0, false, false, true));
        }
        if (age == 580) player.removeEffect(Content.CRYSTALLIZATION);
        if (age == 650) Nekogenix.LOGGER.info("Nekogenix bottle variants server smoke passed: four real throws two native clouds delayed saliva and crystalline sweetness");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            if (age == 40) { mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); capture = "bottle-variants-inventory.png"; }
            if (age == 60) mc.setScreen(null);
            if (age == 90) mc.player.getInventory().selected = 1;
            if (age == 170) mc.player.getInventory().selected = 2;
            if (age == 250) mc.player.getInventory().selected = 4;
            if (age == 330) mc.player.getInventory().selected = 5;
            if (age < 380) { mc.player.setXRot(80); mc.player.xRotO = 80; }
            mc.options.keyUse.setDown(age == 100 || age == 180 || age == 260 || age == 340);
            if (age == 375) capture = "bottle-variants-clouds.png";
            if (age == 425) { mc.player.setXRot(0); mc.player.xRotO = 0; }
            if (age == 465) capture = "crystal-overlay-manual.png";
            if (age == 490) { mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); capture = "crystalline-sweetness-icon.png"; }
            if (age == 510) mc.setScreen(null);
            if (age == 520) org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 700);
            if (age == 540) capture = "crystal-overlay-manual-wide.png";
            if (age == 655) {
                check(mc.player.hasEffect(Content.SWEETNESS) && !mc.player.hasEffect(Content.CRYSTALLIZATION), "Cloud effects synchronize and crystallization clears");
                check(Content.SWEETNESS.get().getDisplayName().getString().equals("Crystalline Sweetness"), "The renamed effect is translated");
                for (var item : new net.minecraft.world.item.Item[] {Content.LINGERING_SALIVA.get(), Content.SPLASH_SWEET_AMETHYST.get(), Content.LINGERING_SWEET_AMETHYST.get()}) {
                    var model = mc.getItemRenderer().getModel(item.getDefaultInstance(), mc.level, mc.player, 0);
                    check(!model.getParticleIcon().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()), "All new potion models load");
                }
                check(mc.getItemColors().getColor(Content.SPLASH_SWEET_AMETHYST.toStack(), 0) == 0xFFBD91D3, "Thrown amethyst uses its own liquid color");
                Nekogenix.LOGGER.info("Nekogenix bottle variants client smoke passed: six bottle forms real throws colored models renamed effect manual icon and manual crystal overlay");
                mc.stop();
            }
        }

        @SubscribeEvent public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) throws java.io.IOException {
            if (!ENABLED || capture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                var directory = mc.gameDirectory.toPath().resolve("screenshots");
                java.nio.file.Files.createDirectories(directory);
                image.writeToFile(directory.resolve(capture));
            }
            capture = null;
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Bottle variants smoke failed: " + message);
    }
}
