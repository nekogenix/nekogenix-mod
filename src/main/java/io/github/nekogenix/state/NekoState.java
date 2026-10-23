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
    public enum Phase { HUMAN, SHOCK, TRANSFORMING, NEKO, CAT_TRANSFORMING, CAT }

    private Phase phase = Phase.HUMAN;
    private int elapsed;
    private int abilities;
    private int lives;
    private boolean accelerated;
    public boolean visionOwned;
    public transient boolean applyingVision;

    public Phase phase() { return phase; }
    public int elapsed() { return elapsed; }
    public boolean accelerated() { return accelerated; }
    public boolean isNeko() { return phase == Phase.NEKO || phase == Phase.CAT_TRANSFORMING || phase == Phase.CAT; }
    public boolean isTransforming() { return phase == Phase.SHOCK || phase == Phase.TRANSFORMING || phase == Phase.CAT_TRANSFORMING; }
    public boolean isCat() { return phase == Phase.CAT; }
    public int lives() { return lives; }
    public boolean stabilize() {
        if (phase != Phase.CAT_TRANSFORMING) return false;
        phase = Phase.NEKO;
        elapsed = 0;
        return true;
    }
    public boolean has(int ability) { return (abilities & ability) != 0; }
    public int sicknessDuration() { return accelerated ? 3 * SHOCK_DURATION : SHOCK_DURATION; }
    public int stageDuration() { return accelerated ? DAY / 4 : DAY; }
    public int stage() { return phase == Phase.TRANSFORMING ? Math.min(2, elapsed / stageDuration()) : 0; }
    public int stageRemaining() { return stageDuration() - elapsed % stageDuration(); }

    public boolean accelerate() {
        if (phase != Phase.SHOCK || accelerated) return false;
        accelerated = true;
        return true;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        elapsed = 0;
        accelerated = false;
        abilities = switch (phase) {
            case HUMAN, SHOCK, TRANSFORMING -> 0;
            case NEKO, CAT_TRANSFORMING, CAT -> AGILITY | VISION | PAWS;
        };
        lives = isNeko() ? 9 : 0;
    }

    public boolean setElapsed(int ticks) {
        int maximum = switch (phase) {
            case SHOCK -> sicknessDuration() - 1;
            case TRANSFORMING -> 3 * stageDuration() - 1;
            case CAT_TRANSFORMING -> stageDuration() - 1;
            default -> 0;
        };
        int next = Mth.clamp(ticks, 0, Math.max(0, maximum));
        if (elapsed == next) return false;
        elapsed = next;
        return true;
    }

    public boolean setAbility(int ability, boolean enabled) {
        int mask = ability & (AGILITY | VISION | PAWS);
        int next = enabled ? abilities | mask : abilities & ~mask;
        if (abilities == next) return false;
        abilities = next;
        return true;
    }

    public boolean setLives(int lives) {
        if (!isNeko()) return false;
        int next = Mth.clamp(lives, 1, 9);
        if (this.lives == next) return false;
        this.lives = next;
        return true;
    }

    public boolean setAccelerated(boolean accelerated) {
        if (!isTransforming() || this.accelerated == accelerated) return false;
        this.accelerated = accelerated;
        setElapsed(elapsed);
        return true;
    }

    public boolean advance(RandomSource random) {
        switch (phase) {
            case HUMAN -> begin();
            case SHOCK -> {
                phase = Phase.TRANSFORMING;
                elapsed = 0;
            }
            case TRANSFORMING -> {
                int boundary = (elapsed / stageDuration() + 1) * stageDuration();
                elapsed = Math.min(boundary - 1, 3 * stageDuration() - 1);
                tick(random);
            }
            case CAT_TRANSFORMING -> {
                phase = Phase.CAT;
                elapsed = 0;
            }
            case NEKO, CAT -> { return false; }
        }
        return true;
    }

    public boolean begin() {
        if (phase != Phase.HUMAN) return false;
        phase = Phase.SHOCK;
        elapsed = 0;
        abilities = 0;
        lives = 0;
        accelerated = false;
        return true;
    }

    public boolean tick(RandomSource random) {
        if (!isTransforming()) return false;
        elapsed++;
        if (phase == Phase.SHOCK && elapsed >= sicknessDuration()) {
            phase = Phase.TRANSFORMING;
            elapsed = 0;
            return true;
        }
        if (phase == Phase.CAT_TRANSFORMING && elapsed >= stageDuration()) {
            phase = Phase.CAT;
            elapsed = 0;
            return true;
        }
        if (phase == Phase.TRANSFORMING && elapsed % stageDuration() == 0) {
            int remaining = 7 & ~abilities;
            int choice = random.nextInt(Integer.bitCount(remaining));
            for (int bit = 1; bit <= PAWS; bit <<= 1) {
                if ((remaining & bit) != 0 && choice-- == 0) {
                    abilities |= bit;
                    break;
                }
            }
            if (elapsed >= 3 * stageDuration()) {
                abilities = AGILITY | VISION | PAWS;
                phase = Phase.CAT_TRANSFORMING;
                elapsed = 0;
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
        if (isTransforming() && !isNeko()) reset();
    }

    public void reset() {
        phase = Phase.HUMAN;
        elapsed = abilities = lives = 0;
        accelerated = false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("phase", phase.ordinal());
        tag.putInt("elapsed", elapsed);
        tag.putInt("abilities", abilities);
        tag.putInt("lives", lives);
        tag.putBoolean("accelerated", accelerated);
        tag.putBoolean("vision_owned", visionOwned);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        phase = Phase.values()[Mth.clamp(tag.getInt("phase"), 0, Phase.values().length - 1)];
        accelerated = phase != Phase.HUMAN && tag.getBoolean("accelerated");
        elapsed = Mth.clamp(tag.getInt("elapsed"), 0, 3 * stageDuration());
        abilities = tag.getInt("abilities") & 7;
        lives = isNeko() ? Mth.clamp(tag.getInt("lives"), 0, 9) : 0;
        visionOwned = tag.getBoolean("vision_owned");
    }
}
