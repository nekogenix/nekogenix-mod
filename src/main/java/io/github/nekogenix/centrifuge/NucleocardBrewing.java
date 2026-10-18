package io.github.nekogenix.centrifuge;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PotionBrewEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class NucleocardBrewing {
    @SubscribeEvent
    public static void recipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(Content.SYRINGE.get()), Ingredient.of(Content.CAT_NUCLEOCARD.get()), Content.INJECTION.toStack());
    }

    @SubscribeEvent
    public static void brew(PotionBrewEvent.Pre event) {
        if (!event.getItem(3).is(Content.CAT_NUCLEOCARD)) return;
        for (int slot = 0; slot < 3; slot++) {
            if (event.getItem(slot).is(Content.SYRINGE)) {
                event.setItem(slot, Content.INJECTION.toStack());
                event.setItem(3, Content.BLANK_NUCLEOCARD.toStack());
                event.setCanceled(true);
                return;
            }
        }
    }

    private NucleocardBrewing() {}
}
