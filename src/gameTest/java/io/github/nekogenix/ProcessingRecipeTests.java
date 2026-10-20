package io.github.nekogenix;

import com.mojang.serialization.JsonOps;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.recipe.CentrifugeRecipe;
import io.github.nekogenix.recipe.ProcessingRecipes;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.RegistryOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Nekogenix.ID)
@PrefixGameTestTemplate(false)
public final class ProcessingRecipeTests {
    @GameTest(template = "empty")
    public static void loadedProcessingRecipesMatchActualInputsAndSynchronize(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var container = new SimpleContainer(8);
        for (int slot = 0; slot < CentrifugeBlockEntity.BOTTLES; slot++) container.setItem(slot, Content.SALIVA_BOTTLE.toStack());
        container.setItem(CentrifugeBlockEntity.CARD, Content.BLANK_NUCLEOCARD.toStack());
        var input = new CentrifugeRecipe.Input(container);
        var recipe = manager.getRecipeFor(ProcessingRecipes.CENTRIFUGING.get(), input, level).orElseThrow().value();
        helper.assertTrue(recipe.assemble(input, level.registryAccess()).is(Content.CAT_NUCLEOCARD),
                "The loaded extraction recipe produces the same card used by the machine");
        var result = recipe.getResultItem(level.registryAccess());
        result.shrink(1);
        helper.assertTrue(recipe.getResultItem(level.registryAccess()).getCount() == 1, "Recipe results cannot be consumed by a caller");
        container.setItem(0, Items.GLASS_BOTTLE.getDefaultInstance());
        helper.assertFalse(recipe.matches(input, level), "Empty bottles cannot substitute for a saliva sample");
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        for (var holder : manager.getRecipes()) {
            if (holder.value().getType() != ProcessingRecipes.CENTRIFUGING.get()
                    && holder.value().getType() != ProcessingRecipes.PNEUMATIC_FILLING.get()) continue;
            var encoded = Recipe.CODEC.encodeStart(ops, holder.value()).getOrThrow();
            var decoded = Recipe.CODEC.parse(ops, encoded).getOrThrow();
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                Recipe.STREAM_CODEC.encode(buffer, decoded);
                var network = Recipe.STREAM_CODEC.decode(buffer);
                helper.assertTrue(network.getType() == holder.value().getType()
                        && net.minecraft.world.item.ItemStack.matches(network.getResultItem(level.registryAccess()), holder.value().getResultItem(level.registryAccess())),
                        "Recipes retain their type and result through JSON and network round trips");
            } finally { buffer.release(); }
        }
        for (var source : new net.minecraft.world.item.ItemStack[] {Content.CAT_NUCLEOCARD.toStack(), Content.NEKO_STABILIZER.toStack()}) {
            var filling = manager.getRecipeFor(ProcessingRecipes.PNEUMATIC_FILLING.get(), new SingleRecipeInput(source), level).orElseThrow().value();
            boolean crystal = source.is(Content.NEKO_STABILIZER);
            helper.assertTrue(filling.result().is(crystal ? Content.STABILIZING_INJECTION : Content.INJECTION)
                    && (crystal ? filling.remainder().is(Items.GLASS_BOTTLE) : filling.remainder().is(Content.BLANK_NUCLEOCARD)),
                    "Both filling recipes declare the real injection and returned source container");
        }
        helper.assertTrue(manager.getRecipeFor(ProcessingRecipes.PNEUMATIC_FILLING.get(), new SingleRecipeInput(Content.BLANK_NUCLEOCARD.toStack()), level).isEmpty(),
                "An empty nucleocard is not a filling source");
        helper.succeed();
    }
}
