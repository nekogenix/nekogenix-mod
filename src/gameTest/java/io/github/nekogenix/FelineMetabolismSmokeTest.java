package io.github.nekogenix;

import io.github.nekogenix.gameplay.TransformationHandler;
import io.github.nekogenix.state.NekoState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class FelineMetabolismSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.metabolismSmokeTest");

    @SubscribeEvent public static void server(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        if (age >= 40 && age <= 70 && age % 10 == 0) {
            var phase = switch (age) {
                case 40 -> NekoState.Phase.HUMAN;
                case 50 -> NekoState.Phase.CAT_TRANSFORMING;
                case 60 -> NekoState.Phase.NEKO;
                default -> NekoState.Phase.CAT;
            };
            var saved = new CompoundTag(); saved.putInt("phase", phase.ordinal());
            saved.putInt("abilities", phase == NekoState.Phase.HUMAN ? 0 : 7); saved.putInt("lives", 9);
            player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), saved);
            TransformationHandler.refreshEffects(player);
            var food = player.getFoodData(); food.setFoodLevel(10); food.setSaturation(0); food.setExhaustion(0);
            player.causeFoodExhaustion(2);
            float expected = age == 40 ? 2 : 1;
            if (Math.abs(food.getExhaustionLevel() - expected) > .0001F) throw new IllegalStateException("Incorrect exhaustion for " + phase);
            food.setExhaustion(0); food.addExhaustion(6);
            if (Math.abs(food.getExhaustionLevel() - expected * 3) > .0001F) throw new IllegalStateException("Direct food cost bypassed reduction for " + phase);
            Nekogenix.LOGGER.info("Nekogenix metabolism runtime checked {}: player cost={} direct cost={}", phase, expected, food.getExhaustionLevel());
        }
        if (age == 100) Nekogenix.LOGGER.info("Nekogenix metabolism server smoke passed: human neko pending cat transformation and cat exhaustion");
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.player.tickCount != 140) return;
            if (!mc.player.getData(Content.CAT_FORM)) throw new IllegalStateException("Cat form did not reach the client");
            Nekogenix.LOGGER.info("Nekogenix metabolism client smoke passed: client joined world with cat form and shared mixins");
            mc.stop();
        }
    }
}
