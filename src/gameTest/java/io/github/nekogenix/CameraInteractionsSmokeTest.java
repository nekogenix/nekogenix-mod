package io.github.nekogenix;

import io.github.nekogenix.config.ClientConfig;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CameraInteractionsSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.cameraInteractionsSmokeTest");
    private static final BlockPos HIGH = new BlockPos(0, 81, 4);
    private static final BlockPos LOW = new BlockPos(0, 80, 4);
    private static final BlockPos CLOSE = new BlockPos(0, 80, 0);
    private static boolean entityInteraction;

    private static void button(ServerPlayer player, BlockPos pos) {
        player.serverLevel().setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
        player.serverLevel().setBlockAndUpdate(pos, Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH));
    }

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age == 20) {
            for (int x = -2; x <= 8; x++) for (int z = -2; z <= 8; z++) {
                for (int y = 79; y <= 84; y++) player.serverLevel().setBlockAndUpdate(new BlockPos(x, y, z),
                        y == 79 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.teleportTo(player.serverLevel(), .5, 80, .5, 0, 0);
            button(player, HIGH);
            player.serverLevel().setBlockAndUpdate(new BlockPos(0, 80, 1), Blocks.SHORT_GRASS.defaultBlockState());
        }
        if (age == 120) CatTransformationTests.cat(player);
        if (age == 175) check(player.serverLevel().getBlockState(HIGH).getValue(ButtonBlock.POWERED), "High cat camera activates the button through short grass");
        if (age == 200) {
            CatTransformationTests.set(player, NekoState.Phase.HUMAN, 0, 0);
            player.serverLevel().setBlockAndUpdate(HIGH, Blocks.AIR.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(new BlockPos(0, 80, 1), Blocks.AIR.defaultBlockState());
            button(player, LOW);
        }
        if (age == 275) check(player.serverLevel().getBlockState(LOW).getValue(ButtonBlock.POWERED), "Low human camera activates a button near maximum reach");
        if (age == 300) { player.serverLevel().setBlockAndUpdate(LOW, Blocks.AIR.defaultBlockState()); button(player, CLOSE); }
        if (age == 345) check(player.serverLevel().getBlockState(CLOSE).getValue(ButtonBlock.POWERED), "A close low button accepts interaction");
        if (age == 395) check(player.serverLevel().getBlockState(CLOSE).isAir(), "Native sustained attack breaks the close low button");
        if (age == 440) {
            player.serverLevel().setBlockAndUpdate(CLOSE.south(), Blocks.AIR.defaultBlockState());
            player.serverLevel().setBlockAndUpdate(LOW.south(), Blocks.AIR.defaultBlockState());
            button(player, LOW.south());
        }
        if (age == 480) {
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(player.serverLevel(), .5, 80, 2.5);
            player.serverLevel().addFreshEntity(stand);
        }
        if (age == 545) {
            check(entityInteraction, "Native use input reaches the server for an entity");
            Nekogenix.LOGGER.info("Nekogenix camera interactions server smoke passed: grass distant and close buttons native breaking and entity use");
        }
    }

    @SubscribeEvent public static void entity(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
        if (ENABLED && !event.getLevel().isClientSide && event.getTarget() instanceof net.minecraft.world.entity.decoration.ArmorStand) entityInteraction = true;
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            int age = mc.player.tickCount;
            mc.player.setYRot(0); mc.player.yRotO = 0; mc.player.setXRot(0); mc.player.xRotO = 0;
            mc.getToasts().clear();
            if (age == 60) fov();
            if (age == 145) check(block().equals(HIGH) && mc.player.getData(Content.CAT_FORM), "A high cat camera selects past short grass");
            if (age == 220) ClientConfig.CAMERA_HEIGHT.set(0);
            if (age == 250) {
                check(!mc.player.getData(Content.CAT_FORM) && block().equals(LOW), "A low human camera selects at full block range");
                check(Math.abs(mc.player.getBbHeight() - 1.8F) < .001, "The low camera does not shrink the human body");
            }
            if (age == 325) check(block().equals(CLOSE), "A low human camera selects the adjacent low block");
            if (age == 160 || age == 260 || age == 330 || age == 520) mc.options.keyUse.setDown(true);
            if (age == 161 || age == 261 || age == 331 || age == 521) mc.options.keyUse.setDown(false);
            if (age == 350) mc.options.keyAttack.setDown(true);
            if (age == 390) mc.options.keyAttack.setDown(false);
            if (age == 430) check(block().equals(CLOSE.south()), "Solid walls still block targets behind them");
            if (age == 460) check(mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS, "Targets beyond native reach remain out of range");
            if (age == 510) check(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult, "Entity selection also follows the low viewpoint");
            if (age == 560) {
                Nekogenix.LOGGER.info("Nekogenix camera interactions client smoke passed: grass close and far targets normal body dimensions wall occlusion reach boundary entities and agility FOV scaling");
                mc.stop();
            }
        }

        private static BlockPos block() {
            var hit = net.minecraft.client.Minecraft.getInstance().hitResult;
            check(hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK, "A block is selected instead of an erroneous miss");
            return ((net.minecraft.world.phys.BlockHitResult)hit).getBlockPos();
        }

        private static void fov() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            double previous = mc.options.fovEffectScale().get();
            var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            player.setSprinting(true);
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(Content.id("feline_sprint"), 1,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            for (double scale : new double[] {1, .5, 0}) {
                mc.options.fovEffectScale().set(scale);
                double stored = mc.options.fovEffectScale().get();
                player.removeEffect(Content.AGILITY);
                float ordinary = player.getFieldOfViewModifier();
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Content.AGILITY, -1));
                float feline = player.getFieldOfViewModifier();
                check(Math.abs(feline - (1 + (ordinary - 1) * .3)) < .00001, "Feline sprint FOV is thirty percent at accessibility value " + scale);
                check(Double.compare(mc.options.fovEffectScale().get(), stored) == 0, "The accessibility preference is unchanged");
            }
            player.removeEffect(Content.AGILITY); player.setSprinting(false); speed.removeModifier(Content.id("feline_sprint"));
            mc.options.fovEffectScale().set(previous);
            Nekogenix.LOGGER.info("Nekogenix agility FOV smoke passed: 100 percent becomes 30 50 becomes 15 zero stays zero and settings are unchanged");
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Camera interactions smoke failed: " + message);
    }
}
