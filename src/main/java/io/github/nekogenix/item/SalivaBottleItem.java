package io.github.nekogenix.item;

import io.github.nekogenix.gameplay.SalivaExposure;
import io.github.nekogenix.gameplay.BottledEffects;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public final class SalivaBottleItem extends Item implements ProjectileItem {
    private final boolean splash;

    public SalivaBottleItem(boolean splash) {
        super(new Item.Properties().stacksTo(1));
        this.splash = splash;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModTooltips.saliva(context, tooltip);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!splash) return ItemUtils.startUsingInstantly(level, player, hand);
        return BottledEffects.throwBottle(level, player, hand);
    }

    @Override public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return BottledEffects.projectile(level, pos, stack);
    }

    @Override public DispenseConfig createDispenseConfig() { return BottledEffects.dispenseConfig(); }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            SalivaExposure.schedule(entity);
            level.gameEvent(entity, GameEvent.DRINK, entity.position());
        }
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }
        stack.shrink(1);
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return splash ? UseAnim.NONE : UseAnim.DRINK; }
}
