package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.centrifuge.CentrifugeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    private static final ResourceLocation TEXTURE = Content.id("textures/gui/centrifuge.png");
    private static final ResourceLocation EMPTY_SAMPLE = Content.id("textures/gui/centrifuge_empty_sample.png");
    private static final ResourceLocation FULL_SAMPLE = Content.id("textures/item/bottle_of_cat_saliva.png");
    private static final ResourceLocation BLANK_CARD = Content.id("textures/item/blank_nucleocard.png");
    private static final ResourceLocation FULL_CARD = Content.id("textures/item/cat_nucleocard.png");

    public CentrifugeScreen(CentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 266;
        inventoryLabelY = 172;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        float fraction = menu.fraction();
        // A small illuminated helix fills alongside the card below the rotor.
        for (int row = 0; row < 14; row++) {
            double phase = row * .65 + (menu.running() ? minecraft.level.getGameTime() * .08 : 0);
            int x = 88 + (int) (Math.sin(phase) * 9);
            int opposite = 176 - x;
            int color = row >= 14 * (1 - fraction) ? 0xffd4a6cf : 0xff526775;
            graphics.fill(leftPos + Math.min(x, opposite), topPos + 56 + row * 2,
                    leftPos + Math.max(x, opposite) + 2, topPos + 57 + row * 2, 0xff667985);
            graphics.fill(leftPos + x, topPos + 56 + row * 2, leftPos + x + 2, topPos + 58 + row * 2, color);
            graphics.fill(leftPos + opposite, topPos + 56 + row * 2, leftPos + opposite + 2, topPos + 58 + row * 2, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        Component status;
        if (menu.running()) status = Component.translatable("container.nekogenix.extracting", Math.round(menu.fraction() * 100));
        else if (menu.getSlot(6).getItem().is(Content.CAT_NUCLEOCARD)) status = Component.translatable("container.nekogenix.ready");
        else status = Component.translatable("container.nekogenix.samples");
        graphics.drawCenteredString(font, status, leftPos + 88, topPos + 126, 0xff303c4b);
        renderTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            String key = hoveredSlot.index < 6 ? "sample_slot" : hoveredSlot.index == 6 ? "card_slot" : hoveredSlot.index == 7 ? "fuel_slot" : null;
            if (key != null) graphics.renderTooltip(font, Component.translatable("container.nekogenix." + key), mouseX, mouseY);
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics graphics, ItemStack item, net.minecraft.world.inventory.Slot slot, String count) {
        if (menu.running() && slot.index < 6) {
            sprite(graphics, EMPTY_SAMPLE, slot.x, slot.y);
            clipSprite(graphics, FULL_SAMPLE, slot.x, slot.y, 1 - menu.fraction(), 6, 15);
        } else if (menu.running() && slot.index == 6) {
            sprite(graphics, BLANK_CARD, slot.x, slot.y);
            clipSprite(graphics, FULL_CARD, slot.x, slot.y, menu.fraction(), 5, 12);
        } else super.renderSlotContents(graphics, item, slot, count);
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }

    private void clipSprite(GuiGraphics graphics, ResourceLocation texture, int x, int y, float fill, int top, int bottom) {
        int height = Math.max(0, Math.min(bottom - top, Math.round(fill * (bottom - top))));
        if (height == 0) return;
        graphics.enableScissor(leftPos + x, topPos + y + bottom - height, leftPos + x + 16, topPos + y + bottom);
        sprite(graphics, texture, x, y);
        graphics.disableScissor();
    }
}
