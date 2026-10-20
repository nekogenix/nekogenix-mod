package io.github.nekogenix.integration.jei;

import io.github.nekogenix.Content;
import io.github.nekogenix.client.CentrifugeScreen;
import io.github.nekogenix.client.PneumaticStandScreen;
import io.github.nekogenix.recipe.CentrifugeRecipe;
import io.github.nekogenix.recipe.PneumaticRecipe;
import io.github.nekogenix.recipe.ProcessingRecipes;
import java.util.List;
import java.util.stream.Stream;
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
    public static final RecipeType<CentrifugeRecipe> CENTRIFUGING = new RecipeType<>(Content.id("centrifuging"), CentrifugeRecipe.class);
    public static final RecipeType<Filling> PNEUMATIC_FILLING = new RecipeType<>(Content.id("pneumatic_filling"), Filling.class);
    public record Filling(PneumaticRecipe recipe, int syringes) {}

    @Override public ResourceLocation getPluginUid() { return Content.id("jei"); }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new CentrifugeCategory(gui), new PneumaticCategory(gui));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var recipes = level.getRecipeManager();
        registration.addRecipes(CENTRIFUGING, recipes.getAllRecipesFor(ProcessingRecipes.CENTRIFUGING.get()).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
        registration.addRecipes(PNEUMATIC_FILLING, recipes.getAllRecipesFor(ProcessingRecipes.PNEUMATIC_FILLING.get()).stream()
                .flatMap(holder -> Stream.of(new Filling(holder.value(), 1), new Filling(holder.value(), 2))).toList());
        registration.addItemStackInfo(List.of(Content.COTTON_SWAB.toStack(), Content.SALIVA_SWAB.toStack()),
                Component.translatable("jei.nekogenix.collecting"));
        registration.addItemStackInfo(List.of(Content.USED_SYRINGE.toStack(), Content.SYRINGE.toStack()),
                Component.translatable("jei.nekogenix.cleaning"));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Content.CENTRIFUGE_ITEM.toStack(), CENTRIFUGING);
        registration.addRecipeCatalyst(Content.PNEUMATIC_ITEM.toStack(), PNEUMATIC_FILLING);
    }

    @Override public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CentrifugeScreen.class, 78, 63, 22, 22, CENTRIFUGING);
        registration.addRecipeClickArea(PneumaticStandScreen.class, 78, 36, 24, 10, PNEUMATIC_FILLING);
    }
}
