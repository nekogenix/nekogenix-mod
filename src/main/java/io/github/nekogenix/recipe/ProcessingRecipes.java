package io.github.nekogenix.recipe;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProcessingRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Nekogenix.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Nekogenix.ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> CENTRIFUGING =
            TYPES.register("centrifuging", () -> RecipeType.simple(Content.id("centrifuging")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<PneumaticRecipe>> PNEUMATIC_FILLING =
            TYPES.register("pneumatic_filling", () -> RecipeType.simple(Content.id("pneumatic_filling")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
            SERIALIZERS.register("centrifuging", CentrifugeRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PneumaticRecipe>> PNEUMATIC_SERIALIZER =
            SERIALIZERS.register("pneumatic_filling", PneumaticRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ProcessingRecipes() {}
}
