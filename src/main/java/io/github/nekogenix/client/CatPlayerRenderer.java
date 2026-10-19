package io.github.nekogenix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.gameplay.CatForm;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
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
        model = new PawModel(context.bakeLayer(ModelLayers.CAT));
        items = context.getItemInHandRenderer();
        ModelPart paws = context.bakeLayer(ModelLayers.CAT);
        rightPaw = paws.getChild("right_front_leg");
        leftPaw = paws.getChild("left_front_leg");
        addLayer(new RenderLayer<Cat, CatModel<Cat>>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Cat cat, float walk,
                    float speed, float partial, float age, float yaw, float pitch) {
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
        pose.translate(0, .45, .02);
        ItemDisplayContext context = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        float modelYaw = Minecraft.getInstance().getItemRenderer().getModel(stack, cat.level(), cat, 0)
                .getTransforms().getTransform(context).rotation.y;
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees((left ? 1 : -1) * modelYaw));
        pose.mulPose(Axis.ZP.rotationDegrees((left ? -1 : 1) * cat.getAttackAnim(partial) * 40));
        pose.scale(.55F, .55F, .55F);
        items.renderItem(cat, stack, context, left, pose, buffers, light);
        pose.popPose();
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
        // The visual cat never enters the world; the player remains authoritative for movement and inventory.
        if (cat.lastWalkTick != player.tickCount) {
            cat.walkAnimation.update(player.walkAnimation.speed(), 1F);
            cat.lastWalkTick = player.tickCount;
        }
        renderer.render(cat, Mth.rotLerp(partial, player.yRotO, player.getYRot()), partial,
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (renderer == null || player == null || !CatForm.active(player) || player.isSpectator()) return;
        event.setCanceled(true);
        boolean main = event.getHand() == InteractionHand.MAIN_HAND;
        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean left = arm == HumanoidArm.LEFT;
        float side = left ? -1 : 1;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float swing = Mth.sin(Mth.sqrt(event.getSwingProgress()) * Mth.PI);
        pose.translate(side * (.42 - swing * .12), -.42 - event.getEquipProgress() * .55 + swing * .12, -.65);
        if (player.isUsingItem() && player.getUsedItemHand() == event.getHand()) {
            float use = player.getUseItemRemainingTicks() - event.getPartialTick();
            switch (event.getItemStack().getUseAnimation()) {
                case EAT, DRINK -> {
                    pose.translate(-side * .18, .14 + Math.abs(Mth.cos(use * .65F)) * .025, .12);
                    pose.mulPose(Axis.ZP.rotationDegrees(side * 20));
                }
                case BLOCK -> pose.mulPose(Axis.YP.rotationDegrees(side * 35));
                case BOW, CROSSBOW, SPEAR -> pose.translate(-side * .14, .08, .12);
                default -> {}
            }
        }
        if (!player.isInvisible()) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(side * -12));
            pose.scale(1.15F, 1.15F, 1.15F);
            ModelPart paw = left ? renderer.leftPaw : renderer.rightPaw;
            paw.resetPose();
            paw.setPos(0, 0, 0);
            paw.render(pose, event.getMultiBufferSource().getBuffer(RenderType.entityCutout(proxy(player).getTextureId())),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.translate(0, .04, -.12);
        pose.mulPose(Axis.YP.rotationDegrees(side * 40));
        pose.mulPose(Axis.ZP.rotationDegrees(side * (-10 - swing * 25)));
        pose.scale(.8F, .8F, .8F);
        renderer.items.renderItem(player, event.getItemStack(), left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                left, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof Player player) CATS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CATS.clear(); }

    private static final class PawModel extends CatModel<Cat> {
        private PawModel(ModelPart root) { super(root); }
        private ModelPart paw(HumanoidArm arm) { return arm == HumanoidArm.LEFT ? leftFrontLeg : rightFrontLeg; }
    }

    private static final class PlayerCat extends Cat {
        private final Player player;
        private int lastWalkTick = -1;
        private PlayerCat(Player player) {
            super(EntityType.CAT, player.level());
            this.player = player;
            setUUID(player.getUUID());
            var variants = net.minecraft.core.registries.BuiltInRegistries.CAT_VARIANT;
            setVariant(variants.getHolder(Math.floorMod(player.getUUID().hashCode(), variants.size())).orElseThrow());
        }
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
