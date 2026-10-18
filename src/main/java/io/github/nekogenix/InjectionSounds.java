package io.github.nekogenix;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class InjectionSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Nekogenix.ID);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OCEAN = List.of(
            sound("ocean_animal"), sound("ocean_dark_one"), sound("ocean_dark_two"),
            sound("ocean_dark_three"), sound("ocean_dark_four"));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Content.id(name)));
    }

    static void register(IEventBus bus) { SOUNDS.register(bus); }

    private InjectionSounds() {}
}
