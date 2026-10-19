package io.github.nekogenix.gameplay;

import io.github.nekogenix.Nekogenix;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class SyringeHands {
    public static boolean blocked(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND && InjectionHandler.isSyringe(player.getMainHandItem());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void item(PlayerInteractEvent.RightClickItem event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void block(PlayerInteractEvent.RightClickBlock event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityPart(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blocked(event.getEntity(), event.getHand())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && blocked(player, event.getHand())) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        if (player.isUsingItem() && blocked(player, player.getUsedItemHand())) player.stopUsingItem();
    }

    private SyringeHands() {}
}
