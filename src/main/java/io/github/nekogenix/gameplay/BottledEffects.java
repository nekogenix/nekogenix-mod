package io.github.nekogenix.gameplay;

import io.github.nekogenix.Content;
import io.github.nekogenix.item.SalivaProjectile;
import io.github.nekogenix.item.ThrownBottleItem;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class BottledEffects {
    public static boolean sweet(ItemStack stack) { return stack.getItem() instanceof ThrownBottleItem item && item.sweet(); }
    public static boolean lingering(ItemStack stack) { return stack.getItem() instanceof ThrownBottleItem item && item.lingering(); }
    public static int color(ItemStack stack) { return sweet(stack) ? 0xBD91D3 : 0x879AC7; }

    public static InteractionResultHolder<ItemStack> throwBottle(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                lingering(stack) ? SoundEvents.LINGERING_POTION_THROW : SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, .5F, .8F + level.random.nextFloat() * .4F);
        if (!level.isClientSide) {
            var projectile = new SalivaProjectile(level, player);
            projectile.setItem(stack);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), -20F, .5F, 1F);
            level.addFreshEntity(projectile);
        }
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static SalivaProjectile projectile(Level level, Position position, ItemStack stack) {
        var projectile = new SalivaProjectile(Content.SALIVA_PROJECTILE.get(), level);
        projectile.setPos(position.x(), position.y(), position.z());
        projectile.setItem(stack);
        return projectile;
    }

    public static ProjectileItem.DispenseConfig dispenseConfig() {
        return ProjectileItem.DispenseConfig.builder().power(1.375F).uncertainty(3F).build();
    }

    public static void impact(SalivaProjectile projectile, HitResult hit) {
        var level = projectile.level();
        if (level.isClientSide) return;
        var stack = projectile.getItem();
        boolean sweet = sweet(stack);
        if (lingering(stack)) {
            var cloud = new AreaEffectCloud(level, projectile.getX(), projectile.getY(), projectile.getZ());
            if (projectile.getOwner() instanceof LivingEntity owner) cloud.setOwner(owner);
            cloud.setRadius(3);
            cloud.setRadiusOnUse(-.5F);
            cloud.setWaitTime(10);
            cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
            var effect = sweet ? new MobEffectInstance(Content.SWEETNESS, SweetnessEffect.DURATION / 4, 0, false, false, true)
                    : new MobEffectInstance(Content.SALIVA_CLOUD, 1, 0, false, false, false);
            cloud.setPotionContents(new PotionContents(Optional.empty(), Optional.of(color(stack)), List.of(effect)));
            level.addFreshEntity(cloud);
        } else {
            for (var target : level.getEntitiesOfClass(LivingEntity.class, projectile.getBoundingBox().inflate(4, 2, 4))) {
                double distance = target.distanceToSqr(projectile);
                boolean direct = hit instanceof EntityHitResult entityHit && entityHit.getEntity() == target;
                if (!target.isAffectedByPotions() || !target.isAlive() || distance >= 16 && !direct) continue;
                if (sweet) {
                    double intensity = direct ? 1 : 1 - Math.sqrt(distance) / 4;
                    int duration = (int)(SweetnessEffect.DURATION * intensity + .5);
                    if (duration > 20) target.addEffect(new MobEffectInstance(Content.SWEETNESS, duration, 0, false, false, true), projectile.getEffectSource());
                } else SalivaExposure.schedule(target);
            }
        }
        level.levelEvent(2002, projectile.blockPosition(), color(stack));
        projectile.discard();
    }

    private BottledEffects() {}
}
