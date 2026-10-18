package io.github.nekogenix;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
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
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
        }
    }

    @SubscribeEvent
    public static void stage(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || Boolean.getBoolean("nekogenix.voiceSmokeTest") || Boolean.getBoolean("nekogenix.transformationSmokeTest")
                || Boolean.getBoolean("nekogenix.injectionSmokeTest")
                || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (player.tickCount == 340) {
            player.serverLevel().setDayTime(6000);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1200));
        }
        if (player.tickCount == 380) {
            var vision = player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            if (vision != null && vision.showIcon() && !player.getData(Content.NEKO).visionOwned) {
                Nekogenix.LOGGER.info("Nekogenix external night vision preserved in daylight");
            } else Nekogenix.LOGGER.error("Nekogenix external night vision failed");
        }
        if (player.tickCount == 400) {
            player.getData(Content.NEKO).deserializeNBT(player.level().registryAccess(), new net.minecraft.nbt.CompoundTag());
            TransformationHandler.refreshEffects(player);
        }
        if (player.tickCount != 240) return;
        var state = player.getData(Content.NEKO);
        var saved = state.serializeNBT(player.level().registryAccess());
        saved.putInt("phase", NekoState.Phase.TRANSFORMING.ordinal());
        saved.putInt("elapsed", NekoState.DAY + 120);
        saved.putInt("abilities", NekoState.VISION);
        state.deserializeNBT(player.level().registryAccess(), saved);
        player.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
        player.serverLevel().setDayTime(13000);
        TransformationHandler.refreshEffects(player);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
    public static final class Client {
        private static boolean checked;
        private static boolean felineHurt;

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
        public static void sound(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity event) {
            if (ENABLED && event.getEntity() == net.minecraft.client.Minecraft.getInstance().player
                    && event.getSound() != null && event.getSound().value() == net.minecraft.sounds.SoundEvents.CAT_HURT) {
                felineHurt = true;
            }
        }

        @SubscribeEvent
        public static void input(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            if (mc.player == null || mc.level == null) return;
            if (Boolean.getBoolean("nekogenix.injectionSmokeTest")) return;
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
            if (Boolean.getBoolean("nekogenix.voiceSmokeTest") || Boolean.getBoolean("nekogenix.transformationSmokeTest")) return;
            if (age == 300) {
                var effect = mc.player.getEffect(Content.TRANSFORMING);
                var vision = mc.player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                var extension = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION.value());
                boolean hidden = vision != null && !extension.isVisibleInInventory(vision) && !extension.isVisibleInGui(vision);
                if (effect != null && effect.getAmplifier() == 1 && hidden && felineHurt && mc.player.getData(Content.FELINE_VOICE)) {
                    Nekogenix.LOGGER.info("Nekogenix runtime vision passed: night vision active, duplicate indicators hidden, stage II synchronized, feline hurt voice received");
                } else {
                    Nekogenix.LOGGER.error("Nekogenix runtime vision failed: stage={}, hidden={}, felineHurt={}, voice={}",
                            effect, hidden, felineHurt, mc.player.getData(Content.FELINE_VOICE));
                }
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            }
            if (age == 330) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "effects-prealpha2.png", mc.getMainRenderTarget(), message -> {});
            if (age == 340) {
                mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                        super.render(graphics, mouseX, mouseY, partial);
                        graphics.renderTooltip(font, Content.INJECTION.toStack(), 24, 30);
                        graphics.renderTooltip(font, Content.SALIVA_BOTTLE.toStack(), 24, 190);
                    }
                });
            }
            if (age == 360) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "tooltips-prealpha2.png", mc.getMainRenderTarget(), message -> {});
            if (age == 440) {
                var vision = mc.player.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
                var extension = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION.value());
                if (vision != null && extension.isVisibleInInventory(vision) && extension.isVisibleInGui(vision)
                        && !mc.player.getData(Content.FELINE_VOICE) && !mc.player.hasEffect(Content.VISION)) {
                    Nekogenix.LOGGER.info("Nekogenix runtime restoration passed: ordinary night vision visible and human voice restored");
                } else Nekogenix.LOGGER.error("Nekogenix runtime restoration failed");
            }
            if (age >= 460) mc.stop();
        }
    }
}
