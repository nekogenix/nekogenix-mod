package io.github.nekogenix.integration.rei;

import io.github.nekogenix.client.recipe.ProcessingDisplay;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record ProcessingReiDisplay(ProcessingDisplay display) implements Display {
    @Override public CategoryIdentifier<ProcessingReiDisplay> getCategoryIdentifier() { return CategoryIdentifier.of(display.machine().id()); }
    @Override public Optional<ResourceLocation> getDisplayLocation() { return Optional.of(display.id()); }
    @Override public List<EntryIngredient> getInputEntries() { return entries(ProcessingDisplay.Role.INPUT); }
    @Override public List<EntryIngredient> getOutputEntries() { return entries(ProcessingDisplay.Role.OUTPUT); }
    private List<EntryIngredient> entries(ProcessingDisplay.Role role) {
        return display.slots().stream().filter(slot -> slot.role() == role).map(ProcessingReiDisplay::ingredient).toList();
    }
    static EntryIngredient ingredient(ProcessingDisplay.Slot slot) {
        return EntryIngredient.of(slot.items().stream().map(item -> {
            EntryStack<?> entry = EntryStacks.of(item);
            return slot.returned() ? entry.tooltip(Component.translatable("recipe.nekogenix.returned")) : entry;
        }).toList());
    }
}
