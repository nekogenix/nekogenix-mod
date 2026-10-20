package io.github.nekogenix.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record PneumaticRecipe(Ingredient source, ItemStack result, ItemStack remainder) implements Recipe<SingleRecipeInput> {
    @Override public boolean matches(SingleRecipeInput input, Level level) { return source.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, source); }
    @Override public RecipeSerializer<?> getSerializer() { return ProcessingRecipes.PNEUMATIC_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ProcessingRecipes.PNEUMATIC_FILLING.get(); }

    public static final class Serializer implements RecipeSerializer<PneumaticRecipe> {
        private static final MapCodec<PneumaticRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("source").forGetter(PneumaticRecipe::source),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PneumaticRecipe::result),
                ItemStack.STRICT_CODEC.fieldOf("remainder").forGetter(PneumaticRecipe::remainder)).apply(instance, PneumaticRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, PneumaticRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, PneumaticRecipe::source,
                ItemStack.STREAM_CODEC, PneumaticRecipe::result,
                ItemStack.STREAM_CODEC, PneumaticRecipe::remainder, PneumaticRecipe::new);
        @Override public MapCodec<PneumaticRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PneumaticRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
