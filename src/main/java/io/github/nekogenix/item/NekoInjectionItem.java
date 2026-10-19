package io.github.nekogenix.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class NekoInjectionItem extends Item {
    private final boolean stabilizing;

    public NekoInjectionItem() { this(false); }

    public NekoInjectionItem(boolean stabilizing) {
        super(new Item.Properties().stacksTo(1));
        this.stabilizing = stabilizing;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stabilizing) ModTooltips.stabilizer(tooltip);
        else ModTooltips.injection(context, tooltip);
    }
}
