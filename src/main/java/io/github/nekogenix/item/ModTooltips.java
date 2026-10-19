package io.github.nekogenix.item;

import io.github.nekogenix.Content;
import io.github.nekogenix.state.NekoState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;

final class ModTooltips {
    static void injection(Item.TooltipContext context, List<Component> tooltip) {
        PotionContents.addPotionTooltip(List.of(
                new MobEffectInstance(Content.SHOCK, NekoState.SHOCK_DURATION),
                new MobEffectInstance(Content.TRANSFORMING, 3 * NekoState.DAY),
                new MobEffectInstance(Content.CAT_TRANSFORMING, NekoState.DAY)), tooltip::add, 1F, context.tickRate());
        line(tooltip, "injection_use", ChatFormatting.GRAY);
        line(tooltip, "stages", ChatFormatting.GRAY);
        line(tooltip, "stage_ability", ChatFormatting.GRAY);
        line(tooltip, "cat_stage", ChatFormatting.GRAY);
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));
        line(tooltip, "agility", ChatFormatting.BLUE);
        line(tooltip, "double_jump", ChatFormatting.BLUE);
        line(tooltip, "vision", ChatFormatting.BLUE);
        line(tooltip, "paws", ChatFormatting.BLUE);
    }

    static void stabilizer(List<Component> tooltip) {
        line(tooltip, "injection_use", ChatFormatting.GRAY);
        line(tooltip, "stabilizing_injection", ChatFormatting.BLUE);
        line(tooltip, "stabilizing_permanent", ChatFormatting.GRAY);
    }

    static void saliva(Item.TooltipContext context, List<Component> tooltip) {
        PotionContents.addPotionTooltip(List.of(new MobEffectInstance(MobEffects.POISON, 600, 2),
                new MobEffectInstance(MobEffects.CONFUSION, 1200)), tooltip::add, 1F, context.tickRate());
        tooltip.add(CommonComponents.EMPTY);
        line(tooltip, "delayed", ChatFormatting.GRAY);
    }

    private static void line(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.translatable("tooltip.nekogenix." + key).withStyle(color));
    }

    private ModTooltips() {}
}
