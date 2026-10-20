package io.github.nekogenix.client.recipe;

import io.github.nekogenix.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ProcessingPage {
    public static void draw(GuiGraphics graphics, ProcessingDisplay display) {
        var machine = display.machine();
        int width = machine.width(), height = machine.height();
        var mc = Minecraft.getInstance();
        float time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        float progress = (time % machine.ticks()) / machine.ticks();
        graphics.fill(0, 0, width, height, 0xffc6c6c6);
        if (machine == ProcessingDisplay.Machine.CENTRIFUGE) {
            graphics.blit(Content.id("textures/gui/centrifuge.png"), 0, 0, 0, 0, 172, 168, 176, 266);
            centrifugeDetails(graphics, 0, 0, progress, time);
        } else {
            graphics.blit(Content.id("textures/gui/pneumatic_stand.png"), 0, 0, 0, 0, 172, 76, 176, 166);
            pneumaticDetails(graphics, 0, 0, progress, time);
        }
        graphics.fill(0, 0, width, 2, 0xffeeeeee);
        graphics.fill(0, 0, 2, height, 0xffeeeeee);
        graphics.fill(width - 2, 0, width, height, 0xff555555);
        graphics.fill(0, height - 2, width, height, 0xff555555);
        for (var slot : display.slots()) {
            if (slot.role() == ProcessingDisplay.Role.OUTPUT) slot(graphics, slot.x(), slot.y());
        }
        arrow(graphics, 177, machine == ProcessingDisplay.Machine.CENTRIFUGE ? 65 : 53, progress);
        if (machine == ProcessingDisplay.Machine.PNEUMATIC) arrow(graphics, 177, 19, progress);
        var duration = Component.translatable("recipe.nekogenix.duration", machine.ticks() / 20);
        graphics.drawString(mc.font, duration, 220 - mc.font.width(duration) / 2,
                machine == ProcessingDisplay.Machine.CENTRIFUGE ? 147 : 78, 0xff555555, false);
    }

    public static void centrifugeDetails(GuiGraphics graphics, int left, int top, float fraction, float time) {
        for (int row = 0; row < 14; row++) {
            double phase = row * .65 + time * .08;
            int x = 88 + (int)(Math.sin(phase) * 9), opposite = 176 - x;
            int color = row >= 14 * (1 - fraction) ? 0xffd4a6cf : 0xff526775;
            graphics.fill(left + Math.min(x, opposite), top + 62 + row * 2,
                    left + Math.max(x, opposite) + 2, top + 63 + row * 2, 0xff667985);
            graphics.fill(left + x, top + 62 + row * 2, left + x + 2, top + 64 + row * 2, color);
            graphics.fill(left + opposite, top + 62 + row * 2, left + opposite + 2, top + 64 + row * 2, color);
        }
    }

    public static void pneumaticDetails(GuiGraphics graphics, int left, int top, float fraction, float time) {
        int pressure = Math.round(20 * (1 - fraction));
        graphics.fill(left + 18, top + 39, left + 18 + pressure, top + 42, 0xffa8d9dc);
        int height = Math.round(22 * fraction);
        graphics.fill(left + 101, top + 18, left + 103, top + 18 + height, 0xffb9dce1);
        int pulse = (int)(time % 16);
        for (int i = 0; i < 3; i++) {
            int y = 20 + (pulse + i * 5) % 18;
            graphics.fill(left + 51 + i % 2 * 3, top + y, left + 53 + i % 2 * 3, top + y + 1, 0xffe6f3ef);
        }
    }

    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff8b8b8b);
        graphics.fill(x - 1, y - 1, x + 17, y, 0xff373737);
        graphics.fill(x - 1, y - 1, x, y + 17, 0xff373737);
        graphics.fill(x, y + 16, x + 17, y + 17, 0xffffffff);
        graphics.fill(x + 16, y, x + 17, y + 17, 0xffffffff);
    }

    private static void arrow(GuiGraphics graphics, int x, int y, float progress) {
        arrowShape(graphics, x, y, 0xff8b8b8b, 14);
        int length = Math.round(progress * 14);
        if (length > 0) arrowShape(graphics, x, y, 0xffeeeeee, length);
    }

    private static void arrowShape(GuiGraphics graphics, int x, int y, int color, int length) {
        graphics.fill(x, y + 4, x + Math.min(9, length), y + 8, color);
        for (int i = 0; i < 5 && 9 + i < length; i++) graphics.fill(x + 9 + i, y + i + 1, x + 10 + i, y + 11 - i, color);
    }

    private ProcessingPage() {}
}
