package io.github.nekogenix.centrifuge;

import io.github.nekogenix.Content;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CentrifugeMenu extends AbstractContainerMenu {
    public static final int[][] BOTTLE_POSITIONS = {{80, 30}, {114, 49}, {114, 87}, {80, 106}, {46, 87}, {46, 49}};
    private final Container container;
    private final ContainerData data;

    public CentrifugeMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(8), new SimpleContainerData(1)); }
    public CentrifugeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Content.CENTRIFUGE_MENU.get(), id);
        checkContainerSize(container, 8);
        checkContainerDataCount(data, 1);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);
        for (int slot = 0; slot < 6; slot++) addMachineSlot(slot, BOTTLE_POSITIONS[slot][0], BOTTLE_POSITIONS[slot][1]);
        addMachineSlot(6, 80, 143);
        addMachineSlot(7, 24, 143);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 184 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 242));
        addDataSlots(data);
    }

    private void addMachineSlot(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !running() && (index < 6 ? stack.is(Content.SALIVA_BOTTLE)
                        : index == 6 ? stack.is(Content.BLANK_NUCLEOCARD) : stack.is(Items.REDSTONE));
            }
            @Override public boolean mayPickup(Player player) { return !running(); }
            @Override public int getMaxStackSize() { return index == 7 ? 64 : 1; }
        });
    }

    public boolean running() { return data.get(0) > 0; }
    public boolean isContainer(Container target) { return container == target; }
    public float fraction() { return data.get(0) / (float) CentrifugeBlockEntity.PROCESS_TICKS; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void clicked(int index, int button, ClickType type, Player player) { if (!running()) super.clicked(index, button, type, player); }
    @Override public boolean canDragTo(Slot slot) { return !running() && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return !running() && super.canTakeItemForPickAll(stack, slot); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (running() || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < 8) moved = moveItemStackTo(stack, 8, 44, true);
        else if (stack.is(Content.SALIVA_BOTTLE)) moved = moveItemStackTo(stack, 0, 6, false);
        else if (stack.is(Content.BLANK_NUCLEOCARD)) moved = moveItemStackTo(stack, 6, 7, false);
        else if (stack.is(Items.REDSTONE)) moved = moveItemStackTo(stack, 7, 8, false);
        else moved = index < 35 ? moveItemStackTo(stack, 35, 44, false) : moveItemStackTo(stack, 8, 35, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
