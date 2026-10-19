package io.github.nekogenix.gameplay;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;

public final class FelineScent {
    public static final int HOSTILE = 0xff5555, NEUTRAL = 0xffff55, PASSIVE = 0x55ff55, PLAYER = 0xffffff;
    public static final int HORIZONTAL_RANGE = 30, VERTICAL_RANGE = 15;

    public static boolean inRange(LivingEntity observer, LivingEntity target) {
        double dx = target.getX() - observer.getX(), dz = target.getZ() - observer.getZ();
        return target != observer && (target instanceof Mob || target instanceof Player) && target.isAlive() && !target.isSpectator()
                && Math.abs(target.getY() - observer.getY()) <= VERTICAL_RANGE
                && Math.abs(dx) <= HORIZONTAL_RANGE && Math.abs(dz) <= HORIZONTAL_RANGE;
    }

    public static int color(LivingEntity entity) {
        if (entity instanceof Player) return PLAYER;
        if (entity instanceof Creeper || entity instanceof Phantom) return PASSIVE;
        if (entity instanceof Rabbit rabbit) return rabbit.getVariant() == Rabbit.Variant.EVIL ? HOSTILE : PASSIVE;
        if (entity.getType() == EntityType.AXOLOTL || entity.getType() == EntityType.FROG) return PASSIVE;
        if (entity instanceof Cat || entity instanceof NeutralMob || entity instanceof Llama) return NEUTRAL;
        if (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER
                || entity instanceof Pufferfish || entity instanceof Goat) return HOSTILE;
        if (entity instanceof Mob mob && mob.getAttribute(Attributes.ATTACK_DAMAGE) != null
                && mob.getAttributeValue(Attributes.ATTACK_DAMAGE) > 0) return NEUTRAL;
        return PASSIVE;
    }

    public static final class Focus {
        private static final int DELAY = 100, FADE = 20;
        private int held, previous, strength;

        /** Returns true only when a fresh five second hold activates the sense. */
        public boolean tick(boolean eligible, boolean crouching) {
            previous = strength;
            if (!eligible) { reset(); return false; }
            if (!crouching) { held = 0; strength = Math.max(0, strength - 1); return false; }
            if (strength > 0) { strength = Math.min(FADE, strength + 1); return false; }
            if (++held < DELAY) return false;
            strength = 1;
            return true;
        }

        public float opacity(float partial) { return Mth.lerp(partial, previous, strength) / FADE; }
        public boolean visible() { return previous > 0 || strength > 0; }
        public void reset() { held = previous = strength = 0; }
    }

    private FelineScent() {}
}
