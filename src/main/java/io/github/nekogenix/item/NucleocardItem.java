package io.github.nekogenix.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class NucleocardItem extends Item {
    private final boolean filled;

    public NucleocardItem(boolean filled) {
        super(new Properties().stacksTo(1));
        this.filled = filled;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.nekogenix." + (filled ? "cat_nucleocard" : "blank_nucleocard")).withStyle(ChatFormatting.GRAY));
        if (filled) tooltip.add(Component.translatable("tooltip.nekogenix.nucleocard_return").withStyle(ChatFormatting.GRAY));
    }
}
