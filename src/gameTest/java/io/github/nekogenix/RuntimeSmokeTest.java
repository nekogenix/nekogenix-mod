package io.github.nekogenix;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Opt-in client and dedicated-server smoke run; this source set is excluded from the released jar. */
@EventBusSubscriber(modid = Nekogenix.ID)
public final class RuntimeSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.smokeTest");

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (ENABLED && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
    public static final class Client {
        private static boolean checked;

        @SubscribeEvent
        public static void input(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            mc.options.keyAttack.setDown(age >= 40 && age < 140);
            if (age == 90) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "injection-smoke.png", mc.getMainRenderTarget(), message -> {});
            if (!checked && age >= 160) {
                checked = true;
                if (mc.player.getMainHandItem().is(Content.USED_SYRINGE) && mc.player.hasEffect(Content.SHOCK)) {
                    Nekogenix.LOGGER.info("Nekogenix runtime smoke passed: left mouse injection completed, used syringe received, shock active");
                    net.minecraft.client.Screenshot.grab(mc.gameDirectory, "shock-smoke.png", mc.getMainRenderTarget(), message -> {});
                } else {
                    Nekogenix.LOGGER.error("Nekogenix runtime smoke failed: held={}, shock={}", mc.player.getMainHandItem(), mc.player.hasEffect(Content.SHOCK));
                }
            }
            if (age >= 200) mc.stop();
        }
    }
}
