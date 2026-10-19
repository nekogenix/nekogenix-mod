package io.github.nekogenix.client.skin;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.skin.CatSkin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class CatSkinScreen extends Screen {
    private final Screen parent;
    private CatSkin choice;
    private List<SkinLibrary.Entry> cards = List.of();
    private CatSkinPreview preview;
    private Button apply, remove, verify;
    private int scroll, previewWidth, gridX, columns, tileWidth, tileHeight, bottom;
    private float rotation = 145, zoom = 1;
    private boolean waitingForLibrary = !CatSkins.loaded();

    public CatSkinScreen(Screen parent) { super(text("title")); this.parent = parent; choice = CatSkins.selected(); }

    @Override protected void init() {
        CatSkins.load();
        preview = new CatSkinPreview();
        previewWidth = Math.max(104, width / 3);
        gridX = previewWidth + 18;
        columns = Math.max(1, Math.min(4, (width - gridX - 12) / 90));
        tileWidth = (width - gridX - 12) / columns;
        tileHeight = Math.max(80, Math.min(106, (height - 100) / 2));
        bottom = height - 80;
        int third = (width - 32) / 3;
        addRenderableWidget(Button.builder(text("import"), button -> minecraft.setScreen(new NewCatSkinScreen(this))).bounds(12, height - 52, third, 20).build());
        remove = addRenderableWidget(Button.builder(text("delete"), button -> minecraft.setScreen(new ConfirmScreen(answer -> {
            if (answer) { CatSkins.remove(choice); choice = CatSkin.DEFAULT; }
            minecraft.setScreen(this);
        }, text("delete_title"), text("delete_description")))).bounds(16 + third, height - 52, third, 20).build());
        apply = addRenderableWidget(Button.builder(text("use"), button -> CatSkins.apply(choice)).bounds(20 + third * 2, height - 52, third, 20).build());
        verify = addRenderableWidget(Button.builder(text("verify"), button -> CatSkins.verify()).bounds(12, height - 28, Math.min(180, width / 2 - 20), 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width - 112, height - 28, 100, 20).build());
        refresh();
        if (!CatSkins.licensed()) CatSkins.verify();
    }

    private void refresh() {
        var items = new ArrayList<SkinLibrary.Entry>();
        items.add(new SkinLibrary.Entry("default", CatSkin.DEFAULT));
        for (String variant : CatSkin.VARIANTS) items.add(new SkinLibrary.Entry(variant, new CatSkin(variant, new byte[0])));
        items.addAll(CatSkins.entries()); cards = items;
        scroll = Math.clamp(scroll, 0, maxScroll());
        apply.active = CatSkins.loaded() && CatSkins.licensed() && !CatSkins.checking();
        remove.active = choice.variant().equals("custom");
        verify.active = !CatSkins.licensed() && !CatSkins.checking();
        verify.visible = !CatSkins.licensed();
    }
    @Override public void tick() {
        if (waitingForLibrary && CatSkins.loaded()) { choice = CatSkins.selected(); waitingForLibrary = false; }
        refresh();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        graphics.fill(8, 30, previewWidth + 8, bottom, 0xd010171c);
        graphics.fill(gridX - 4, 30, width - 8, bottom, 0xc00b1014);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        var label = label(new SkinLibrary.Entry("", choice));
        if (choice.variant().equals("custom")) label = cards.stream().filter(e -> same(e.skin(), choice)).map(this::label).findFirst().orElse(text("custom"));
        graphics.drawCenteredString(font, font.plainSubstrByWidth(label.getString(), previewWidth - 12), previewWidth / 2 + 8, 40, 0xffffff);
        graphics.enableScissor(9, 58, previewWidth + 7, bottom - 15);
        float scale = Math.min(previewWidth * .85F, (bottom - 68) * .9F) * zoom;
        preview.render(graphics, choice, previewWidth / 2 + 8, (58 + bottom - 15) / 2 + (int)(scale * .3F), scale, rotation);
        graphics.disableScissor();
        graphics.drawCenteredString(font, text("drag"), previewWidth / 2 + 8, bottom - 13, 0x89989f);
        graphics.enableScissor(gridX - 3, 31, width - 9, bottom - 1);
        for (int i = 0; i < cards.size(); i++) {
            int x = gridX + i % columns * tileWidth, y = 34 + i / columns * tileHeight - scroll;
            if (y + tileHeight < 30 || y >= bottom) continue;
            var entry = cards.get(i);
            boolean picked = same(entry.skin(), choice), used = same(entry.skin(), CatSkins.selected());
            boolean hover = mouseX >= x && mouseX < x + tileWidth - 4 && mouseY >= Math.max(31, y) && mouseY < Math.min(bottom, y + tileHeight - 4);
            graphics.fill(x, y, x + tileWidth - 4, y + tileHeight - 4, picked ? 0xff719b91 : hover ? 0xff5e707b : 0xff34424b);
            graphics.fill(x + 1, y + 1, x + tileWidth - 5, y + tileHeight - 5, picked ? 0xff253b39 : 0xff19232a);
            float cardScale = Math.min((tileWidth - 8) * .72F, (tileHeight - 28) * 1.05F);
            preview.render(graphics, entry.skin(), x + (tileWidth - 4) / 2, y + tileHeight / 2 + (int)(cardScale * .28F), cardScale, 145);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(label(entry).getString(), tileWidth - 10), x + (tileWidth - 4) / 2, y + tileHeight - 17, 0xffffff);
            if (used) graphics.drawString(font, text("active"), x + 4, y + 4, 0xa6d6a4, false);
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int track = bottom - 36, handle = Math.max(16, track * track / (track + maxScroll()));
            int y = 34 + (track - handle) * scroll / maxScroll();
            graphics.fill(width - 7, 34, width - 4, bottom - 2, 0xff1b252b);
            graphics.fill(width - 7, y, width - 4, y + handle, 0xff91a3a7);
        }
        Component status = CatSkins.message();
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), width - 24), 12, height - 73,
                CatSkins.licensed() ? 0xa7b4bc : 0xd8bf8c, false);
        if (mouseY >= height - 76 && mouseY < height - 58)
            graphics.renderTooltip(font, font.split(status, Math.min(250, width - 30)), mouseX, mouseY);
    }

    private Component label(SkinLibrary.Entry entry) {
        return entry.skin().variant().equals("custom") ? Component.literal(entry.name()) : text("variant." + entry.skin().variant());
    }
    private static boolean same(CatSkin a, CatSkin b) { return a.variant().equals(b.variant()) && (!a.variant().equals("custom") || a.hash().equals(b.hash())); }
    private int maxScroll() { return Math.max(0, (cards.size() + columns - 1) / columns * tileHeight - (bottom - 34)); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= gridX && y >= 34 && y < bottom) {
            int col = (int)(x - gridX) / tileWidth, row = ((int)y - 34 + scroll) / tileHeight;
            int index = row * columns + col;
            if (col < columns && index >= 0 && index < cards.size()) {
                choice = cards.get(index).skin(); waitingForLibrary = false;
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && x < previewWidth + 8 && y > 30 && y < bottom) { rotation += (float)dx * 1.5F; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < previewWidth + 8) zoom = net.minecraft.util.Mth.clamp(zoom + (float)vertical * .08F, .65F, 1.3F);
        else scroll = Math.clamp(scroll - (int)(vertical * 32), 0, maxScroll());
        return true;
    }
    @Override public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty()) return;
        var screen = new NewCatSkinScreen(this); minecraft.setScreen(screen); screen.onFilesDrop(paths);
    }
    void saved(CatSkin skin) {
        choice = skin; waitingForLibrary = false; refresh();
        for (int i = 0; i < cards.size(); i++) if (same(cards.get(i).skin(), skin)) {
            int top = 34 + i / columns * tileHeight;
            if (top - scroll < 34 || top + tileHeight - scroll > bottom)
                scroll = Math.clamp(top + tileHeight - bottom, 0, maxScroll());
            break;
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    private static Component text(String key) { return Component.translatable("skins.nekogenix." + key); }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
    public static final class Menu {
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            if (!(event.getScreen() instanceof SkinCustomizationScreen screen)) return;
            for (var listener : event.getListenersList()) if (listener instanceof OptionsList list) {
                list.addSmall(List.of(Button.builder(text("title"), button -> net.minecraft.client.Minecraft.getInstance().setScreen(new CatSkinScreen(screen))).build()));
                return;
            }
        }
    }
}
