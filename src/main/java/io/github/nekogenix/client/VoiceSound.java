package io.github.nekogenix.client;

import io.github.nekogenix.config.ClientConfig;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;

final class VoiceSound extends AbstractSoundInstance implements TickableSoundInstance {
    private final SoundInstance original;

    VoiceSound(SoundInstance original) {
        // Master Volume still applies; the Players slider controls action sounds independently.
        super(original.getLocation(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.original = original;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        var resolved = original.resolve(manager);
        sound = original.getSound();
        return resolved;
    }

    @Override
    public float getVolume() { return original.getVolume() * ClientConfig.NEKO_VOICE_VOLUME.get().floatValue(); }

    @Override
    public float getPitch() { return original.getPitch(); }

    @Override
    public double getX() { return original.getX(); }

    @Override
    public double getY() { return original.getY(); }

    @Override
    public double getZ() { return original.getZ(); }

    @Override
    public boolean isLooping() { return original.isLooping(); }

    @Override
    public boolean isRelative() { return original.isRelative(); }

    @Override
    public int getDelay() { return original.getDelay(); }

    @Override
    public Attenuation getAttenuation() { return original.getAttenuation(); }

    @Override
    public boolean canPlaySound() { return original.canPlaySound(); }

    @Override
    public boolean canStartSilent() { return original.canStartSilent(); }

    @Override
    public void tick() {
        if (original instanceof TickableSoundInstance ticking) ticking.tick();
    }

    @Override
    public boolean isStopped() { return original instanceof TickableSoundInstance ticking && ticking.isStopped(); }
}
