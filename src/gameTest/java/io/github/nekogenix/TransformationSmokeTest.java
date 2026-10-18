package io.github.nekogenix;

import com.mojang.authlib.GameProfile;
import io.github.nekogenix.gameplay.FelineSounds;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Opt-in multiplayer transformation check, excluded from the released jar. */
@EventBusSubscriber(modid = Nekogenix.ID)
public final class TransformationSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.transformationSmokeTest");
    private static ServerPlayer guest;

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player == guest) return;
        if (player.tickCount == 355) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "NekoGuest"), false);
            guest = new ServerPlayer(player.server, player.serverLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            player.server.getPlayerList().placeNewPlayer(connection, guest, cookie);
            guest.teleportTo(player.getX() + 1, player.getY(), player.getZ());
            guest.getData(Content.NEKO).begin();
            guest.getFoodData().setFoodLevel(0);
            guest.getFoodData().setSaturation(0);
            FelineSounds.sync(guest);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 2));
        }
        if (player.tickCount == 410 && guest != null) {
            if (guest.getFoodData().getFoodLevel() == 2 && player.getMainHandItem().is(Items.COD)
                    && player.getMainHandItem().getCount() == 1) {
                Nekogenix.LOGGER.info("Nekogenix multiplayer feeding passed: remote feline player fed through normal interaction packet");
            } else throw new IllegalStateException("Nekogenix multiplayer feeding failed");
            player.server.getPlayerList().remove(guest);
            guest = null;
        }
        if (player.tickCount == 430) {
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.MILK_BUCKET.getDefaultInstance());
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static int completed = -1;
        private static int ocean;

        @SubscribeEvent
        public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (!ENABLED || event.getSound() == null || !event.getSound().getLocation()
                    .equals(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE.getLocation())) return;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            check(player != null && completed >= 0 && Math.abs(player.tickCount - completed - 200) <= 4,
                    "Ocean cue arrives ten seconds after completion");
            ocean++;
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            mc.options.keyUse.setDown(age >= 435 && age < 475);
            if (completed < 0 && mc.player.hasEffect(Content.SHOCK)) completed = age;
            if (age == 180) {
                var nausea = mc.player.getEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
                var extension = net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions.of(
                        net.minecraft.world.effect.MobEffects.CONFUSION.value());
                check(nausea != null && !extension.isVisibleInInventory(nausea) && !extension.isVisibleInGui(nausea),
                        "Sickness nausea stays active with both indicators hidden");
                var ordinary = new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 200);
                check(extension.isVisibleInInventory(ordinary) && extension.isVisibleInGui(ordinary), "Ordinary nausea remains visible");
            }
            if (age == 340) {
                check(ocean == 1, "Ocean cue plays exactly once");
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
                    @Override
                    public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partial) {
                        super.render(graphics, x, y, partial);
                        graphics.renderTooltip(font, Content.INJECTION.toStack(), 24, 30);
                        graphics.renderTooltip(font, Content.SALIVA_BOTTLE.toStack(), 24, 190);
                    }
                });
            }
            if (age == 350) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "tooltips-prealpha4.png", mc.getMainRenderTarget(), message -> {});
            if (age == 360) mc.setScreen(null);
            if (age == 380) {
                var guest = mc.level.players().stream().filter(player -> player.getGameProfile().getName().equals("NekoGuest"))
                        .findFirst().orElseThrow();
                mc.gameMode.interact(mc.player, guest, InteractionHand.MAIN_HAND);
            }
            if (age == 420) check(mc.player.getMainHandItem().is(Items.COD) && mc.player.getMainHandItem().getCount() == 1,
                    "Feeder inventory update arrives from the server");
            if (age == 435) mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            if (age == 480) {
                check(mc.player.hasEffect(net.minecraft.world.effect.MobEffects.POISON) && mc.player.hasEffect(Content.SHOCK),
                        "Drinking milk poisons without curing sickness: " + mc.player.getActiveEffects() + "; held=" + mc.player.getMainHandItem());
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                Nekogenix.LOGGER.info("Nekogenix transformation smoke passed: sickness nausea hidden, ordinary nausea visible, timed ocean cue received once, feeding synchronized and milk poisonous");
            }
            if (age == 490) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "sickness-prealpha4.png", mc.getMainRenderTarget(), message -> {});
            if (age >= 510) mc.stop();
        }

        private static void check(boolean passed, String message) {
            if (!passed) throw new IllegalStateException("Nekogenix transformation smoke failed: " + message);
        }
    }
}
