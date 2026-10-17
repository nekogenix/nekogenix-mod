package io.github.nekogenix;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VoiceSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Nekogenix.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MEOW = sound("voice_meow");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRAY_MEOW = sound("voice_stray_meow");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEG = sound("voice_beg");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURR = sound("voice_purr");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURREOW = sound("voice_purreow");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Content.id(name)));
    }

    public static boolean isVoice(net.minecraft.resources.ResourceLocation id) {
        return id.equals(MEOW.getId()) || id.equals(STRAY_MEOW.getId()) || id.equals(BEG.getId())
                || id.equals(PURR.getId()) || id.equals(PURREOW.getId());
    }

    static void register(IEventBus bus) { SOUNDS.register(bus); }

    private VoiceSounds() {}
}
