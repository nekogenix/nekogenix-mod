package io.github.nekogenix.client;

import io.github.nekogenix.Nekogenix;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class ControlOptions {
    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ControlsScreen) && !(event.getScreen() instanceof AccessibilityOptionsScreen)) return;
        for (var listener : event.getListenersList()) {
            if (listener instanceof OptionsList list) {
                if (event.getScreen() instanceof AccessibilityOptionsScreen) addAccessibilityOptions(list);
                else list.addSmall(ClientOptions.doubleJump(), ClientOptions.felineVision());
                return;
            }
        }
    }

    private static void addAccessibilityOptions(OptionsList list) {
        var minecraft = Minecraft.getInstance();
        var autoJump = list.findOption(minecraft.options.autoJump());
        int insertion = list.children().size();
        for (int i = 0; i < list.children().size(); i++) {
            ContainerObjectSelectionList.Entry<?> entry = list.children().get(i);
            if (entry.children().contains(autoJump)) {
                insertion = i + 1;
                break;
            }
        }
        for (var option : List.of(ClientOptions.doubleJump(), ClientOptions.felineVision())) {
            list.addSmall(new StringWidget(Component.empty(), minecraft.font), option.createButton(minecraft.options));
        }
        Collections.rotate(list.children().subList(insertion, list.children().size()), 2);
    }

    private ControlOptions() {}
}
