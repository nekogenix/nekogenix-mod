package io.github.nekogenix.integration.jei;

import io.github.nekogenix.Content;
import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

final class PneumaticCategory implements IRecipeCategory<NekogenixJei.Filling> {
    private final IDrawable icon, arrow;

    PneumaticCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(Content.PNEUMATIC_ITEM);
        arrow = gui.createAnimatedRecipeArrow(PneumaticStandBlockEntity.PROCESS_TICKS);
    }
    @Override public RecipeType<NekogenixJei.Filling> getRecipeType() { return NekogenixJei.PNEUMATIC_FILLING; }
    @Override public Component getTitle() { return Component.translatable("block.nekogenix.pneumatic_stand"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 112; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, NekogenixJei.Filling filling, IFocusGroup focuses) {
        var recipe = filling.recipe();
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 6).setStandardSlotBackground().setSlotName("fuel")
                .addItemStack(Items.WIND_CHARGE.getDefaultInstance());
        builder.addSlot(RecipeIngredientRole.INPUT, 58, 6).setStandardSlotBackground().setSlotName("source").addIngredients(recipe.source());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 6).setStandardSlotBackground().setSlotName("remainder")
                .addItemStack(recipe.remainder()).addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.nekogenix.returned")));
        for (int slot = 0; slot < filling.syringes(); slot++) {
            int y = filling.syringes() == 1 ? 62 : 49 + slot * 29;
            builder.addSlot(RecipeIngredientRole.INPUT, 28, y).setStandardSlotBackground().setSlotName("syringe_" + slot)
                    .addItemStack(Content.SYRINGE.toStack());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 132, y).setOutputSlotBackground().setSlotName("injection_" + slot)
                    .addItemStack(recipe.result());
        }
    }

    @Override public void draw(NekogenixJei.Filling filling, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 88, filling.syringes() == 1 ? 62 : 63);
        var time = Component.translatable("jei.nekogenix.duration", PneumaticStandBlockEntity.PROCESS_TICKS / 20);
        graphics.drawString(Minecraft.getInstance().font, time, 88, 98, 0xFF555555, false);
    }
}
