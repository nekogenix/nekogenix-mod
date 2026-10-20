package io.github.nekogenix.integration.jei;

import io.github.nekogenix.Content;
import io.github.nekogenix.client.CentrifugeScreen;
import io.github.nekogenix.client.PneumaticStandScreen;
import io.github.nekogenix.client.recipe.ProcessingDisplay;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class NekogenixJei implements IModPlugin {
    public static final RecipeType<ProcessingDisplay> CENTRIFUGING = new RecipeType<>(Content.id("centrifuging"), ProcessingDisplay.class);
    public static final RecipeType<ProcessingDisplay> PNEUMATIC_FILLING = new RecipeType<>(Content.id("pneumatic_filling"), ProcessingDisplay.class);

    @Override public ResourceLocation getPluginUid() { return Content.id("jei"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.JEI)) return;
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ProcessingCategory(gui, ProcessingDisplay.Machine.CENTRIFUGE),
                new ProcessingCategory(gui, ProcessingDisplay.Machine.PNEUMATIC));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.JEI)) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(CENTRIFUGING, ProcessingDisplay.centrifuging(level));
        registration.addRecipes(PNEUMATIC_FILLING, ProcessingDisplay.filling(level));
        registration.addItemStackInfo(List.of(Content.COTTON_SWAB.toStack(), Content.SALIVA_SWAB.toStack()),
                Component.translatable("recipe.nekogenix.collecting"));
        registration.addItemStackInfo(List.of(Content.USED_SYRINGE.toStack(), Content.SYRINGE.toStack()),
                Component.translatable("recipe.nekogenix.cleaning"));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.JEI)) return;
        registration.addRecipeCatalyst(Content.CENTRIFUGE_ITEM.toStack(), CENTRIFUGING);
        registration.addRecipeCatalyst(Content.PNEUMATIC_ITEM.toStack(), PNEUMATIC_FILLING);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.JEI)) return;
        registration.addRecipeClickArea(CentrifugeScreen.class, 78, 63, 22, 22, CENTRIFUGING);
        registration.addRecipeClickArea(PneumaticStandScreen.class, 78, 36, 24, 10, PNEUMATIC_FILLING);
    }
}
