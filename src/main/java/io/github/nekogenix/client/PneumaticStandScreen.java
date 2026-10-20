package io.github.nekogenix.client;

import io.github.nekogenix.Content;
import io.github.nekogenix.client.recipe.ProcessingPage;
import io.github.nekogenix.pneumatic.PneumaticStandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PneumaticStandScreen extends AbstractContainerScreen<PneumaticStandMenu> {
    private static final ResourceLocation TEXTURE = Content.id("textures/gui/pneumatic_stand.png");
    private static final ResourceLocation SYRINGE_HINT = Content.id("textures/gui/pneumatic_syringe_slot.png");
    private static final ResourceLocation FUEL_HINT = Content.id("textures/gui/pneumatic_fuel_slot.png");

    public PneumaticStandScreen(PneumaticStandMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        for (int index = 0; index < 4; index++) {
            var slot = menu.getSlot(index);
            if (index != PneumaticStandMenu.CARD_SLOT && !slot.hasItem()) graphics.blit(index < 2 ? SYRINGE_HINT : FUEL_HINT,
                    leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
        if (!menu.running()) return;
        ProcessingPage.pneumaticDetails(graphics, leftPos, topPos,
                menu.fraction(), minecraft.level.getGameTime());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
