package io.github.nekogenix.pneumatic;

import io.github.nekogenix.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PneumaticStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int CARD = 3;
    public static final int FUEL = 4;
    public static final int SIZE = 5;
    public static final int PROCESS_TICKS = 400;
    private static final int[] SLOTS = {0, 1, 2, CARD, FUEL};
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private int target = -1;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : target; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else target = value; }
        @Override public int getCount() { return 2; }
    };

    public PneumaticStandBlockEntity(BlockPos pos, BlockState state) { super(Content.PNEUMATIC_ENTITY.get(), pos, state); }
    public boolean running() { return progress > 0; }
    public int progress() { return progress; }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumaticStandBlockEntity stand) {
        if (!stand.running()) {
            if (!stand.getItem(CARD).is(Content.CAT_NUCLEOCARD) || !stand.getItem(FUEL).is(Items.WIND_CHARGE)) return;
            for (int slot = 0; slot < 3; slot++) {
                if (!stand.getItem(slot).is(Content.SYRINGE)) continue;
                stand.target = slot;
                stand.progress = 1;
                stand.items.get(FUEL).shrink(1);
                stand.sound(SoundEvents.BREEZE_INHALE, .55F, 1.2F);
                stand.sync();
                return;
            }
        } else if (stand.target < 0 || !stand.getItem(stand.target).is(Content.SYRINGE) || !stand.getItem(CARD).is(Content.CAT_NUCLEOCARD)) {
            stand.progress = 0;
            stand.target = -1;
            stand.sound(SoundEvents.BREEZE_SLIDE, .35F, 1.3F);
            stand.sync();
        } else if (++stand.progress >= PROCESS_TICKS) {
            stand.items.set(stand.target, Content.INJECTION.toStack());
            stand.items.set(CARD, Content.BLANK_NUCLEOCARD.toStack());
            stand.progress = 0;
            stand.target = -1;
            stand.sound(SoundEvents.BREEZE_SHOOT, .55F, 1.4F);
            stand.sound(SoundEvents.AMETHYST_BLOCK_CHIME, .45F, 1.5F);
            stand.sync();
        } else {
            stand.setChanged();
            if (stand.progress % 20 == 0) stand.sync();
            if (stand.progress % 80 == 0) stand.sound(SoundEvents.BREEZE_IDLE_AIR, .18F, 1.3F);
        }
    }

    private void sound(SoundEvent sound, float volume, float pitch) { level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch); }
    private void sync() { super.setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !running()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.nekogenix.pneumatic_stand"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new PneumaticStandMenu(id, inventory, this, data); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.is(Items.WIND_CHARGE) ? 64 : 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < CARD ? stack.is(Content.SYRINGE) : slot == CARD ? stack.is(Content.CAT_NUCLEOCARD) : slot == FUEL && stack.is(Items.WIND_CHARGE);
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < CARD ? stack.is(Content.INJECTION) : slot == CARD && stack.is(Content.BLANK_NUCLEOCARD);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
        tag.putInt("Target", target);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = Mth.clamp(tag.getInt("Progress"), 0, PROCESS_TICKS - 1);
        target = tag.contains("Target") ? Mth.clamp(tag.getInt("Target"), -1, 2) : -1;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
