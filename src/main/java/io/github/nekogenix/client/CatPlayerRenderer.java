package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CatPlayerRenderer extends CatRenderer {
    private static CatPlayerRenderer renderer;
    private static final Map<UUID, PlayerCat> CATS = new HashMap<>();
    private final ItemInHandRenderer items;
    private final ModelPart rightPaw;
    private final ModelPart leftPaw;

    private CatPlayerRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = PawModel.createRoot();
        model = new PawModel(root);
        CatEquipment equipment = new CatEquipment(context, root);
        items = context.getItemInHandRenderer();
        ModelPart paws = context.bakeLayer(ModelLayers.CAT);
        rightPaw = paws.getChild("right_front_leg");
        leftPaw = paws.getChild("left_front_leg");
        addLayer(new RenderLayer<Cat, CatModel<Cat>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Cat cat, float walk,
                    float speed, float partial, float age, float yaw, float pitch) {
                if (cat instanceof PlayerCat proxy && proxy.player instanceof AbstractClientPlayer player) {
                    equipment.render(player, pose, buffers, light, player.walkAnimation.position(partial),
                            player.walkAnimation.speed(partial), partial, age, yaw, pitch);
                }
                if (cat.isInvisible()) return;
                drawHeld(cat, HumanoidArm.RIGHT, pose, buffers, light, partial);
                drawHeld(cat, HumanoidArm.LEFT, pose, buffers, light, partial);
            }
        });
    }

    private void drawHeld(Cat cat, HumanoidArm arm, PoseStack pose, MultiBufferSource buffers, int light, float partial) {
        boolean left = arm == HumanoidArm.LEFT;
        ItemStack stack = arm == cat.getMainArm() ? cat.getMainHandItem() : cat.getOffhandItem();
        if (stack.isEmpty()) return;
        pose.pushPose();
        ((PawModel) model).paw(arm).translateAndRotate(pose);
        // Align the vanilla palm with the cat's toes, scaling the entire grip as well as the item.
        float scale = stack.getUseAnimation() == net.minecraft.world.item.UseAnim.SPEAR ? .4F : .55F;
        pose.translate(0, 9 / 16F, 1 / 16F);
        pose.scale(scale, scale, scale);
        pose.translate((left ? -1 : 1) / 16F, -10 / 16F, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate((left ? -1 : 1) / 16F, .125, -.625);
        Player player = ((PlayerCat) cat).player;
        items.renderItem(player, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, pose, buffers, light);
        pose.popPose();
    }

    @Override
    protected void setupRotations(Cat cat, PoseStack pose, float age, float yaw, float partial, float scale) {
        PlayerCat proxy = (PlayerCat) cat;
        super.setupRotations(cat, pose, age, yaw + proxy.bodyYaw(partial), partial, scale);
        // A cat is already horizontal; tilt around its center rather than applying the humanoid dive pose.
        pose.translate(0, .35, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-proxy.bodyPitch(partial)));
        pose.mulPose(Axis.ZP.rotationDegrees(proxy.bodyRoll(partial)));
        pose.translate(0, -.35, 0);
    }

    @Override
    protected boolean shouldShowName(Cat cat) {
        if (!(cat instanceof PlayerCat proxy)) return false;
        var mc = Minecraft.getInstance();
        Player player = proxy.player;
        if (player == mc.getCameraEntity() || player.isInvisibleTo(mc.player) || !Minecraft.renderNames()) return false;
        double distance = entityRenderDispatcher.distanceToSqr(player);
        if (distance >= (player.isDiscrete() ? 1024 : 4096)) return false;
        Team team = player.getTeam();
        Team viewer = mc.player == null ? null : mc.player.getTeam();
        if (team == null) return true;
        return switch (team.getNameTagVisibility()) {
            case ALWAYS -> true;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> viewer == null || team.isAlliedTo(viewer);
            case HIDE_FOR_OWN_TEAM -> viewer == null || !team.isAlliedTo(viewer);
        };
    }

    private static PlayerCat proxy(Player player) {
        PlayerCat cat = CATS.get(player.getUUID());
        if (cat == null || cat.player != player) {
            cat = new PlayerCat(player);
            CATS.put(player.getUUID(), cat);
        }
        cat.updatePose();
        cat.setPos(player.position());
        cat.setYRot(player.getYRot());
        cat.yRotO = player.yRotO;
        cat.setXRot(player.getXRot());
        cat.xRotO = player.xRotO;
        cat.yBodyRot = player.yBodyRot;
        cat.yBodyRotO = player.yBodyRotO;
        cat.yHeadRot = player.yHeadRot;
        cat.yHeadRotO = player.yHeadRotO;
        cat.tickCount = player.tickCount;
        cat.hurtTime = player.hurtTime;
        cat.deathTime = player.deathTime;
        cat.attackAnim = player.attackAnim;
        cat.oAttackAnim = player.oAttackAnim;
        cat.setInvisible(player.isInvisible());
        cat.setSprinting(player.isSprinting());
        cat.setGlowingTag(player.isCurrentlyGlowing());
        cat.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem());
        cat.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem());
        return cat;
    }

    @SubscribeEvent
    public static void player(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (renderer == null || !CatForm.active(player) || player.isSpectator()) return;
        event.setCanceled(true);
        PlayerCat cat = proxy(player);
        float partial = event.getPartialTick();
        renderer.render(cat, Mth.rotLerp(partial, player.yRotO, player.getYRot()), partial,
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (renderer == null || player == null || !CatForm.active(player) || player.isSpectator() || player.isScoping()) return;
        ItemStack stack = event.getItemStack();
        // Vanilla controls item transforms, use animations, maps and which hands are visible.
        // Empty hands and map arms reach the arm event below.
        if (stack.isEmpty() || stack.getItem() instanceof MapItem || player.isInvisible()) return;
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        PlayerCat cat = proxy(player);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float swing = Mth.sin(Mth.sqrt(event.getSwingProgress()) * Mth.PI);
        pose.translate(side * (.56 - swing * .16), -.52 - event.getEquipProgress() * .6 + swing * .12, -.72);
        if (player.isUsingItem() && player.getUsedItemHand() == event.getHand()) {
            switch (stack.getUseAnimation()) {
                case EAT, DRINK -> pose.translate(side * -.18, .16, .12);
                case BLOCK -> pose.translate(side * -.12, .08, .05);
                case BOW, CROSSBOW, SPEAR -> pose.translate(side * -.22, .08, .06);
                default -> {}
            }
        }
        pose.mulPose(Axis.ZP.rotationDegrees(-side * swing * 20));
        ModelPart paw = renderer.paw(arm);
        paw.setPos(0, -10, -.5F);
        paw.render(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutout(cat.getTextureId())),
                event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        var player = event.getPlayer();
        if (renderer == null || !CatForm.active(player) || player.isSpectator()) return;
        event.setCanceled(true);
        if (player.isInvisible()) return;
        PlayerCat cat = proxy(player);
        ModelPart paw = renderer.paw(event.getArm());
        float side = event.getArm() == HumanoidArm.RIGHT ? 1 : -1;
        paw.setPos(-side * 5, 2, -1);
        paw.yScale = 1.2F;
        paw.render(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.entityCutout(cat.getTextureId())),
                event.getPackedLight(), OverlayTexture.NO_OVERLAY);
    }

    private ModelPart paw(HumanoidArm arm) {
        ModelPart paw = arm == HumanoidArm.LEFT ? leftPaw : rightPaw;
        paw.resetPose();
        return paw;
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof Player player) CATS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CATS.clear(); }

    private static final class PawModel extends CatModel<Cat> {
        private final ModelPart ears;
        private PawModel(ModelPart root) { super(root); ears = head.getChild("ears"); }
        private ModelPart paw(HumanoidArm arm) { return arm == HumanoidArm.LEFT ? leftFrontLeg : rightFrontLeg; }

        private static ModelPart createRoot() {
            var mesh = CatModel.createBodyMesh(CubeDeformation.NONE);
            var head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-2.5F, -2, -3, 5, 4, 5)
                    .texOffs(0, 24).addBox(-1.5F, -.001F, -4, 3, 2, 2), PartPose.offset(0, 15, -9));
            head.addOrReplaceChild("ears", CubeListBuilder.create()
                    .texOffs(0, 10).addBox(-2, -3, 0, 1, 1, 2)
                    .texOffs(6, 10).addBox(1, -3, 0, 1, 1, 2), PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 32).bakeRoot();
        }

        @Override
        public void prepareMobModel(Cat cat, float walk, float speed, float partial) {
            leftFrontLeg.resetPose(); rightFrontLeg.resetPose();
            leftHindLeg.resetPose(); rightHindLeg.resetPose();
            tail1.resetPose(); tail2.resetPose();
            super.prepareMobModel(cat, walk, speed, partial);
        }

        @Override
        public void setupAnim(Cat cat, float walk, float speed, float age, float yaw, float pitch) {
            PlayerCat proxy = (PlayerCat) cat;
            Player player = proxy.player;
            float partial = Mth.clamp(age - cat.tickCount, 0, 1);
            boolean walking = player.onGround() && !player.isInWaterOrBubble();
            super.setupAnim(cat, player.walkAnimation.position(partial), walking ? player.walkAnimation.speed(partial) : 0, age, yaw, pitch);
            ears.visible = player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            head.yRot = Mth.clamp(Mth.wrapDegrees(yaw - proxy.bodyYaw(partial)), -70, 70) * Mth.DEG_TO_RAD;
            head.xRot = Mth.clamp(pitch - proxy.bodyPitch(partial), -60, 60) * Mth.DEG_TO_RAD;
            float air = proxy.air(partial);
            float stride = walking ? Math.min(1, player.walkAnimation.speed(partial)) * .85F : 0;
            float phase = player.walkAnimation.position(partial) * (player.isSprinting() ? 1.45F : .9F);
            float hindOffset = player.isSprinting() ? .65F : .2F;
            rightFrontLeg.xRot = Mth.cos(phase) * stride;
            leftFrontLeg.xRot = Mth.cos(phase + Mth.PI) * stride;
            rightHindLeg.xRot = Mth.cos(phase + Mth.PI + hindOffset) * stride;
            leftHindLeg.xRot = Mth.cos(phase + hindOffset) * stride;
            float forward = proxy.forward(partial) * .3F + proxy.vertical(partial) * .15F;
            float sideways = proxy.sideways(partial) * .25F;
            rightFrontLeg.zRot = air * .75F + sideways;
            leftFrontLeg.zRot = -air * .75F + sideways;
            rightHindLeg.zRot = air * .65F + sideways;
            leftHindLeg.zRot = -air * .65F + sideways;
            rightFrontLeg.xRot -= air * .22F; leftFrontLeg.xRot -= air * .22F;
            rightHindLeg.xRot += air * .22F; leftHindLeg.xRot += air * .22F;
            rightFrontLeg.xRot += forward; leftFrontLeg.xRot += forward;
            rightHindLeg.xRot += forward; leftHindLeg.xRot += forward;
            float swim = proxy.swim(partial);
            rightFrontLeg.xRot += swim * .12F + proxy.stroke(partial, 1) * .5F;
            leftFrontLeg.xRot += swim * .12F + proxy.stroke(partial, -1) * .5F;
            rightHindLeg.xRot += swim * .18F + proxy.stroke(partial, -1) * .35F;
            leftHindLeg.xRot += swim * .18F + proxy.stroke(partial, 1) * .35F;
            tail(proxy, partial);
            hold(player, HumanoidArm.RIGHT, partial);
            hold(player, HumanoidArm.LEFT, partial);
        }

        private void tail(PlayerCat cat, float partial) {
            float motion = cat.motion(partial);
            tail1.xRot = Mth.lerp(motion, tail1.xRot, 1.15F + cat.vertical(partial) * .25F - cat.forward(partial) * .1F);
            tail1.yRot = cat.sideways(partial) * .3F;
            tail1.zRot = -cat.sideways(partial) * .08F;
            var endpoint = new org.joml.Vector3f(0, 8, 0)
                    .rotate(new org.joml.Quaternionf().rotationZYX(tail1.zRot, tail1.yRot, tail1.xRot));
            tail2.x = Mth.lerp(motion, tail2.x, tail1.x + endpoint.x);
            tail2.y = Mth.lerp(motion, tail2.y, tail1.y + endpoint.y);
            tail2.z = Mth.lerp(motion, tail2.z, tail1.z + endpoint.z);
            tail2.xRot = Mth.lerp(motion, tail2.xRot, 1.65F + cat.tipVertical(partial) * .18F);
            tail2.yRot = cat.tipSideways(partial) * .35F;
            tail2.zRot = -cat.tipSideways(partial) * .1F;
        }

        private void hold(Player player, HumanoidArm arm, float partial) {
            InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (player.getItemInHand(hand).isEmpty()) return;
            ModelPart paw = paw(arm);
            float side = arm == HumanoidArm.RIGHT ? -1 : 1;
            paw.x += side * 1.2F;
            paw.z -= 1.5F;
            paw.xRot = paw.xRot * .25F - .65F;
            paw.zRot += -side * .12F;
            if (player.swingingArm == hand) paw.xRot -= player.getAttackAnim(partial) * .75F;
            if (player.isUsingItem() && player.getUsedItemHand() == hand) {
                switch (player.getUseItem().getUseAnimation()) {
                    case EAT, DRINK -> paw.xRot = -1.2F;
                    case BLOCK, BOW, CROSSBOW, SPEAR, SPYGLASS -> paw.xRot = -1F;
                    default -> {}
                }
            }
        }
    }

    private static final class PlayerCat extends Cat {
        private final Player player;
        private int poseTick = -1;
        private float oldCrouch, crouch, oldAir, air;
        private float oldForward, forward, oldSideways, sideways;
        private float oldVertical, vertical, oldMotion, motion, oldSwim, swim, oldSpeed, speed;
        private float oldTipVertical, tipVertical, oldTipSideways, tipSideways;
        private float oldBodyYaw, bodyYaw, oldBodyPitch, bodyPitch, oldBodyRoll, bodyRoll;
        private float oldStrokePhase, strokePhase;
        private int fallingTicks;
        private PlayerCat(Player player) {
            super(EntityType.CAT, player.level());
            this.player = player;
            setUUID(player.getUUID());
            var variants = net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT;
            setVariant(variants.getHolder(Math.floorMod(player.getUUID().hashCode(), variants.size())).orElseThrow());
        }
        private void updatePose() {
            if (poseTick == player.tickCount) return;
            int ticks = poseTick < 0 ? 1 : Mth.clamp(player.tickCount - poseTick, 1, 4);
            poseTick = player.tickCount;
            oldCrouch = crouch; oldAir = air;
            oldForward = forward; oldSideways = sideways;
            oldVertical = vertical; oldMotion = motion; oldSwim = swim; oldSpeed = speed;
            oldTipVertical = tipVertical; oldTipSideways = tipSideways;
            oldBodyYaw = bodyYaw; oldBodyPitch = bodyPitch; oldBodyRoll = bodyRoll;
            oldStrokePhase = strokePhase;
            crouch = Mth.lerp(1 - (float)Math.pow(.6, ticks), crouch, player.isCrouching() ? 1 : 0);
            boolean airborne = !player.onGround() && !player.isInWaterOrBubble() && !player.onClimbable() && !player.isPassenger();
            boolean flying = player.getAbilities().flying;
            boolean falling = airborne && !flying && !player.isFallFlying() && player.getDeltaMovement().y < -.03;
            fallingTicks = falling ? Math.min(30, fallingTicks + ticks) : 0;
            float spread = airborne && flying ? .4F : falling ? .12F + .88F * fallingTicks / 30F : 0;
            float blend = 1 - (float)Math.pow(.6, ticks);
            air = Mth.lerp(blend, air, spread);
            var motion = player.getDeltaMovement();
            boolean swimming = player.isInWaterOrBubble() && (!player.onGround() || player.isSwimming()) && !player.isPassenger();
            boolean directional = airborne || swimming;
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            float along = (float)(motion.z * Mth.cos(yaw) - motion.x * Mth.sin(yaw));
            float across = (float)(motion.x * Mth.cos(yaw) + motion.z * Mth.sin(yaw));
            forward = Mth.lerp(blend, forward, directional ? Mth.clamp(along * 3, -1, 1) : 0);
            sideways = Mth.lerp(blend, sideways, directional ? Mth.clamp(across * 3, -1, 1) : 0);
            vertical = Mth.lerp(blend, vertical, directional ? Mth.clamp((float)motion.y * 2, -1, 1) : 0);
            this.motion = Mth.lerp(blend, this.motion, directional ? 1 : 0);
            swim = Mth.lerp(blend, swim, swimming ? 1 : 0);
            speed = Mth.lerp(blend, speed, Math.min(1, (float)motion.length() * 5));
            strokePhase += Math.min(.5F, (float)motion.length()) * 1.8F * ticks;
            float tailBlend = 1 - (float)Math.pow(.82, ticks);
            tipVertical = Mth.lerp(tailBlend, tipVertical, vertical);
            tipSideways = Mth.lerp(tailBlend, tipSideways, sideways);
            boolean aligning = player.isFallFlying() || swimming;
            double horizontal = motion.horizontalDistance();
            float targetYaw = aligning && horizontal > .015
                    ? Mth.wrapDegrees((float)Math.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG - player.yBodyRot) : 0;
            float targetPitch = aligning && motion.lengthSqr() > .000225
                    ? Mth.clamp((float)Math.atan2(-motion.y, horizontal) * Mth.RAD_TO_DEG, -80, 80) : 0;
            float turn = Mth.wrapDegrees(targetYaw - bodyYaw);
            float bodyBlend = 1 - (float)Math.pow(.75, ticks);
            bodyYaw = Mth.rotLerp(bodyBlend, bodyYaw, targetYaw);
            bodyPitch = Mth.lerp(bodyBlend, bodyPitch, targetPitch);
            bodyRoll = Mth.lerp(bodyBlend, bodyRoll, aligning ? Mth.clamp(-turn * .35F, -18, 18) : 0);
        }
        private float crouch(float partial) { return Mth.lerp(partial, oldCrouch, crouch); }
        private float air(float partial) { return Mth.lerp(partial, oldAir, air); }
        private float forward(float partial) { return Mth.lerp(partial, oldForward, forward); }
        private float sideways(float partial) { return Mth.lerp(partial, oldSideways, sideways); }
        private float vertical(float partial) { return Mth.lerp(partial, oldVertical, vertical); }
        private float motion(float partial) { return Mth.lerp(partial, oldMotion, motion); }
        private float swim(float partial) { return Mth.lerp(partial, oldSwim, swim); }
        private float stroke(float partial, float side) {
            return Mth.cos(Mth.lerp(partial, oldStrokePhase, strokePhase) + (side < 0 ? Mth.PI : 0))
                    * swim(partial) * Mth.lerp(partial, oldSpeed, speed);
        }
        private float tipVertical(float partial) { return Mth.lerp(partial, oldTipVertical, tipVertical); }
        private float tipSideways(float partial) { return Mth.lerp(partial, oldTipSideways, tipSideways); }
        private float bodyYaw(float partial) { return Mth.rotLerp(partial, oldBodyYaw, bodyYaw); }
        private float bodyPitch(float partial) { return Mth.lerp(partial, oldBodyPitch, bodyPitch); }
        private float bodyRoll(float partial) { return Mth.lerp(partial, oldBodyRoll, bodyRoll); }
        @Override public Component getDisplayName() { return player.getDisplayName(); }
        @Override public Component getName() { return player.getName(); }
        @Override public net.minecraft.world.scores.PlayerTeam getTeam() { return player.getTeam(); }
        @Override public boolean isInvisibleTo(Player viewer) { return player.isInvisibleTo(viewer); }
        @Override public boolean isCrouching() { return player != null && player.isCrouching(); }
        @Override public HumanoidArm getMainArm() { return player.getMainArm(); }
        @Override public float getScale() { return player == null ? 1 : player.getScale(); }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            CATS.clear();
            renderer = new CatPlayerRenderer(event.getContext());
        }
    }
}
