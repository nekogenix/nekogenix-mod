package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import io.github.nekogenix.config.ClientConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class CameraOptions {
    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen)) return;
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof OptionInstance.OptionInstanceSliderButton<?> fov)) continue;
            event.addListener(new HeightSlider(fov));
            return;
        }
    }

    private static final class HeightSlider extends net.minecraft.client.gui.components.AbstractSliderButton {
        private final AbstractWidget anchor;

        HeightSlider(AbstractWidget anchor) {
            super(anchor.getX(), anchor.getY() + anchor.getHeight() + 4, anchor.getWidth(), anchor.getHeight(), Component.empty(), ClientConfig.cameraHeight() / 100.0);
            this.anchor = anchor;
            updateMessage();
        }

        @Override public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            setPosition(anchor.getX(), anchor.getY() + anchor.getHeight() + 4);
            value = ClientConfig.cameraHeight() / 100.0;
            updateMessage();
            super.renderWidget(graphics, mouseX, mouseY, partial);
        }

        @Override protected void updateMessage() {
            int percent = (int)Math.round(value * 100);
            var caption = Component.translatable("options.nekogenix.camera_height");
            setMessage(percent == 0 || percent == 100
                    ? Options.genericValueLabel(caption, Component.translatable("options.nekogenix.camera_height." + (percent == 0 ? "cat" : "human")))
                    : Component.translatable("options.percent_value", caption, percent));
        }

        @Override protected void applyValue() {
            ClientConfig.CAMERA_HEIGHT.set((int)Math.round(value * 100));
            ClientConfig.SPEC.save();
        }
    }

    private CameraOptions() {}
}
