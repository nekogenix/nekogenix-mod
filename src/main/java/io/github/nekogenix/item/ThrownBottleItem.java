package io.github.nekogenix.item;

import io.github.nekogenix.gameplay.BottledEffects;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ThrownBottleItem extends Item implements ProjectileItem {
    private final boolean sweet, lingering;

    public ThrownBottleItem(boolean sweet, boolean lingering) {
        super(new Item.Properties().stacksTo(1));
        this.sweet = sweet;
        this.lingering = lingering;
    }

    public boolean sweet() { return sweet; }
    public boolean lingering() { return lingering; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return BottledEffects.throwBottle(level, player, hand);
    }

    @Override public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return BottledEffects.projectile(level, position, stack);
    }

    @Override public DispenseConfig createDispenseConfig() { return BottledEffects.dispenseConfig(); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (sweet) ModTooltips.sweetAmethyst(context, tooltip, lingering ? .25F : 1F);
        else ModTooltips.saliva(context, tooltip);
    }
}
