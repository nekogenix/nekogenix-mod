package io.github.nekogenix.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class NekoState implements INBTSerializable<CompoundTag> {
    public static final int DAY = 24_000;
    public static final int SHOCK_DURATION = 1_200;
    public static final int AGILITY = 1;
    public static final int VISION = 2;
    public static final int PAWS = 4;
    public enum Phase { HUMAN, SHOCK, TRANSFORMING, NEKO }

    private Phase phase = Phase.HUMAN;
    private int elapsed;
    private int abilities;
    private int lives;
    public boolean visionOwned;
    public transient boolean applyingVision;

    public Phase phase() { return phase; }
    public int elapsed() { return elapsed; }
    public boolean isNeko() { return phase == Phase.NEKO; }
    public boolean isTransforming() { return phase == Phase.SHOCK || phase == Phase.TRANSFORMING; }
    public boolean has(int ability) { return (abilities & ability) != 0; }
    public int stage() { return phase == Phase.TRANSFORMING ? Math.min(2, elapsed / DAY) : 0; }
    public int stageRemaining() { return DAY - elapsed % DAY; }

    public boolean begin() {
        if (phase != Phase.HUMAN) return false;
        phase = Phase.SHOCK;
        elapsed = 0;
        abilities = 0;
        lives = 0;
        return true;
    }

    /** Advances only while the living player actually ticks; daylight and sleep never change this timer. */
    public boolean tick(RandomSource random) {
        if (phase == Phase.HUMAN || phase == Phase.NEKO) return false;
        elapsed++;
        if (phase == Phase.SHOCK && elapsed >= SHOCK_DURATION) {
            phase = Phase.TRANSFORMING;
            elapsed = 0;
            return true;
        }
        if (phase == Phase.TRANSFORMING && elapsed % DAY == 0) {
            int remaining = 7 & ~abilities;
            int choice = random.nextInt(Integer.bitCount(remaining));
            for (int bit = 1; bit <= PAWS; bit <<= 1) {
                if ((remaining & bit) != 0 && choice-- == 0) {
                    abilities |= bit;
                    break;
                }
            }
            if (elapsed >= 3 * DAY) {
                phase = Phase.NEKO;
                lives = 9;
            }
            return true;
        }
        return false;
    }

    public boolean loseLife() {
        if (!isNeko() || --lives > 0) return false;
        reset();
        return true;
    }

    public void cancelTransformation() {
        if (isTransforming()) reset();
    }

    private void reset() {
        phase = Phase.HUMAN;
        elapsed = abilities = lives = 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("phase", phase.ordinal());
        tag.putInt("elapsed", elapsed);
        tag.putInt("abilities", abilities);
        tag.putInt("lives", lives);
        tag.putBoolean("vision_owned", visionOwned);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        phase = Phase.values()[Mth.clamp(tag.getInt("phase"), 0, Phase.values().length - 1)];
        elapsed = Mth.clamp(tag.getInt("elapsed"), 0, 3 * DAY);
        abilities = tag.getInt("abilities") & 7;
        lives = isNeko() ? Mth.clamp(tag.getInt("lives"), 0, 9) : 0;
        visionOwned = tag.getBoolean("vision_owned");
    }
}
