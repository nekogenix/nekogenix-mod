package io.github.nekogenix;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class SweetAmethystSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.sweetAmethystSmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            TransformationTests.setState(player, 4);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(0); player.getFoodData().setSaturation(0);
            player.serverLevel().setDayTime(6000);
            player.teleportTo(player.serverLevel(), .5, -60, .5, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.NEKO_STABILIZER.toStack());
        }
        if (age == 130) {
            check(player.getMainHandItem().is(Items.GLASS_BOTTLE) && player.hasEffect(Content.SWEETNESS), "Actual drinking consumes one potion and returns glass");
            check(player.getFoodData().getFoodLevel() > 0 && player.getFoodData().getFoodLevel() < 5 && !player.hasEffect(Content.CRYSTALLIZATION),
                    "A drink starts gradual nourishment without crystallizing the player");
        }
        if (age == 550) {
            check(player.getFoodData().getFoodLevel() == 20 && player.getFoodData().getSaturationLevel() == 20, "One drink reaches full food and saturation");
            var tag = player.getData(Content.NEKO).serializeNBT(player.registryAccess());
            tag.putInt("phase", io.github.nekogenix.state.NekoState.Phase.CAT_TRANSFORMING.ordinal());
            tag.putInt("elapsed", 0);
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), tag);
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
        }
        if (age == 710) {
            check(player.getMainHandItem().is(Content.USED_SYRINGE) && player.hasEffect(Content.CRYSTALLIZATION), "Real injection starts Crystallization and returns a used syringe");
            check(player.getData(Content.NEKO).phase() == io.github.nekogenix.state.NekoState.Phase.NEKO, "Crystallization preserves permanent stabilization");
            check(player.getEffect(Content.CRYSTALLIZATION).getDuration() > 1150, "The injection grants the full sixty seconds");
        }
        if (age == 800) check(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < .006, "Full crystallization nearly immobilizes the player on the server");
        if (age == 820) player.removeEffect(Content.CRYSTALLIZATION);
        if (age == 920) {
            check(!player.hasEffect(Content.CRYSTALLIZATION) && player.hasEffect(Content.SWEETNESS)
                    && player.getData(Content.NEKO).phase() == io.github.nekogenix.state.NekoState.Phase.NEKO, "The penalty ends without removing nourishment or stabilization");
            check(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - .1) < .0001, "The server restores ordinary movement");
            Nekogenix.LOGGER.info("Nekogenix sweet amethyst server smoke passed: actual drinking gradual nourishment real crystallizing injection and restored movement");
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String capture;
        private static boolean partial, full, fading;
        private static float previous;
        private static int crystalSounds;

        @SubscribeEvent public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (!ENABLED || event.getSound() == null) return;
            var id = event.getSound().getLocation();
            if (id.equals(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE.getLocation())
                    || id.equals(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP.getLocation())) crystalSounds++;
        }

        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.getToasts().clear();
            mc.options.keyUse.setDown(age >= 70 && age < 105 || age >= 590 && age < 700);
            if (age == 130) check(mc.player.hasEffect(Content.SWEETNESS) && mc.player.getFoodData().getFoodLevel() < 5, "The client sees nourishment without a full food prediction");
            if (age == 535) check(mc.player.getFoodData().getFoodLevel() == 20, "The client receives full hunger after gradual refill");
            float alpha = io.github.nekogenix.client.CrystallizationOverlay.strength(1);
            check(Math.abs(alpha - previous) < .025, "The crystal overlay changes gradually");
            previous = alpha;
            if (age >= 700 && age < 750 && alpha > .05 && alpha < .95) partial = true;
            if (age >= 770 && age < 815 && alpha > .99) full = true;
            if (age > 830 && alpha > .1 && alpha < .9) fading = true;
            if (age == 725 || age == 780 || age == 850 || age == 910) capture = "crystallization-" + age + ".png";
            if (age == 785) { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT); capture = "crystallization-particles.png"; }
            if (age == 795) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            if (age == 805) { mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); capture = "crystallization-effects.png"; }
            if (age == 810) mc.setScreen(null);
            if (age == 925) {
                check(partial && full && fading && alpha == 0 && crystalSounds > 0, "Crystal visuals and sounds grow and fade completely");
                check(!mc.player.hasEffect(Content.CRYSTALLIZATION) && mc.player.hasEffect(Content.SWEETNESS)
                        && Math.abs(mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) - .1) < .0001, "The client restores movement without stale effects");
                Nekogenix.LOGGER.info("Nekogenix sweet amethyst client smoke passed: drinking injection gradual amethyst overlay particles sounds temporary effect icons and complete recovery");
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
        if (!valid) throw new IllegalStateException("Sweet amethyst smoke failed: " + message);
    }
}
