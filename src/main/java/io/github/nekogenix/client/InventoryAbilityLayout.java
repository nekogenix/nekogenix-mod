package io.github.nekogenix.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

final class InventoryAbilityLayout {
    private static final int EDGE = 4;
    private static final int GAP = 6;
    private static final int SPACING = 4;
    private static final int VIEWER_FOOTER = 22;
    private static Request cached;
    private static List<ScreenRectangle> cells = List.of();

    private record Request(int width, int height, ScreenRectangle panel, int count, List<ScreenRectangle> occupied) {}

    static List<ScreenRectangle> locate(AbstractContainerScreen<?> screen, int count) {
        int tabs = screen instanceof CreativeModeInventoryScreen ? 28 : 0;
        var panel = new ScreenRectangle(screen.getGuiLeft(), screen.getGuiTop() - tabs,
                screen.getXSize(), screen.getYSize() + tabs * 2);
        var occupied = new ArrayList<ScreenRectangle>();
        occupied.add(panel);
        collect(screen.children(), occupied);
        if (ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei") || ModList.get().isLoaded("roughlyenoughitems")) {
            occupied.add(new ScreenRectangle(0, screen.height - VIEWER_FOOTER, screen.width, VIEWER_FOOTER));
        }
        var request = new Request(screen.width, screen.height, panel, count, List.copyOf(occupied));
        if (!request.equals(cached)) {
            cells = arrange(screen.width, screen.height, panel, count, occupied);
            cached = request;
        }
        return cells;
    }

    private static void collect(List<? extends GuiEventListener> children, List<ScreenRectangle> occupied) {
        for (var child : children) {
            if (child instanceof AbstractWidget widget && !widget.visible) continue;
            if (child instanceof ContainerEventHandler container) collect(container.children(), occupied);
            if (child instanceof AbstractWidget || !(child instanceof ContainerEventHandler)) {
                var bounds = child.getRectangle();
                if (bounds.width() > 0 && bounds.height() > 0) occupied.add(bounds);
            }
        }
    }

    static List<ScreenRectangle> arrange(int width, int height, ScreenRectangle panel, int count, List<ScreenRectangle> occupied) {
        if (count < 1) return List.of();
        occupied = occupied.stream().sorted(Comparator.comparingInt(ScreenRectangle::top)).toList();
        int preferredTop = panel.bottom() + GAP;
        int center = panel.left() + panel.width() / 2;
        for (int size : new int[] {32, 24, 18}) {
            int rowWidth = count * (size + SPACING) - SPACING;
            if (rowWidth > width - EDGE * 2) continue;
            int x = Mth.clamp(center - rowWidth / 2, EDGE, width - rowWidth - EDGE);
            int y = freeY(x, rowWidth, size, preferredTop, preferredTop, height, occupied);
            if (y >= 0) return grid(x, y, size, count, count);
        }
        for (int columns = count; columns >= 1; columns--) for (int size : new int[] {24, 18}) {
            int rowWidth = columns * (size + SPACING) - SPACING;
            int rows = (count + columns - 1) / columns;
            int rowHeight = rows * (size + SPACING) - SPACING;
            if (rowWidth > width - EDGE * 2 || rowHeight > height - EDGE * 2) continue;
            int preferredLeft = center - rowWidth / 2;
            int bestX = 0, bestY = -1, bestDistance = Integer.MAX_VALUE;
            for (int anchor : new int[] {preferredLeft, panel.left() - rowWidth - GAP, panel.right() + GAP, EDGE, width - rowWidth - EDGE}) {
                int x = Mth.clamp(anchor, EDGE, width - rowWidth - EDGE);
                int y = freeY(x, rowWidth, rowHeight, Math.max(EDGE, panel.top() + panel.height() / 2), preferredTop, height, occupied);
                int distance = y < 0 ? Integer.MAX_VALUE : Math.abs(x - preferredLeft) + Math.abs(y - preferredTop) * 3;
                if (distance < bestDistance) { bestX = x; bestY = y; bestDistance = distance; }
            }
            if (bestY >= 0) return grid(bestX, bestY, size, columns, count);
        }
        return List.of();
    }

    private static int freeY(int x, int width, int height, int minimum, int preferred, int screenHeight, List<ScreenRectangle> occupied) {
        int cursor = Math.max(EDGE, minimum);
        int best = -1, distance = Integer.MAX_VALUE;
        for (var bounds : occupied) {
            if (x + width + GAP <= bounds.left() || x >= bounds.right() + GAP) continue;
            int end = Math.min(screenHeight - EDGE, bounds.top() - GAP);
            if (end - cursor >= height) {
                int y = Mth.clamp(preferred, cursor, end - height);
                if (Math.abs(y - preferred) < distance) { best = y; distance = Math.abs(y - preferred); }
            }
            cursor = Math.max(cursor, bounds.bottom() + GAP);
        }
        if (screenHeight - EDGE - cursor >= height) {
            int y = Mth.clamp(preferred, cursor, screenHeight - EDGE - height);
            if (Math.abs(y - preferred) < distance) best = y;
        }
        return best;
    }

    private static List<ScreenRectangle> grid(int x, int y, int size, int columns, int count) {
        var result = new ArrayList<ScreenRectangle>(count);
        for (int i = 0; i < count; i++) result.add(new ScreenRectangle(x + i % columns * (size + SPACING),
                y + i / columns * (size + SPACING), size, size));
        return List.copyOf(result);
    }

    private InventoryAbilityLayout() {}
}
