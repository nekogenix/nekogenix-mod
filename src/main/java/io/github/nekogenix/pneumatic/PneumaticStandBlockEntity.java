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
import net.minecraft.world.Containers;
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
    // Keep saved slot identifiers; slot 1 only holds items from the former center slot.
    private static final int[] SLOTS = {0, 2, CARD, FUEL};
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private int targets;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : targets; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else targets = value; }
        @Override public int getCount() { return 2; }
    };

    public PneumaticStandBlockEntity(BlockPos pos, BlockState state) { super(Content.PNEUMATIC_ENTITY.get(), pos, state); }
    public boolean running() { return progress > 0; }
    public int progress() { return progress; }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumaticStandBlockEntity stand) {
        if (!stand.running()) {
            stand.returnCenterItem();
            if (!stand.getItem(CARD).is(Content.CAT_NUCLEOCARD) || !stand.getItem(FUEL).is(Items.WIND_CHARGE)) return;
            stand.targets = (stand.getItem(0).is(Content.SYRINGE) ? 1 : 0) | (stand.getItem(2).is(Content.SYRINGE) ? 4 : 0);
            if (stand.targets == 0) return;
            stand.progress = 1;
            stand.items.get(FUEL).shrink(1);
            stand.sound(SoundEvents.BREEZE_INHALE, .55F, 1.2F);
            stand.sync();
        } else if (!stand.validInputs()) {
            stand.progress = 0;
            stand.targets = 0;
            stand.returnCenterItem();
            stand.sound(SoundEvents.BREEZE_SLIDE, .35F, 1.3F);
            stand.sync();
        } else if (++stand.progress >= PROCESS_TICKS) {
            for (int slot = 0; slot < 3; slot++) if ((stand.targets & 1 << slot) != 0) stand.items.set(slot, Content.INJECTION.toStack());
            stand.items.set(CARD, Content.BLANK_NUCLEOCARD.toStack());
            stand.progress = 0;
            stand.targets = 0;
            stand.returnCenterItem();
            stand.sound(SoundEvents.BREEZE_SHOOT, .55F, 1.4F);
            stand.sound(SoundEvents.AMETHYST_BLOCK_CHIME, .45F, 1.5F);
            stand.sync();
        } else {
            stand.setChanged();
            if (stand.progress % 20 == 0) stand.sync();
            if (stand.progress % 80 == 0) stand.sound(SoundEvents.BREEZE_IDLE_AIR, .18F, 1.3F);
        }
    }

    private boolean validInputs() {
        if (targets == 0 || !getItem(CARD).is(Content.CAT_NUCLEOCARD)) return false;
        for (int slot = 0; slot < 3; slot++) if ((targets & 1 << slot) != 0 && !getItem(slot).is(Content.SYRINGE)) return false;
        return true;
    }

    private void returnCenterItem() {
        ItemStack previous = items.get(1);
        if (previous.isEmpty()) return;
        for (int slot : new int[] {0, 2}) {
            if (!items.get(slot).isEmpty()) continue;
            items.set(slot, previous);
            items.set(1, ItemStack.EMPTY);
            sync();
            return;
        }
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, previous);
        items.set(1, ItemStack.EMPTY);
        sync();
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
        return slot == 0 || slot == 2 ? stack.is(Content.SYRINGE) : slot == CARD ? stack.is(Content.CAT_NUCLEOCARD) : slot == FUEL && stack.is(Items.WIND_CHARGE);
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 || slot == 2 ? stack.is(Content.INJECTION) : slot == CARD && stack.is(Content.BLANK_NUCLEOCARD);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
        tag.putInt("Targets", targets);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = Mth.clamp(tag.getInt("Progress"), 0, PROCESS_TICKS - 1);
        int previousTarget = tag.contains("Target") ? tag.getInt("Target") : -1;
        targets = tag.contains("Targets") ? tag.getInt("Targets") & 7
                : running() && previousTarget >= 0 && previousTarget < 3 ? 1 << previousTarget : 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
