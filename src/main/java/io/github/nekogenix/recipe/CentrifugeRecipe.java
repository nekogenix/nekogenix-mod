package io.github.nekogenix.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record CentrifugeRecipe(Ingredient sample, Ingredient card, ItemStack result) implements Recipe<CentrifugeRecipe.Input> {
    public record Input(Container container) implements RecipeInput {
        @Override public ItemStack getItem(int slot) { return container.getItem(slot); }
        @Override public int size() { return container.getContainerSize(); }
    }

    @Override public boolean matches(Input input, Level level) {
        if (input.size() <= CentrifugeBlockEntity.CARD || !card.test(input.getItem(CentrifugeBlockEntity.CARD))) return false;
        for (int slot = 0; slot < CentrifugeBlockEntity.BOTTLES; slot++) {
            if (input.getItem(slot).getCount() != 1 || !sample.test(input.getItem(slot))) return false;
        }
        return true;
    }
    @Override public ItemStack assemble(Input input, HolderLookup.Provider provider) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider provider) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.withSize(CentrifugeBlockEntity.BOTTLES + 1, sample);
        ingredients.set(CentrifugeBlockEntity.CARD, card);
        return ingredients;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ProcessingRecipes.CENTRIFUGE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ProcessingRecipes.CENTRIFUGING.get(); }

    public static final class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        private static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("sample").forGetter(CentrifugeRecipe::sample),
                Ingredient.CODEC_NONEMPTY.fieldOf("card").forGetter(CentrifugeRecipe::card),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CentrifugeRecipe::result)).apply(instance, CentrifugeRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CentrifugeRecipe::sample,
                Ingredient.CONTENTS_STREAM_CODEC, CentrifugeRecipe::card,
                ItemStack.STREAM_CODEC, CentrifugeRecipe::result, CentrifugeRecipe::new);
        @Override public MapCodec<CentrifugeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
