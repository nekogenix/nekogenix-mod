package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.network.InjectionInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static boolean held;
    private static int ticks;
    private static boolean loggedJoin;
    private static boolean shockWasActive;

    private static float elapsed(LivingEntity entity, float partial) {
        if (entity == null || !entity.hasData(Content.INJECTION_STARTED)) return -1;
        var returning = entity.getData(Content.INJECTION_RETURN);
        if (returning.started() >= 0) {
            float remaining = returning.remaining(entity.level().getGameTime(), partial);
            return remaining > 0 ? returning.progress() * remaining : -1;
        }
        long start = entity.getData(Content.INJECTION_STARTED);
        return start < 0 ? -1 : entity.level().getGameTime() - start + partial;
    }

    private static boolean injecting(LivingEntity entity) {
        float age = elapsed(entity, 0);
        return age >= 0 && age < InjectionHandler.TOTAL_TICKS;
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && InjectionHandler.isInjection(player.getMainHandItem())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { held = false; return; }
        sicknessDistortion(mc);
        boolean down = mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator()
                && InjectionHandler.isInjection(mc.player.getMainHandItem()) && mc.options.keyAttack.isDown();
        if (down != held || down && ++ticks % 20 == 0) {
            PacketDistributor.sendToServer(new InjectionInput(down));
            held = down;
        }
        if (!loggedJoin && mc.player.tickCount > 20) {
            CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), mc.player.canUseGameMasterBlocks(), mc.level.registryAccess());
            var tab = Content.TAB.get();
            boolean icon = tab.getIconItem().is(Content.INJECTION);
            int count = tab.getDisplayItems().size();
            if (icon && count == Content.itemCount()) {
                Nekogenix.LOGGER.info("Nekogenix client joined world {}; creative tab contains {} items with Neko Injection icon",
                        mc.level.dimension().location(), count);
                loggedJoin = true;
            } else {
                Nekogenix.LOGGER.error("Nekogenix creative tab validation failed: items={}, injectionIcon={}", count, icon);
                loggedJoin = true;
            }
        }
    }

    @SubscribeEvent
    public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) sicknessDistortion(mc);
    }

    private static void sicknessDistortion(Minecraft mc) {
        float intensity;
        if (mc.player.hasEffect(Content.SHOCK)) {
            shockWasActive = true;
            // Reduced distortion uses scaled camera movement instead of the green nausea overlay.
            intensity = mc.options.screenEffectScale().get() < 1 ? 0F : 1.5F;
        } else if (shockWasActive) {
            var nausea = mc.player.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
            // The hidden nausea can outlive the sickness marker by a frame, including at duration zero.
            boolean lingering = nausea != null && !nausea.isVisible() && !nausea.showIcon();
            intensity = lingering || nausea == null ? 0F : 1F;
            if (!lingering) shockWasActive = false;
        } else return;
        mc.player.spinningEffectIntensity = mc.player.oSpinningEffectIntensity = intensity;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        held = loggedJoin = shockWasActive = false;
        ticks = 0;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(Content.SHOCK)) return;
        float scale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float time = player.tickCount + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + (Mth.sin(time * .73F) * 2.2F + Mth.sin(time * .17F) * 3.0F) * scale);
        event.setPitch(event.getPitch() + Mth.cos(time * .59F) * 2.5F * scale);
        event.setRoll(event.getRoll() + (Mth.sin(time * .19F) * 8.0F + Mth.cos(time * .89F) * 1.8F) * scale);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (var item : new net.minecraft.world.item.Item[] {Content.INJECTION.get(), Content.STABILIZING_INJECTION.get()}) ItemProperties.register(item, Content.id("injection_stage"),
                    (stack, level, entity, seed) -> {
                        float age = elapsed(entity, 0);
                        if (age < 0 || age >= InjectionHandler.TOTAL_TICKS || entity.getMainHandItem() != stack) return 0;
                        if (age >= InjectionHandler.FORWARD_TICKS) return 4 + Math.min(3, ((int)age - InjectionHandler.FORWARD_TICKS) / 3);
                        int frame = Math.min(3, (int)(Mth.clamp((age - InjectionHandler.NEEDLE_ENTRY_TICKS)
                                / (InjectionHandler.FORWARD_TICKS - InjectionHandler.NEEDLE_ENTRY_TICKS), 0, 1) * 4));
                        return frame;
                    });
            });
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(Content.SALIVA_PROJECTILE.get(), ThrownItemRenderer::new);
        }

        @SubscribeEvent
        public static void extensions(RegisterClientExtensionsEvent event) {
            EffectDisplay.register(event);
            event.registerItem(new IClientItemExtensions() {
                @Override
                public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                    return hand == InteractionHand.MAIN_HAND && injecting(entity) ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
                }

                @Override
                public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                        ItemStack stack, float partial, float equip, float swing) {
                    float age = elapsed(player, partial);
                    if (age < 0 || age >= InjectionHandler.TOTAL_TICKS) return false;
                    float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                    float approachAge = age;
                    float retreat = age < InjectionHandler.FORWARD_TICKS ? 1
                            : 1 - (age - InjectionHandler.FORWARD_TICKS) / InjectionHandler.RETURN_TICKS;
                    float pressure = Mth.sin(age * .48F) * .001F;
                    float tremor = Mth.cos(age * .48F) * .1F;
                    var returning = player.getData(Content.INJECTION_RETURN);
                    if (returning.started() >= 0) {
                        retreat = returning.remaining(player.level().getGameTime(), partial);
                        approachAge = returning.progress();
                        pressure *= retreat;
                        tremor *= retreat;
                    }
                    float approach = Mth.clamp(approachAge / InjectionHandler.APPROACH_TICKS, 0, 1);
                    approach = approach * approach * (3 - 2 * approach);
                    float jab = Mth.clamp((approachAge - InjectionHandler.APPROACH_TICKS)
                            / (InjectionHandler.NEEDLE_ENTRY_TICKS - InjectionHandler.APPROACH_TICKS), 0, 1);
                    pressure *= 1 - jab;
                    tremor *= 1 - jab;
                    // The intact needle crosses the lower view boundary while the barrel stays above it.
                    pose.translate(side * (.56F - .45F * approach * retreat), -.52F + (.45072432F * approach - .09F * jab) * retreat + pressure,
                            -.72F - .069F * approach * retreat);
                    pose.mulPose(Axis.YP.rotationDegrees(side * (45F + 45F * approach * retreat)));
                    pose.mulPose(Axis.ZP.rotationDegrees(side * (145F * approach * retreat)));
                    pose.mulPose(Axis.XP.rotationDegrees(25F * approach * retreat + tremor));
                    return true;
                }
            }, Content.INJECTION.get(), Content.STABILIZING_INJECTION.get());
        }
    }

    private ClientEvents() {}
}
