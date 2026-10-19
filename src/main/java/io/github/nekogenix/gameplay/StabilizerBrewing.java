package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class StabilizerBrewing {
    @SubscribeEvent
    public static void brewing(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(Items.HONEY_BOTTLE), Ingredient.of(Items.AMETHYST_SHARD), Content.NEKO_STABILIZER.toStack());
    }

    private StabilizerBrewing() {}
}
