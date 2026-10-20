package io.github.nekogenix.integration.jei;

import io.github.nekogenix.client.recipe.ProcessingDisplay;
import io.github.nekogenix.client.recipe.ProcessingPage;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class ProcessingCategory implements IRecipeCategory<ProcessingDisplay> {
    private final ProcessingDisplay.Machine machine;
    private final IDrawable icon;
    ProcessingCategory(IGuiHelper gui, ProcessingDisplay.Machine machine) {
        this.machine = machine;
        icon = gui.createDrawableItemStack(machine.icon());
    }
    @Override public RecipeType<ProcessingDisplay> getRecipeType() { return machine == ProcessingDisplay.Machine.CENTRIFUGE ? NekogenixJei.CENTRIFUGING : NekogenixJei.PNEUMATIC_FILLING; }
    @Override public Component getTitle() { return machine.title(); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return machine.width(); }
    @Override public int getHeight() { return machine.height(); }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ProcessingDisplay display, IFocusGroup focuses) {
        for (var slot : display.slots()) {
            var target = builder.addSlot(slot.role() == ProcessingDisplay.Role.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT,
                    slot.x(), slot.y()).setSlotName(slot.name()).addItemStacks(slot.items());
            if (slot.returned()) target.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("recipe.nekogenix.returned")));
        }
    }
    @Override public void draw(ProcessingDisplay display, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ProcessingPage.draw(graphics, display);
    }
}
