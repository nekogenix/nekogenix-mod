package io.github.nekogenix.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import io.github.nekogenix.gameplay.FelineScent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class ScentOutlines {
    private static final FelineScent.Focus FOCUS = new FelineScent.Focus();
    private static final VertexConsumer DISCARD = VertexMultiConsumer.create(new VertexConsumer[0]);
    private static final Map<RenderType, RenderType> TYPES = new HashMap<>();
    private static OutlineBufferSource outlines;
    private static ClientLevel level;
    private static LocalPlayer observer;
    private static List<LivingEntity> targets = List.of();
    private static PostChain effect;
    private static int width, height, scan;
    private static boolean failed, drawn;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != level || mc.player != observer) {
            reset(); level = mc.level; observer = mc.player;
        }
        if (mc.isPaused()) return;
        boolean eligible = observer != null && observer.isAlive() && !observer.isSpectator() && CatForm.active(observer);
        if (FOCUS.tick(eligible, eligible && observer.isCrouching())) observer.playSound(SoundEvents.FOX_SNIFF, .6F, 1.1F);
        if (!FOCUS.visible() || level == null) { targets = List.of(); scan = 0; return; }
        if (scan-- <= 0) {
            scan = 4;
            targets = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(observer.position(), observer.position()).inflate(FelineScent.HORIZONTAL_RANGE, FelineScent.VERTICAL_RANGE, FelineScent.HORIZONTAL_RANGE),
                    entity -> FelineScent.inRange(observer, entity));
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            drawn = false;
            if (!FOCUS.visible() || targets.isEmpty() || mc.player != observer || mc.level != level
                    || !observer.isAlive() || !CatForm.active(observer) || mc.getCameraEntity() != observer
                    || !event.getLevelRenderer().shouldShowEntityOutlines()) return;
            if (!prepare(mc)) return;
            var mask = effect.getTempTarget("mask");
            mask.clear(Minecraft.ON_OSX);
            mc.getMainRenderTarget().bindWrite(false);
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            var camera = event.getCamera().getPosition();
            var dispatcher = mc.getEntityRenderDispatcher();
            MultiBufferSource buffers = type -> {
                var outline = type.isOutline() ? type : type.outline().orElse(null);
                if (outline == null) return DISCARD;
                if (TYPES.size() > 256) TYPES.clear();
                var output = TYPES.computeIfAbsent(outline, original -> new RenderType("nekogenix_scent", original.format(), original.mode(),
                        original.bufferSize(), false, false, () -> {
                            original.setupRenderState(); effect.getTempTarget("mask").bindWrite(false);
                        }, original::clearRenderState) {
                            @Override public boolean isOutline() { return true; }
                        });
                return outlines.getBuffer(output);
            };
            try {
                for (var entity : targets) {
                    if (entity.isRemoved() || !FelineScent.inRange(observer, entity)
                            || !event.getFrustum().isVisible(entity.getBoundingBox())) continue;
                    int color = FelineScent.color(entity);
                    outlines.setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255);
                    dispatcher.render(entity, Mth.lerp(partial, entity.xOld, entity.getX()) - camera.x,
                            Mth.lerp(partial, entity.yOld, entity.getY()) - camera.y,
                            Mth.lerp(partial, entity.zOld, entity.getZ()) - camera.z,
                            Mth.rotLerp(partial, entity.yRotO, entity.getYRot()), partial, event.getPoseStack(), buffers,
                            dispatcher.getPackedLightCoords(entity, partial));
                    drawn = true;
                }
                outlines.endOutlineBatch();
            } finally { mc.getMainRenderTarget().bindWrite(false); }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && drawn && effect != null) {
            try {
                float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
                effect.setUniform("Opacity", FOCUS.opacity(partial));
                effect.process(event.getPartialTick().getGameTimeDeltaTicks());
                mc.getMainRenderTarget().bindWrite(false);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                effect.getTempTarget("mask").blitToScreen(width, height, false);
            } finally {
                RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc();
                mc.getMainRenderTarget().bindWrite(false);
                drawn = false;
            }
        }
    }

    private static boolean prepare(Minecraft mc) {
        if (failed) return false;
        try {
            if (outlines == null) outlines = new OutlineBufferSource(mc.renderBuffers().bufferSource());
            if (effect == null) effect = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                    Content.id("shaders/post/feline_scent.json"));
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            if (width != w || height != h) { effect.resize(w, h); width = w; height = h; }
            return true;
        } catch (IOException | RuntimeException e) {
            close(); failed = true; Nekogenix.LOGGER.error("Could not load feline scent outline effect", e); return false;
        } finally { mc.getMainRenderTarget().bindWrite(false); }
    }

    private static void close() {
        if (effect != null) { effect.close(); effect = null; }
        width = height = 0; TYPES.clear(); drawn = false;
    }
    private static void reset() { FOCUS.reset(); targets = List.of(); observer = null; level = null; scan = 0; close(); failed = false; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> { close(); failed = false; });
        }
    }
    private ScentOutlines() {}
}
