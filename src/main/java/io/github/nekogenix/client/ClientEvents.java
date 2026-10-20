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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
    private static boolean offHeld;
    private static boolean useWasDown;
    private static boolean primaryOff;
    private static boolean secondaryInjection;
    private static int ticks;
    private static boolean loggedJoin;
    private static boolean shockWasActive;

    private static float elapsed(LivingEntity entity, InteractionHand hand, float partial) {
        if (entity == null) return -1;
        var returning = InjectionHandler.returning(entity, hand);
        if (returning.started() >= 0) {
            float remaining = returning.remaining(entity.level().getGameTime(), partial);
            return remaining > 0 ? returning.progress() * remaining : -1;
        }
        long start = InjectionHandler.started(entity, hand);
        return start < 0 ? -1 : entity.level().getGameTime() - start + partial;
    }

    private static boolean injecting(LivingEntity entity, InteractionHand hand) {
        float age = elapsed(entity, hand, 0);
        return age >= 0 && age < InjectionHandler.TOTAL_TICKS;
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (player != null && (event.isUseItem() && (InjectionHandler.isInjection(player.getMainHandItem())
                || InjectionHandler.isInjection(player.getOffhandItem()))
                || event.isAttack() && (Minecraft.getInstance().options.keyUse.isDown()
                && (InjectionHandler.isInjection(player.getMainHandItem()) || InjectionHandler.isInjection(player.getOffhandItem()))
                || injecting(player, InteractionHand.MAIN_HAND) || injecting(player, InteractionHand.OFF_HAND)))) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            held = offHeld = useWasDown = primaryOff = secondaryInjection = false;
            return;
        }
        sicknessDistortion(mc);
        boolean enabled = mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator();
        boolean use = enabled && mc.options.keyUse.isDown();
        boolean attack = enabled && mc.options.keyAttack.isDown();
        if (use && !useWasDown) primaryOff = !InjectionHandler.isInjection(mc.player.getMainHandItem());
        useWasDown = use;
        if (!attack || !enabled) secondaryInjection = false;
        else if (!primaryOff && use && InjectionHandler.canDual(mc.player)) secondaryInjection = true;
        boolean down = !primaryOff && use && InjectionHandler.isInjection(mc.player.getMainHandItem());
        boolean offDown = (primaryOff && use || secondaryInjection && attack)
                && InjectionHandler.isInjection(mc.player.getOffhandItem());
        if (down != held || offDown != offHeld || (down || offDown) && ++ticks % 20 == 0) {
            PacketDistributor.sendToServer(new InjectionInput(down, offDown));
            held = down;
            offHeld = offDown;
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
            intensity = mc.options.screenEffectScale().get() < 1 ? 0F : 1.5F;
        } else if (shockWasActive) {
            var nausea = mc.player.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
            boolean lingering = nausea != null && !nausea.isVisible() && !nausea.showIcon();
            intensity = lingering || nausea == null ? 0F : 1F;
            if (!lingering) shockWasActive = false;
        } else return;
        mc.player.spinningEffectIntensity = mc.player.oSpinningEffectIntensity = intensity;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        held = offHeld = useWasDown = primaryOff = secondaryInjection = loggedJoin = shockWasActive = false;
        ticks = 0;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void hand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean main = injecting(player, InteractionHand.MAIN_HAND);
        boolean off = injecting(player, InteractionHand.OFF_HAND);
        if ((main || off) && !(event.getHand() == InteractionHand.MAIN_HAND ? main : off)) event.setCanceled(true);
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
            ModList.get().getModContainerById(Nekogenix.ID).orElseThrow().registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> new ModOptionsScreen(parent));
            event.enqueueWork(() -> {
                for (var item : new net.minecraft.world.item.Item[] {Content.INJECTION.get(), Content.STABILIZING_INJECTION.get()}) ItemProperties.register(item, Content.id("injection_stage"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) return 0;
                        InteractionHand hand = entity.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                                : entity.getOffhandItem() == stack ? InteractionHand.OFF_HAND : null;
                        if (hand == null) return 0;
                        float age = elapsed(entity, hand, 0);
                        if (age < 0 || age >= InjectionHandler.TOTAL_TICKS) return 0;
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
                    return injecting(entity, hand) ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
                }

                @Override
                public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                        ItemStack stack, float partial, float equip, float swing) {
                    InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    if (player.getItemInHand(hand) != stack) return false;
                    float age = elapsed(player, hand, partial);
                    if (age < 0 || age >= InjectionHandler.TOTAL_TICKS) return false;
                    float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                    float approachAge = age;
                    float retreat = age < InjectionHandler.FORWARD_TICKS ? 1
                            : 1 - (age - InjectionHandler.FORWARD_TICKS) / InjectionHandler.RETURN_TICKS;
                    float pressure = Mth.sin(age * .48F) * .001F;
                    float tremor = Mth.cos(age * .48F) * .1F;
                    var returning = InjectionHandler.returning(player, hand);
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
