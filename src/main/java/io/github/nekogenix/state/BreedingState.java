package io.github.nekogenix.state;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class BreedingState implements INBTSerializable<CompoundTag> {
    public static final int LOVE_TICKS = 600;
    public static final int COOLDOWN_TICKS = 6000;
    private int love;
    private int cooldown;
    private UUID cause;
    public transient UUID partner;
    public transient int courtship;

    public boolean canFeed() { return love == 0 && cooldown == 0; }
    public boolean inLove() { return love > 0 && cooldown == 0; }
    public UUID cause() { return cause; }

    public void feed(UUID feeder) { love = LOVE_TICKS; cause = feeder; }
    public void finish() { clearLove(); cooldown = COOLDOWN_TICKS; }
    public void clearLove() { love = 0; cause = null; partner = null; courtship = 0; }

    public void tick() {
        if (cooldown > 0) cooldown--;
        if (love > 0 && --love == 0) clearLove();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("love", love);
        tag.putInt("cooldown", cooldown);
        if (cause != null) tag.putUUID("cause", cause);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        love = Mth.clamp(tag.getInt("love"), 0, LOVE_TICKS);
        cooldown = Mth.clamp(tag.getInt("cooldown"), 0, COOLDOWN_TICKS);
        cause = tag.hasUUID("cause") ? tag.getUUID("cause") : null;
        partner = null;
        courtship = 0;
    }
}
