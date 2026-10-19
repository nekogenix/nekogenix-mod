package io.github.nekogenix.client;

import io.github.nekogenix.client.skin.CatSkinScreen;
import io.github.nekogenix.config.ClientConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class ModOptionsScreen extends OptionsSubScreen {
    public ModOptionsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("options.nekogenix.title"));
    }

    @Override
    protected void addOptions() {
        list.addBig(ClientOptions.voiceVolume());
        list.addBig(ClientOptions.cameraHeight());
        list.addSmall(List.of(Button.builder(Component.translatable("skins.nekogenix.title"),
                button -> minecraft.setScreen(new CatSkinScreen(this))).width(310).build()));
    }

    @Override
    public void removed() {
        if (list != null) list.applyUnsavedChanges();
        ClientConfig.SPEC.save();
        super.removed();
    }
}
