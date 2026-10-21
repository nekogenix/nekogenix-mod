package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

public final class InventoryAbilityLayoutChecks {
    public static void verify() {
        int cases = 0;
        for (int[] viewport : new int[][] {{320, 240}, {360, 240}, {427, 240}, {427, 320}, {480, 270},
                {640, 360}, {854, 480}, {960, 540}, {1280, 720}}) {
            int width = viewport[0], height = viewport[1];
            var panel = new ScreenRectangle((width - 176) / 2, (height - 166) / 2, 176, 166);
            for (int count = 1; count <= 4; count++) for (int scenario = 0; scenario < 6; scenario++) {
                var occupied = new ArrayList<ScreenRectangle>();
                occupied.add(panel);
                if (scenario > 0) occupied.add(new ScreenRectangle((width - 200) / 2, panel.bottom() + 2, 200, 20));
                if (scenario > 1) occupied.add(new ScreenRectangle((width - 180) / 2, height - 22, 180, 18));
                if (scenario > 2) occupied.add(new ScreenRectangle(4, panel.bottom() + 2, 22, 20));
                if (scenario > 3) occupied.add(new ScreenRectangle(0, height - 22, width, 22));
                if (scenario == 5) occupied.add(new ScreenRectangle(panel.left() - 16, panel.bottom(), panel.width() + 32, 26));
                var cells = InventoryAbilityLayout.arrange(width, height, panel, count, occupied);
                check(cells.size() == count, "All abilities fit around inventory controls at " + width + "x" + height);
                check(cells.getFirst().top() >= panel.top() + panel.height() / 2,
                        "Compact layouts stay in the lower inventory area instead of moving abilities above it");
                verify(width, height, cells, occupied);
                if (scenario == 1 && height >= 320) check(cells.getFirst().top() >= panel.bottom() + 28,
                        "A button below the inventory moves the row down with a visible gap");
                cases++;
            }
        }
        var panel = new ScreenRectangle(72, 37, 176, 166);
        check(InventoryAbilityLayout.arrange(320, 240, panel, 4,
                List.of(new ScreenRectangle(0, 0, 320, 240))).isEmpty(), "A fully covered screen never produces overlapping icons");
        Nekogenix.LOGGER.info("Nekogenix inventory layout checks passed: {} layouts across nine viewport sizes with buttons search fields and screen borders", cases);
    }

    public static void verify(AbstractContainerScreen<?> screen) {
        var occupied = new ArrayList<ScreenRectangle>();
        int tabs = screen instanceof CreativeModeInventoryScreen ? 28 : 0;
        occupied.add(new ScreenRectangle(screen.getGuiLeft(), screen.getGuiTop() - tabs,
                screen.getXSize(), screen.getYSize() + tabs * 2));
        collect(screen.children(), occupied);
        if (net.neoforged.fml.ModList.get().isLoaded("inventoryprofilesnext")) {
            occupied.add(new ScreenRectangle(screen.getGuiLeft() - 16, screen.getGuiTop() + screen.getYSize() + 3,
                    screen.getXSize() + 32, 20));
        }
        var cells = InventoryAbilities.bounds(screen);
        check(cells.size() == 4, "All four acquired abilities remain visible at the actual GUI scale");
        if (net.neoforged.fml.ModList.get().isLoaded("emi") || net.neoforged.fml.ModList.get().isLoaded("jei")
                || net.neoforged.fml.ModList.get().isLoaded("roughlyenoughitems")) {
            for (var cell : cells) check(cell.bottom() <= screen.height - 28,
                    "Ability icons leave clear space above recipe viewer search and footer controls");
        }
        verify(screen.width, screen.height, cells, occupied);
    }

    private static void verify(int width, int height, List<ScreenRectangle> cells, List<ScreenRectangle> occupied) {
        for (int i = 0; i < cells.size(); i++) {
            var cell = cells.get(i);
            check(cell.left() >= 4 && cell.top() >= 4 && cell.right() <= width - 4 && cell.bottom() <= height - 4,
                    "Ability icons stay fully inside the viewport");
            for (var control : occupied) check(!new ScreenRectangle(control.left() - 5, control.top() - 5,
                    control.width() + 10, control.height() + 10).overlaps(cell), "Ability icons leave clear space around visible controls");
            for (int j = i + 1; j < cells.size(); j++) check(!cell.overlaps(cells.get(j)), "Ability icons never overlap each other");
        }
    }

    private static void collect(List<? extends GuiEventListener> children, List<ScreenRectangle> result) {
        for (var child : children) {
            if (child instanceof AbstractWidget widget && !widget.visible) continue;
            if (child instanceof ContainerEventHandler group) collect(group.children(), result);
            if (child instanceof AbstractWidget || !(child instanceof ContainerEventHandler)) {
                var bounds = child.getRectangle();
                if (bounds.width() > 0 && bounds.height() > 0) result.add(bounds);
            }
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException("Inventory ability layout check failed: " + message);
    }

    private InventoryAbilityLayoutChecks() {}
}
