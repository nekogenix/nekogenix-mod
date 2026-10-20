package io.github.nekogenix.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.nekogenix.client.recipe.ProcessingDisplay;
import io.github.nekogenix.client.recipe.ProcessingPage;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record ProcessingEmiRecipe(ProcessingDisplay display, EmiRecipeCategory category) implements EmiRecipe {
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return display.machine() == ProcessingDisplay.Machine.PNEUMATIC ? display.id().withPrefix("/") : display.id(); }
    @Override public int getDisplayWidth() { return display.machine().width(); }
    @Override public int getDisplayHeight() { return display.machine().height(); }
    @Override public List<EmiIngredient> getInputs() {
        return display.slots().stream().filter(slot -> slot.role() == ProcessingDisplay.Role.INPUT).map(ProcessingEmiRecipe::ingredient).toList();
    }
    @Override public List<EmiStack> getOutputs() {
        return display.slots().stream().filter(slot -> slot.role() == ProcessingDisplay.Role.OUTPUT)
                .flatMap(slot -> slot.items().stream()).map(EmiStack::of).toList();
    }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, getDisplayWidth(), getDisplayHeight(), (graphics, x, y, delta) -> ProcessingPage.draw(graphics, display));
        for (var slot : display.slots()) {
            var widget = widgets.addSlot(ingredient(slot), slot.x() - 1, slot.y() - 1).drawBack(false);
            if (slot.role() == ProcessingDisplay.Role.OUTPUT) widget.recipeContext(this);
            if (slot.returned()) widget.appendTooltip(Component.translatable("recipe.nekogenix.returned"));
        }
    }
    private static EmiIngredient ingredient(ProcessingDisplay.Slot slot) {
        return EmiIngredient.of(slot.items().stream().map(EmiStack::of).toList(), slot.items().getFirst().getCount());
    }
}
