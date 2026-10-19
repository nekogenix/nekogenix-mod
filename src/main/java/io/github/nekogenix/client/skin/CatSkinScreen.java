package io.github.nekogenix.client.skin;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.skin.CatSkin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

public final class CatSkinScreen extends Screen {
    private final Screen parent;
    private final List<Card> cards = new ArrayList<>();
    private List<SkinLibrary.Entry> lastEntries;
    private CatSkin choice = CatSkins.selected();
    private CatSkinPreview preview;
    private EditBox name;
    private Button verify;
    private Component hoverTip;
    private int scroll, previewWidth, gridX, columns, tileWidth, tileHeight, bottom;
    private float rotation = 145, zoom = 1;
    private boolean waitingForLibrary = !CatSkins.loaded();

    public CatSkinScreen(Screen parent) { super(text("title")); this.parent = parent; }

    @Override protected void init() {
        CatSkins.load(); preview = new CatSkinPreview();
        previewWidth = Math.max(104, width / 3); gridX = previewWidth + 18;
        columns = Math.max(1, Math.min(4, (width - gridX - 12) / 90));
        tileWidth = (width - gridX - 12) / columns;
        tileHeight = Math.max(80, Math.min(106, (height - 86) / 2)); bottom = height - 58;
        name = addRenderableWidget(new EditBox(font, 20, 43, previewWidth - 38, 16, text("rename")));
        name.setMaxLength(32); name.setBordered(false); name.setTextColor(0xe4f1ee);
        verify = addRenderableWidget(new VerifyButton(width - 28, 7));
        var footer = new HeaderAndFooterLayout(this);
        footer.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
        footer.arrangeElements(); footer.visitWidgets(this::addRenderableWidget);
        lastEntries = null; sync(); updateName();
        if (!CatSkins.licensed()) CatSkins.verify();
    }

    private void sync() {
        if (waitingForLibrary && CatSkins.loaded()) { choice = CatSkins.selected(); waitingForLibrary = false; }
        var entries = CatSkins.entries();
        if (entries != lastEntries) {
            if (choice.variant().equals("custom") && entries.stream().noneMatch(e -> same(e.skin(), choice))) choice = CatSkins.selected();
            if (getFocused() instanceof Card) setFocused(null);
            for (var card : cards) removeWidget(card);
            cards.clear(); cards.add(addWidget(new Card(null)));
            for (String variant : CatSkin.VARIANTS) cards.add(addWidget(new Card(new SkinLibrary.Entry(variant, new CatSkin(variant, new byte[0])))));
            for (var entry : entries) cards.add(addWidget(new Card(entry)));
            lastEntries = entries;
            if (!name.isFocused()) updateName();
        }
        verify.visible = !CatSkins.licensed(); verify.active = !CatSkins.checking();
        scroll = Math.clamp(scroll, 0, maxScroll()); layoutCards();
    }

    private void layoutCards() {
        for (int i = 0; i < cards.size(); i++) {
            var card = cards.get(i);
            card.setPosition(gridX + i % columns * tileWidth, 34 + i / columns * tileHeight - scroll);
            card.visible = card.getY() + card.getHeight() > 30 && card.getY() < bottom;
        }
    }
    @Override public void tick() { sync(); }

