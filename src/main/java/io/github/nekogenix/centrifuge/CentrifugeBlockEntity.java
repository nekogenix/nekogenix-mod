package io.github.nekogenix.centrifuge;

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
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CentrifugeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int BOTTLES = 6;
    public static final int CARD = 6;
    public static final int FUEL = 7;
    public static final int SIZE = 8;
    public static final int PROCESS_TICKS = 600;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, CARD, FUEL};
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private boolean open;
    private float lid, oldLid;
    private float rotor, oldRotor, speed;
    private float displayProgress;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return progress; }
        @Override public void set(int index, int value) { progress = value; }
        @Override public int getCount() { return 1; }
    };

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) { super(Content.CENTRIFUGE_ENTITY.get(), pos, state); }
    public boolean running() { return progress > 0; }
    public int progress() { return progress; }
    public float fraction(float partial) { return Mth.clamp((displayProgress + (running() ? partial : 0)) / PROCESS_TICKS, 0, 1); }
    public float lid(float partial) { float value = Mth.lerp(partial, oldLid, lid); return 1 - (float) Math.pow(1 - value, 3); }
    public float rotation(float partial) { return Mth.lerp(partial, oldRotor, rotor); }

    public static void tick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity machine) {
        if (level.isClientSide) {
            machine.oldLid = machine.lid;
            machine.lid = Mth.clamp(machine.lid + (machine.open && !machine.running() ? .1F : -.15F), 0, 1);
            machine.oldRotor = machine.rotor;
            float target = machine.running() ? 32F * Mth.clamp(machine.displayProgress / 20, 0, 1)
                    * Mth.clamp((PROCESS_TICKS - machine.displayProgress) / 20, 0, 1) : 0;
            machine.speed = Mth.approach(machine.speed, target, 2F);
            machine.rotor += machine.speed;
            if (machine.rotor >= 360) { machine.rotor -= 360; machine.oldRotor -= 360; }
            if (machine.running()) machine.displayProgress = Math.min(PROCESS_TICKS, machine.displayProgress + 1);
            return;
        }
        if (!machine.running()) {
            if (!machine.ready()) return;
            machine.items.get(FUEL).shrink(1);
            machine.progress = 1;
            machine.open = false;
            machine.sound(SoundEvents.IRON_TRAPDOOR_CLOSE, .7F, .8F);
            machine.sync();
        } else if (++machine.progress >= PROCESS_TICKS) {
            for (int slot = 0; slot < BOTTLES; slot++) machine.items.set(slot, Items.GLASS_BOTTLE.getDefaultInstance());
            machine.items.set(CARD, Content.CAT_NUCLEOCARD.toStack());
            machine.progress = 0;
            machine.open = true;
            machine.sound(SoundEvents.AMETHYST_BLOCK_CHIME, .8F, 1.2F);
            machine.sound(SoundEvents.IRON_TRAPDOOR_OPEN, .7F, 1F);
            machine.sync();
        } else {
            machine.setChanged();
            if (machine.progress % 20 == 0) machine.sync();
            if (machine.progress % 60 == 0) machine.sound(SoundEvents.BEACON_AMBIENT, .22F, .8F + machine.progress / 1500F);
        }
    }

    private boolean ready() {
        if (!getItem(CARD).is(Content.BLANK_NUCLEOCARD) || !getItem(FUEL).is(Items.REDSTONE)) return false;
        for (int slot = 0; slot < BOTTLES; slot++) if (!getItem(slot).is(Content.SALIVA_BOTTLE) || getItem(slot).getCount() != 1) return false;
        return true;
    }

    private void sound(SoundEvent sound, float volume, float pitch) { level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch); }
    private void sync() { super.setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override public void startOpen(Player player) {
        if (level != null && !level.isClientSide && !player.isSpectator() && !running() && !open) {
            open = true;
            sound(SoundEvents.IRON_TRAPDOOR_OPEN, .6F, 1F);
            sync();
        }
    }
    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !running()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.nekogenix.centrifuge"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CentrifugeMenu(id, inventory, this, data); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.is(Items.REDSTONE) ? 64 : 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return !running() && (slot < BOTTLES ? stack.is(Content.SALIVA_BOTTLE)
                : slot == CARD ? stack.is(Content.BLANK_NUCLEOCARD) : slot == FUEL && stack.is(Items.REDSTONE));
    }
    @Override public boolean canTakeItem(Container target, int slot, ItemStack stack) { return !running(); }
    @Override public ItemStack removeItem(int slot, int count) { return running() ? ItemStack.EMPTY : super.removeItem(slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return running() ? ItemStack.EMPTY : super.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { if (!running()) super.setItem(slot, stack); }
    @Override public void clearContent() { if (!running()) { super.clearContent(); setChanged(); } }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return !running(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Progress", progress);
        tag.putBoolean("Open", open);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, provider);
        progress = Mth.clamp(tag.getInt("Progress"), 0, PROCESS_TICKS - 1);
        displayProgress = progress;
        open = tag.getBoolean("Open");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
