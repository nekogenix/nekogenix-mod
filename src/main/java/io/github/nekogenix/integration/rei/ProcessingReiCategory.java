package io.github.nekogenix.integration.rei;

import io.github.nekogenix.client.recipe.ProcessingDisplay;
import io.github.nekogenix.client.recipe.ProcessingPage;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

public record ProcessingReiCategory(ProcessingDisplay.Machine machine) implements DisplayCategory<ProcessingReiDisplay> {
    @Override public CategoryIdentifier<ProcessingReiDisplay> getCategoryIdentifier() { return CategoryIdentifier.of(machine.id()); }
    @Override public Component getTitle() { return machine.title(); }
    @Override public Renderer getIcon() { return EntryStacks.of(machine.icon()); }
    @Override public int getDisplayWidth(ProcessingReiDisplay display) { return machine.width(); }
    @Override public int getDisplayHeight() { return machine.height(); }
    @Override public List<Widget> setupDisplay(ProcessingReiDisplay recipe, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            graphics.pose().pushPose();
            graphics.pose().translate(bounds.x, bounds.y, 0);
            ProcessingPage.draw(graphics, recipe.display());
            graphics.pose().popPose();
        }));
        for (var slot : recipe.display().slots()) {
            var widget = Widgets.createSlot(new Point(bounds.x + slot.x() - 1, bounds.y + slot.y() - 1))
                    .entries(ProcessingReiDisplay.ingredient(slot)).disableBackground();
            if (slot.role() == ProcessingDisplay.Role.INPUT) widget.markInput();
            else widget.markOutput();
            widgets.add(widget);
        }
        return widgets;
    }
}