    private Component chosenName() {
        return choice.variant().equals("custom") ? CatSkins.entries().stream().filter(e -> same(e.skin(), choice)).map(e -> Component.literal(e.name())).findFirst().orElse(Component.literal(""))
                : text("variant." + choice.variant());
    }
    private void updateName() {
        name.visible = choice.variant().equals("custom"); name.setEditable(name.visible); name.setValue(chosenName().getString());
    }
    private void finishRename() {
        if (!name.visible || !name.isFocused()) return;
        String value = name.getValue().strip();
        if (!value.isEmpty() && !value.equals(chosenName().getString())) CatSkins.rename(choice, value);
        else if (value.isEmpty()) updateName();
        name.setFocused(false);
    }
    private void pick(CatSkin skin) { finishRename(); choice = skin; waitingForLibrary = false; updateName(); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics, mouseX, mouseY, partial); hoverTip = null;
        graphics.fill(8, 30, previewWidth + 8, bottom, 0xd010171c);
        graphics.fill(gridX - 4, 30, width - 8, bottom, 0xc00b1014);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        if (name.visible) {
            graphics.fill(14, 36, previewWidth + 2, 61, name.isFocused() ? 0xff759e94 : 0xff405b57);
            graphics.fill(15, 37, previewWidth + 1, 60, name.isFocused() ? 0xff243c37 : 0xff1c302d);
            icon(graphics, 2, previewWidth - 16, 39, 0xffa6cbbd);
            if (mouseX >= 14 && mouseX < previewWidth + 2 && mouseY >= 36 && mouseY < 61) hoverTip = text("rename");
        } else graphics.drawCenteredString(font, font.plainSubstrByWidth(chosenName().getString(), previewWidth - 12), previewWidth / 2 + 8, 44, 0xffffff);
        graphics.enableScissor(9, 65, previewWidth + 7, bottom - 15);
        float scale = Math.min(previewWidth * .85F, (bottom - 78) * .9F) * zoom;
        preview.render(graphics, choice, previewWidth / 2 + 8, (65 + bottom - 15) / 2 + (int)(scale * .3F), scale, rotation);
        graphics.disableScissor();
        graphics.drawCenteredString(font, text("drag"), previewWidth / 2 + 8, bottom - 13, 0x89989f);
        graphics.enableScissor(gridX - 3, 31, width - 9, bottom - 1);
        boolean inside = mouseY >= 31 && mouseY < bottom;
        for (var card : cards) card.render(graphics, mouseX, inside ? mouseY : -10000, partial);
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int track = bottom - 36, handle = Math.max(16, track * track / (track + maxScroll()));
            int y = 34 + (track - handle) * scroll / maxScroll();
            graphics.fill(width - 7, 34, width - 4, bottom - 2, 0xff1b252b);
            graphics.fill(width - 7, y, width - 4, y + handle, 0xff91a3a7);
        }
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partial);
        var status = CatSkins.message();
        graphics.drawString(font, font.plainSubstrByWidth(status.getString(), width - 24), 12, height - 49, CatSkins.licensed() ? 0xa7b4bc : 0xd8bf8c, false);
        if (mouseY >= height - 52 && mouseY < height - 34 && !status.getString().isEmpty()) hoverTip = status;
        if (hoverTip != null) graphics.renderTooltip(font, font.split(hoverTip, Math.min(250, width - 30)), mouseX, mouseY);
    }

    private int maxScroll() { return Math.max(0, (cards.size() + columns - 1) / columns * tileHeight - (bottom - 34)); }
    private static boolean same(CatSkin a, CatSkin b) { return a.variant().equals(b.variant()) && (!a.variant().equals("custom") || a.hash().equals(b.hash())); }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && name.visible && x >= 14 && x < previewWidth + 2 && y >= 36 && y < 61) {
            if (!name.isMouseOver(x, y)) { setFocused(name); name.setFocused(true); return true; }
        } else if (name.isFocused()) finishRename();
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (name.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { finishRename(); setFocused(null); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { updateName(); setFocused(null); return true; }
            if (key == GLFW.GLFW_KEY_TAB) finishRename();
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && x < previewWidth + 8 && y >= 65 && y < bottom) { rotation += (float)dx * 1.5F; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < previewWidth + 8) zoom = net.minecraft.util.Mth.clamp(zoom + (float)vertical * .08F, .65F, 1.3F);
        else { scroll = Math.clamp(scroll - (int)(vertical * 32), 0, maxScroll()); layoutCards(); }
        return true;
    }
    @Override public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty()) return;
        finishRename(); var screen = new NewCatSkinScreen(this); minecraft.setScreen(screen); screen.onFilesDrop(paths);
    }
    void saved(CatSkin skin) {
        choice = skin; waitingForLibrary = false; sync(); updateName();
        for (var card : cards) if (card.entry != null && same(card.entry.skin(), skin)) {
            int top = card.getY() + scroll;
            if (top - scroll < 34 || top + tileHeight - scroll > bottom) scroll = Math.clamp(top + tileHeight - bottom, 0, maxScroll());
            layoutCards(); break;
        }
    }
    @Override public void onClose() { finishRename(); minecraft.setScreen(parent); }
    private static Component text(String key) { return Component.translatable("skins.nekogenix." + key); }

    private final class Card extends Button {
        final SkinLibrary.Entry entry;
        Card(SkinLibrary.Entry entry) {
            super(0, 0, tileWidth - 4, tileHeight - 4, entry == null ? text("import") : entry.skin().variant().equals("custom") ? Component.literal(entry.name()) : text("variant." + entry.skin().variant()), button -> {}, DEFAULT_NARRATION);
            this.entry = entry;
        }
        boolean custom() { return entry != null && entry.skin().variant().equals("custom"); }
        boolean used() { return entry != null && same(entry.skin(), CatSkins.selected()); }
        boolean canUse() { return !used() && CatSkins.loaded() && CatSkins.licensed() && !CatSkins.checking(); }
        int actionY() { return getY() + getHeight() - 21; }
        int useX() { return getX() + getWidth() - (custom() ? 40 : 21); }
        void use() { if (entry != null && canUse()) { pick(entry.skin()); CatSkins.apply(entry.skin()); } }
        void delete() {
            if (!custom()) return;
            finishRename(); minecraft.setScreen(new ConfirmScreen(answer -> { if (answer) CatSkins.remove(entry.skin()); minecraft.setScreen(CatSkinScreen.this); }, text("delete_title"), text("delete_description")));
        }
        @Override public void onPress() {
            finishRename();
            if (entry == null) minecraft.setScreen(new NewCatSkinScreen(CatSkinScreen.this)); else pick(entry.skin());
        }
        @Override public void onClick(double x, double y) {
            if (entry != null && y >= actionY() && y < actionY() + 17) {
                if (!used() && x >= useX() && x < useX() + 17) { use(); return; }
                if (custom() && x >= getX() + getWidth() - 21) { delete(); return; }
            }
            onPress();
        }
        @Override public boolean mouseClicked(double x, double y, int button) { return y >= 31 && y < bottom && super.mouseClicked(x, y, button); }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            if (isFocused() && entry != null) {
                if (key == GLFW.GLFW_KEY_U) { use(); return true; }
                if (key == GLFW.GLFW_KEY_DELETE && custom()) { delete(); return true; }
            }
            return super.keyPressed(key, scan, modifiers);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean picked = entry != null && same(entry.skin(), choice), used = entry != null && same(entry.skin(), CatSkins.selected());
            boolean hovered = isHoveredOrFocused();
            graphics.fill(x, y, x + w, y + h, picked ? 0xff719b91 : hovered ? 0xff728790 : 0xff34424b);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, picked ? 0xff253b39 : 0xff19232a);
            int labelWidth = w - (used ? 26 : 10);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(getMessage().getString(), labelWidth), x + w / 2 - (used ? 6 : 0), y + 8, 0xffffff);
            if (entry == null) {
                graphics.fill(x + w / 2 - 1, y + h / 2 - 10, x + w / 2 + 2, y + h / 2 + 11, hovered ? 0xffd5e8e0 : 0xff8fa9a2);
                graphics.fill(x + w / 2 - 10, y + h / 2 - 1, x + w / 2 + 11, y + h / 2 + 2, hovered ? 0xffd5e8e0 : 0xff8fa9a2);
                return;
            }
            float scale = Math.min((w - 4) * .72F, (h - 24) * 1.05F);
            preview.render(graphics, entry.skin(), x + w / 2, y + h / 2 + (int)(scale * .4F), scale, 145);
            if (used) icon(graphics, 0, x + w - 20, y + 3, 0xffa6d6a4);
            if (hovered) {
                if (!used) action(graphics, 0, useX(), actionY(), mouseX, mouseY, canUse(), canUse() ? text("use") : text("license_required"));
                if (custom()) action(graphics, 1, x + w - 21, actionY(), mouseX, mouseY, true, text("delete"));
            }
            if (isHovered && mouseY < y + 22 && font.width(getMessage()) > labelWidth) hoverTip = getMessage();
        }
        @Override public void updateWidgetNarration(NarrationElementOutput output) {
            super.updateWidgetNarration(output);
            if (entry != null) output.add(NarratedElementType.USAGE, text("card_actions"));
        }
    }

    private void action(GuiGraphics graphics, int type, int x, int y, int mouseX, int mouseY, boolean enabled, Component tip) {
        boolean hover = mouseX >= x && mouseX < x + 17 && mouseY >= y && mouseY < y + 17;
        graphics.fill(x, y, x + 17, y + 17, hover ? 0xff7d9b92 : 0xff40534f);
        graphics.fill(x + 1, y + 1, x + 16, y + 16, hover ? 0xff304c43 : 0xff1b2c27);
        icon(graphics, type, x, y, enabled ? 0xffdcece2 : 0xff75817d);
        if (hover) hoverTip = tip;
    }
    private static void icon(GuiGraphics graphics, int type, int x, int y, int color) {
        x += 4; y += 4;
        if (type == 0) {
            for (int i = 0; i < 3; i++) graphics.fill(x + i, y + 4 + i, x + i + 2, y + 6 + i, color);
            for (int i = 0; i < 6; i++) graphics.fill(x + 2 + i, y + 6 - i, x + 4 + i, y + 8 - i, color);
        } else if (type == 1) {
            graphics.fill(x + 1, y + 1, x + 9, y + 2, color); graphics.fill(x + 3, y, x + 7, y + 1, color);
            graphics.fill(x + 2, y + 3, x + 3, y + 9, color); graphics.fill(x + 7, y + 3, x + 8, y + 9, color);
            graphics.fill(x + 2, y + 8, x + 8, y + 9, color); graphics.fill(x + 4, y + 3, x + 6, y + 7, color);
        } else if (type == 2) {
            for (int i = 0; i < 7; i++) graphics.fill(x + i, y + 7 - i, x + i + 2, y + 9 - i, color);
            graphics.fill(x, y + 9, x + 2, y + 10, color);
        } else if (type == 3) {
            graphics.fill(x + 1, y + 1, x + 6, y + 6, color); graphics.fill(x + 2, y + 2, x + 5, y + 5, 0xff243c37);
            graphics.fill(x + 5, y + 5, x + 9, y + 7, color); graphics.fill(x + 7, y + 6, x + 9, y + 9, color);
        }
    }
    private final class VerifyButton extends Button {
        VerifyButton(int x, int y) { super(x, y, 18, 18, text("verify"), button -> CatSkins.verify(), DEFAULT_NARRATION); setTooltip(Tooltip.create(text("verify"))); }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            graphics.fill(getX(), getY(), getX() + 18, getY() + 18, isHoveredOrFocused() ? 0xff405b57 : 0xff1c302d);
            icon(graphics, 3, getX(), getY(), active ? 0xffd8bf8c : 0xff75817d);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
    public static final class Menu {
        @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
            if (!(event.getScreen() instanceof SkinCustomizationScreen screen)) return;
            for (var listener : event.getListenersList()) if (listener instanceof OptionsList list) {
                list.addSmall(List.of(Button.builder(text("title"), button -> net.minecraft.client.Minecraft.getInstance().setScreen(new CatSkinScreen(screen))).build())); return;
            }
        }
    }
}
