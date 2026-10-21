package io.github.nekogenix.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.nekogenix.Content;
import io.github.nekogenix.client.InventoryAbilities;
import io.github.nekogenix.client.recipe.ProcessingDisplay;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@EmiEntrypoint
public final class NekogenixEmi implements EmiPlugin {
    @Override public void register(EmiRegistry registry) {
        if (!ProcessingDisplay.active(ProcessingDisplay.Viewer.EMI)) return;
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
                InventoryAbilities.bounds(container).forEach(area -> consumer.accept(
                        new dev.emi.emi.api.widget.Bounds(area.left(), area.top(), area.width(), area.height())));
            }
        });
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var machine : ProcessingDisplay.Machine.values()) {
            var category = new EmiRecipeCategory(machine.id(), EmiStack.of(machine.icon())) {
                @Override public Component getName() { return machine.title(); }
            };
            registry.addCategory(category);
            registry.addWorkstation(category, EmiStack.of(machine.icon()));
            var displays = machine == ProcessingDisplay.Machine.CENTRIFUGE
                    ? ProcessingDisplay.centrifuging(level) : ProcessingDisplay.filling(level);
            displays.forEach(display -> registry.addRecipe(new ProcessingEmiRecipe(display, category)));
        }
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(Content.COTTON_SWAB), EmiStack.of(Content.SALIVA_SWAB)),
                List.of(Component.translatable("recipe.nekogenix.collecting")), Content.id("/collecting")));
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(Content.USED_SYRINGE), EmiStack.of(Content.SYRINGE)),
                List.of(Component.translatable("recipe.nekogenix.cleaning")), Content.id("/cleaning")));
    }
}
