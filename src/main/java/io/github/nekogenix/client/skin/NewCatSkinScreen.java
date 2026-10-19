package io.github.nekogenix.client.skin;

import io.github.nekogenix.skin.CatSkin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class NewCatSkinScreen extends Screen {
    private final CatSkinScreen parent;
    private CatSkinPreview preview;
    private CatSkin draft, template, placeholder;
    private EditBox name;
    private Button save, use, browse, export;
    private Component status = Component.empty();
    private String filename = "";
    private int panel, controls, controlWidth, request;
    private float rotation = 145, zoom = 1;
    private boolean reading, saving, picker;

    public NewCatSkinScreen(CatSkinScreen parent) { super(text("import")); this.parent = parent; }

    @Override protected void init() {
        CatSkins.load(); preview = new CatSkinPreview();
        if (template == null) {
            try { template = CatSkinTemplate.create(); placeholder = CatSkinTemplate.placeholder(); }
            catch (java.io.IOException e) { status = text("storage_failed"); }
        }
        panel = Math.max(104, width / 2 - 16); controls = panel + 24; controlWidth = width - controls - 12;
        String previous = name == null ? "" : name.getValue();
        name = addRenderableWidget(new EditBox(font, controls, 46, controlWidth, 20, text("name")));
        name.setMaxLength(32); name.setValue(previous); name.setHint(text("name_hint"));
        name.setResponder(value -> refresh());
        browse = addRenderableWidget(Button.builder(text("browse"), button -> fileDialog(false)).bounds(controls, 110, controlWidth, 20).build());
        export = addRenderableWidget(Button.builder(text("template"), button -> fileDialog(true)).bounds(controls, 136, controlWidth, 20)
                .tooltip(Tooltip.create(text("template_hint"))).build());
        int buttonWidth = (width - 32) / 3;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(12, height - 28, buttonWidth, 20).build());
        save = addRenderableWidget(Button.builder(text("save"), button -> save(false)).bounds(16 + buttonWidth, height - 28, buttonWidth, 20).build());
        use = addRenderableWidget(Button.builder(text("save_use"), button -> save(true)).bounds(20 + buttonWidth * 2, height - 28, buttonWidth, 20)
                .tooltip(Tooltip.create(text("save_offline"))).build());
        refresh(); setInitialFocus(name);
    }

    private void refresh() {
        if (save == null) return;
        save.active = draft != null && !reading && !saving && !name.getValue().isBlank();
        use.active = save.active && CatSkins.licensed() && !CatSkins.checking();
        browse.active = !saving && !picker;
        export.active = template != null && !saving && !picker;
    }
    @Override public void tick() { refresh(); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics, mouseX, mouseY, partial);
        graphics.fill(8, 30, panel + 8, height - 38, 0xd010171c);
        graphics.fill(panel + 16, 30, width - 8, height - 38, 0xc00b1014);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        graphics.drawString(font, text("name"), controls, 34, 0xffffff, false);
        graphics.drawWordWrap(font, text("requirements"), controls, 76, controlWidth, 0xa7b4bc);
        graphics.enableScissor(9, 31, panel + 7, height - 54);
        float scale = Math.min(panel * .85F, (height - 96) * .9F) * zoom;
        CatSkin skin = draft != null ? draft : placeholder;
        if (skin != null) preview.render(graphics, skin, panel / 2 + 8, (height - 24) / 2 + (int)(scale * .3F), scale, rotation);
        graphics.disableScissor();
        graphics.drawCenteredString(font, text("drag"), panel / 2 + 8, height - 51, 0x89989f);
        graphics.drawString(font, font.plainSubstrByWidth(filename.isEmpty() ? text("no_file").getString() : filename, controlWidth), controls, 166, 0xffffff, false);
        Component hint = status.getString().isEmpty() && !CatSkins.licensed() ? text("save_offline") : status;
        graphics.enableScissor(controls, 180, width - 8, height - 36);
        graphics.drawWordWrap(font, hint, controls, 182, controlWidth, 0xd8bf8c);
        graphics.disableScissor();
        if (!hint.getString().isEmpty() && mouseX >= controls && mouseY >= 178 && mouseY < height - 35)
            graphics.renderTooltip(font, font.split(hint, Math.min(250, width - 30)), mouseX, mouseY);
    }

    @Override public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty() || saving) return;
        Path file = paths.getFirst(); int expected = ++request;
        draft = null; reading = true; status = text("reading"); filename = file.getFileName().toString(); refresh();
        CatSkins.previewFile(file, skin -> {
            if (request != expected) return;
            draft = skin; reading = false; status = text("ready_save");
            if (name.getValue().isBlank()) name.setValue(filename.replaceFirst("(?i)\\.png$", ""));
            refresh();
        }, error -> { if (request == expected) { reading = false; status = error; refresh(); } });
    }

    public void save(boolean apply) {
        if (!save.active || apply && !use.active) return;
        saving = true; refresh();
        CatSkins.saveDraft(name.getValue(), draft, skin -> {
            saving = false;
            if (apply) CatSkins.apply(skin);
            if (minecraft.screen == this) { minecraft.setScreen(parent); parent.saved(skin); }
        }, error -> { saving = false; status = error; refresh(); });
    }

    public void saveTemplate(Path path) {
        if (template == null) return;
        byte[] bytes = template.png();
        CompletableFuture.runAsync(() -> {
            try { Files.write(path, bytes); minecraft.execute(() -> status = text("template_saved")); }
            catch (java.io.IOException | SecurityException e) { minecraft.execute(() -> status = text("storage_failed")); }
        });
    }

    private void fileDialog(boolean templateFile) {
        if (picker) return;
        picker = true; refresh();
        CompletableFuture.runAsync(() -> {
            String selected = null; Component failure = null;
            try (var stack = MemoryStack.stackPush()) {
                var patterns = stack.mallocPointer(1); patterns.put(stack.UTF8("*.png")).flip();
                selected = templateFile ? TinyFileDialogs.tinyfd_saveFileDialog(text("template").getString(), "cat-skin-template.png", patterns, "PNG")
                        : TinyFileDialogs.tinyfd_openFileDialog(text("browse").getString(), "", patterns, "PNG", false);
            } catch (RuntimeException | LinkageError e) { failure = text("file_dialog_failed"); }
            String file = selected; Component error = failure;
            minecraft.execute(() -> {
                picker = false;
                if (error != null) status = error;
                else if (file != null) {
                    if (templateFile) saveTemplate(Path.of(file)); else onFilesDrop(List.of(Path.of(file)));
                }
                refresh();
            });
        });
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && x < panel + 8 && y >= 30 && y < height - 38) { rotation += (float)dx * 1.5F; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < panel + 8) { zoom = net.minecraft.util.Mth.clamp(zoom + (float)vertical * .08F, .65F, 1.3F); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void onClose() { if (!saving) { request++; minecraft.setScreen(parent); } }
    private static Component text(String key) { return Component.translatable("skins.nekogenix." + key); }
}
