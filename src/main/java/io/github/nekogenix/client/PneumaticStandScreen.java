package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.pneumatic.PneumaticStandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PneumaticStandScreen extends AbstractContainerScreen<PneumaticStandMenu> {
    private static final ResourceLocation TEXTURE = Content.id("textures/gui/pneumatic_stand.png");
    private static final ResourceLocation CARD_HINT = Content.id("textures/gui/pneumatic_card_slot.png");
    private static final ResourceLocation SYRINGE_HINT = Content.id("textures/gui/pneumatic_syringe_slot.png");
    private static final ResourceLocation FUEL_HINT = Content.id("textures/gui/pneumatic_fuel_slot.png");

    public PneumaticStandScreen(PneumaticStandMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        for (int index = 0; index < 4; index++) {
            var slot = menu.getSlot(index);
            if (!slot.hasItem()) graphics.blit(index < 2 ? SYRINGE_HINT : index == PneumaticStandMenu.CARD_SLOT ? CARD_HINT : FUEL_HINT,
                    leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
        if (!menu.running()) return;
        int pressure = Math.round(20 * (1 - menu.fraction()));
        graphics.fill(leftPos + 18, topPos + 39, leftPos + 18 + pressure, topPos + 42, 0xffa8d9dc);
        int height = Math.round(22 * menu.fraction());
        graphics.fill(leftPos + 101, topPos + 18, leftPos + 103, topPos + 18 + height, 0xffb9dce1);
        int pulse = (int) (minecraft.level.getGameTime() % 16);
        for (int i = 0; i < 3; i++) {
            int y = 20 + (pulse + i * 5) % 18;
            graphics.fill(leftPos + 51 + i % 2 * 3, topPos + y, leftPos + 53 + i % 2 * 3, topPos + y + 1, 0xffe6f3ef);
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
