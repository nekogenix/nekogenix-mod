package io.github.nekogenix.item;

import io.github.nekogenix.Content;
import io.github.nekogenix.gameplay.BottledEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class SalivaProjectile extends ThrowableItemProjectile {
    public SalivaProjectile(EntityType<? extends SalivaProjectile> type, Level level) { super(type, level); }
    public SalivaProjectile(Level level, LivingEntity owner) { super(Content.SALIVA_PROJECTILE.get(), owner, level); }

    @Override
    protected Item getDefaultItem() { return Content.SPLASH_SALIVA.get(); }

    @Override
    protected double getDefaultGravity() { return 0.05; }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        BottledEffects.impact(this, result);
    }
}
