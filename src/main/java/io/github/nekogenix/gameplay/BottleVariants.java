package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.Nekogenix;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class BottleVariants {
    @SubscribeEvent public static void brewing(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addRecipe(Ingredient.of(Content.SPLASH_SALIVA.get()), Ingredient.of(Items.DRAGON_BREATH), Content.LINGERING_SALIVA.toStack());
        builder.addRecipe(Ingredient.of(Content.NEKO_STABILIZER.get()), Ingredient.of(Items.GUNPOWDER), Content.SPLASH_SWEET_AMETHYST.toStack());
        builder.addRecipe(Ingredient.of(Content.SPLASH_SWEET_AMETHYST.get()), Ingredient.of(Items.DRAGON_BREATH), Content.LINGERING_SWEET_AMETHYST.toStack());
    }

    @EventBusSubscriber(modid = Nekogenix.ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                for (var item : new net.minecraft.world.item.Item[] {Content.SPLASH_SALIVA.get(), Content.LINGERING_SALIVA.get(),
                        Content.SPLASH_SWEET_AMETHYST.get(), Content.LINGERING_SWEET_AMETHYST.get()}) {
                    DispenserBlock.registerBehavior(item, new ProjectileDispenseBehavior(item));
                }
            });
        }
    }

    private BottleVariants() {}
}
