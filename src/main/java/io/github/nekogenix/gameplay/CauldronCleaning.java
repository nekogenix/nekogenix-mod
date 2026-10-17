package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CauldronCleaning {
    private record Cycle(long finish, BlockState water) {}
    private static final Map<ServerLevel, Map<BlockPos, Cycle>> CYCLES = new WeakHashMap<>();

    public static boolean heated(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)
                || (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) && state.getValue(CampfireBlock.LIT);
    }

    @SubscribeEvent
    public static void itemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)
                || !item.getItem().is(Content.USED_SYRINGE) || item.tickCount % 5 != 0) return;
        BlockPos pos = item.blockPosition();
        BlockState water = level.getBlockState(pos);
        if (water.is(Blocks.WATER_CAULDRON) && heated(level.getBlockState(pos.below()))) {
            CYCLES.computeIfAbsent(level, key -> new HashMap<>()).computeIfAbsent(pos.immutable(),
                    key -> new Cycle(level.getGameTime() + 200, water));
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Map<BlockPos, Cycle> cycles = CYCLES.get(level);
        if (cycles == null) return;
        var iterator = cycles.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();
            Cycle cycle = entry.getValue();
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).equals(cycle.water())
                    || !heated(level.getBlockState(pos.below()))) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() % 20 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.9,
                        pos.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.01);
                level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4F, 1F);
            }
            if (level.getGameTime() < cycle.finish()) continue;
            if (cycle.water().getValue(LayeredCauldronBlock.LEVEL) == 3) {
                var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos),
                        item -> item.isAlive() && item.getItem().is(Content.USED_SYRINGE));
                if (!items.isEmpty()) {
                    ItemEntity dirty = items.getFirst();
                    dirty.getItem().shrink(1);
                    if (dirty.getItem().isEmpty()) dirty.discard();
                    ItemEntity clean = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.9,
                            pos.getZ() + 0.5, Content.SYRINGE.toStack());
                    clean.setDeltaMovement(0, 0.1, 0);
                    clean.setDefaultPickUpDelay();
                    level.addFreshEntity(clean);
                }
            }
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1,
                    pos.getZ() + 0.5, 12, 0.25, 0.15, 0.25, 0.02);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
            iterator.remove();
        }
    }

    private CauldronCleaning() {}
}
