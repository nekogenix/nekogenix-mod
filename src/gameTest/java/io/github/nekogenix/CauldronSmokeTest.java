package io.github.nekogenix;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CauldronSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.cauldronSmokeTest");
    private static final BlockPos CAULDRON = new BlockPos(10, -60, 8);

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var level = player.serverLevel();
        if (player.tickCount == 40) {
            level.setDayTime(6000);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 10.5, -58.7, 6.2, 0, 43);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(CAULDRON).inflate(2)).forEach(ItemEntity::discard);
            level.setBlockAndUpdate(CAULDRON.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(CAULDRON, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        }
        if (player.tickCount == 80) {
            ItemEntity syringe = new ItemEntity(level, CAULDRON.getX() + .5, CAULDRON.getY() + .5,
                    CAULDRON.getZ() + .5, Content.USED_SYRINGE.toStack());
            syringe.setNoGravity(true);
            syringe.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(syringe);
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new IllegalStateException("Nekogenix cauldron smoke failed: " + message);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static boolean bubbling;

        @SubscribeEvent
        public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (ENABLED && event.getSound() != null && event.getSound().getLocation()
                    .equals(net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_BUBBLE_POP.getLocation())) bubbling = true;
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 45) mc.options.fov().set(35);
            if (age == 120 || age == 180 || age == 250) {
                int expected = age == 120 ? 3 : age == 180 ? 2 : 1;
                var state = mc.level.getBlockState(CAULDRON);
                check(state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == expected,
                        "The client receives successive water levels " + expected);
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "cauldron-level-" + expected + ".png",
                        mc.getMainRenderTarget(), message -> {});
            }
            if (age == 310) {
                check(mc.level.getBlockState(CAULDRON).is(Blocks.CAULDRON), "Water finishes evaporating");
                var items = mc.level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(CAULDRON).inflate(1.2));
                check(items.stream().anyMatch(item -> item.getItem().is(Content.SYRINGE))
                        && items.stream().noneMatch(item -> item.getItem().is(Content.USED_SYRINGE)), "Only the completed cycle cleans the syringe");
                check(bubbling, "Vanilla bubbling sound reaches the client");
                Nekogenix.LOGGER.info("Nekogenix cauldron smoke passed: three water levels, completed cleaning and bubbling sound");
            }
            if (age >= 320) mc.stop();
        }
    }
}
