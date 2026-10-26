package io.github.nekogenix.gameplay;

import io.github.nekogenix.Nekogenix;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineAdvancements {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Nekogenix.ID);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> CAT_TRANSFORMATION_COMPLETED =
            TRIGGERS.register("cat_transformation_completed", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> DUAL_INJECTION_COMPLETED =
            TRIGGERS.register("dual_injection_completed", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> SINGLE_INJECTION_COMPLETED =
            TRIGGERS.register("single_injection_completed", PlayerTrigger::new);

    public static void register(IEventBus bus) { TRIGGERS.register(bus); }

    @SubscribeEvent
    public static void layout(OnDatapackSyncEvent event) {
        var advancements = event.getPlayerList().getServer().getAdvancements();
        position(advancements, "root", 0, 1.5F);
        position(advancements, "what_could_go_wrong", 1.5F, 1.5F);
        position(advancements, "dual_injection", 1.5F, 0);
        position(advancements, "finish_what_you_started", 3, 1);
        position(advancements, "weeb", 3, 2);
    }

    private static void position(ServerAdvancementManager advancements, String name, float x, float y) {
        var advancement = advancements.get(ResourceLocation.fromNamespaceAndPath(Nekogenix.ID, name));
        if (advancement != null) advancement.value().display().ifPresent(display -> display.setLocation(x, y));
    }

    private FelineAdvancements() {}
}
