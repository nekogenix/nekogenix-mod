package io.github.nekogenix.integration.rei;

import io.github.nekogenix.Content;
import io.github.nekogenix.client.CentrifugeScreen;
import io.github.nekogenix.client.PneumaticStandScreen;
import io.github.nekogenix.client.recipe.ProcessingDisplay;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@REIPluginClient
public final class NekogenixRei implements REIClientPlugin {
    @Override public void registerCategories(CategoryRegistry registry) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.REI)) return;
        for (var machine : ProcessingDisplay.Machine.values()) {
            var category = new ProcessingReiCategory(machine);
            registry.add(category);
            registry.addWorkstations(category.getCategoryIdentifier(), EntryStacks.of(machine.icon()));
        }
    }
    @Override public void registerDisplays(DisplayRegistry registry) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.REI)) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ProcessingDisplay.centrifuging(level).forEach(display -> registry.add(new ProcessingReiDisplay(display)));
        ProcessingDisplay.filling(level).forEach(display -> registry.add(new ProcessingReiDisplay(display)));
        registry.add(DefaultInformationDisplay.createFromEntries(EntryIngredient.of(EntryStacks.of(Content.COTTON_SWAB), EntryStacks.of(Content.SALIVA_SWAB)),
                Content.SALIVA_SWAB.toStack().getHoverName()).line(Component.translatable("recipe.nekogenix.collecting")));
        registry.add(DefaultInformationDisplay.createFromEntries(EntryIngredient.of(EntryStacks.of(Content.USED_SYRINGE), EntryStacks.of(Content.SYRINGE)),
                Content.SYRINGE.toStack().getHoverName()).line(Component.translatable("recipe.nekogenix.cleaning")));
    }
    @Override public void registerScreens(ScreenRegistry registry) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.REI)) return;
        registry.registerContainerClickArea(new Rectangle(78, 63, 22, 22), CentrifugeScreen.class, CategoryIdentifier.of(ProcessingDisplay.Machine.CENTRIFUGE.id()));
        registry.registerContainerClickArea(new Rectangle(78, 36, 24, 10), PneumaticStandScreen.class, CategoryIdentifier.of(ProcessingDisplay.Machine.PNEUMATIC.id()));
    }
}
