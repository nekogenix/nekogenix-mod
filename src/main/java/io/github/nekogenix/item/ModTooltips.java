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
        tooltip.add(CommonComponents.EMPTY);
        line(tooltip, "after_stages", ChatFormatting.DARK_PURPLE);
        tooltip.add(Component.translatable("effect.nekogenix.feline_agility").withStyle(ChatFormatting.BLUE));
        line(tooltip, "double_jump", ChatFormatting.BLUE);
        tooltip.add(Component.translatable("effect.nekogenix.feline_vision").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("effect.nekogenix.soft_paw_pads").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("ability.nekogenix.feline_scent").withStyle(ChatFormatting.BLUE));
        line(tooltip, "cat_body", ChatFormatting.BLUE);
        line(tooltip, "lives", ChatFormatting.BLUE);
        line(tooltip, "hunger", ChatFormatting.BLUE);
        line(tooltip, "diet", ChatFormatting.BLUE);
        line(tooltip, "cats", ChatFormatting.BLUE);
        line(tooltip, "creepers", ChatFormatting.BLUE);
        line(tooltip, "phantoms", ChatFormatting.BLUE);
        line(tooltip, "wetness", ChatFormatting.RED);
        line(tooltip, "food_intolerance", ChatFormatting.RED);
    }

    static void stabilizer(Item.TooltipContext context, List<Component> tooltip) {
        PotionContents.addPotionTooltip(List.of(new MobEffectInstance(Content.CRYSTALLIZATION,
                io.github.nekogenix.gameplay.CrystallizationEffect.DURATION)), tooltip::add, 1F, context.tickRate());
        line(tooltip, "stabilizing_injection", ChatFormatting.BLUE);
    }

    static void sweetAmethyst(Item.TooltipContext context, List<Component> tooltip) {
        sweetAmethyst(context, tooltip, 1F);
    }

    static void sweetAmethyst(Item.TooltipContext context, List<Component> tooltip, float durationScale) {
        PotionContents.addPotionTooltip(List.of(new MobEffectInstance(Content.SWEETNESS,
                io.github.nekogenix.gameplay.SweetnessEffect.DURATION)), tooltip::add, durationScale, context.tickRate());
        line(tooltip, "sweetness", ChatFormatting.BLUE);
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
