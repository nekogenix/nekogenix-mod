package io.github.nekogenix.integration.jei;

import io.github.nekogenix.Content;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.recipe.CentrifugeRecipe;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CentrifugeCategory implements IRecipeCategory<CentrifugeRecipe> {
    private static final int[][] SAMPLES = {{40, 6}, {65, 23}, {65, 56}, {40, 73}, {15, 56}, {15, 23}};
    private final IDrawable icon, arrow;

    CentrifugeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(Content.CENTRIFUGE_ITEM);
        arrow = gui.createAnimatedRecipeArrow(CentrifugeBlockEntity.PROCESS_TICKS);
    }
    @Override public RecipeType<CentrifugeRecipe> getRecipeType() { return NekogenixJei.CENTRIFUGING; }
    @Override public Component getTitle() { return Component.translatable("block.nekogenix.centrifuge"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 160; }
    @Override public int getHeight() { return 138; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeRecipe recipe, IFocusGroup focuses) {
        for (int slot = 0; slot < SAMPLES.length; slot++) {
            builder.addSlot(RecipeIngredientRole.INPUT, SAMPLES[slot][0], SAMPLES[slot][1])
                    .setStandardSlotBackground().setSlotName("sample_" + slot).addIngredients(recipe.sample());
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 108).setStandardSlotBackground().setSlotName("card").addIngredients(recipe.card());
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 108).setStandardSlotBackground().setSlotName("fuel")
                .addItemStack(new ItemStack(Items.REDSTONE, CentrifugeBlockEntity.REDSTONE_PER_CARD));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 40).setOutputSlotBackground().setSlotName("result").addItemStack(recipe.result());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 81).setStandardSlotBackground().setSlotName("bottles")
                .addItemStack(new ItemStack(Items.GLASS_BOTTLE, CentrifugeBlockEntity.BOTTLES));
    }

    @Override public void draw(CentrifugeRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 94, 41);
        var time = Component.translatable("jei.nekogenix.duration", CentrifugeBlockEntity.PROCESS_TICKS / 20);
        graphics.drawString(Minecraft.getInstance().font, time, 94, 111, 0xFF555555, false);
    }
}
