package io.github.nekogenix;

import com.mojang.authlib.GameProfile;
import io.github.nekogenix.gameplay.FelineSounds;
import io.github.nekogenix.gameplay.TransformationHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Opt-in client and server breeding check, excluded from the released jar. */
@EventBusSubscriber(modid = Nekogenix.ID)
public final class BreedingSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.breedingSmokeTest");
    private static ServerPlayer guest;

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player) || player == guest
                || player.getGameProfile().getName().equals("NekoGuest")) return;
        if (guest != null) guest.doTick();
        if (player.tickCount == 520) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "BreedingGuest"), false);
            guest = new ServerPlayer(player.server, player.serverLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            NetworkRegistry.configureMockConnection(connection);
            player.server.getPlayerList().placeNewPlayer(connection, guest, cookie);
            guest.teleportTo(player.getX() + 1, player.getY(), player.getZ());
            makeNeko(player);
            makeNeko(guest);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 4));
        }
        if (player.tickCount == 550) {
            guest.setItemInHand(InteractionHand.MAIN_HAND, Items.COD.getDefaultInstance());
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(guest, InteractionHand.MAIN_HAND, player));
        }
        if (player.tickCount == 660) {
            guest.getData(Content.BREEDING).deserializeNBT(guest.registryAccess(), new CompoundTag());
            player.getData(Content.BREEDING).clearLove();
            Cat cat = EntityType.CAT.create(player.serverLevel());
            cat.tame(player);
            cat.setOrderedToSit(false);
            cat.moveTo(player.getX() + 2.5, player.getY(), player.getZ());
            cat.setCustomName(net.minecraft.network.chat.Component.literal("BreedingCat"));
            player.serverLevel().addFreshEntity(cat);
        }
        if (player.tickCount == 820) {
            player.server.getPlayerList().remove(guest);
            guest = null;
        }
    }

    private static void makeNeko(ServerPlayer player) {
        player.removeAllEffects();
        var tag = new CompoundTag();
        tag.putInt("phase", 3);
        tag.putInt("abilities", 7);
        tag.putInt("lives", 9);
        player.getData(Content.NEKO).deserializeNBT(player.registryAccess(), tag);
        player.getFoodData().setFoodLevel(20);
        player.getData(Content.BREEDING).deserializeNBT(player.registryAccess(), new CompoundTag());
        TransformationHandler.refreshEffects(player);
        FelineSounds.sync(player);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            if (age == 515) mc.setScreen(null);
            if (age == 540 || age == 680) {
                var other = mc.level.players().stream().filter(p -> p.getGameProfile().getName().equals("BreedingGuest"))
                        .findFirst().orElseThrow();
                mc.gameMode.interact(mc.player, other, InteractionHand.MAIN_HAND);
            }
            if (age == 685) {
                var cat = mc.level.getEntitiesOfClass(Cat.class, mc.player.getBoundingBox().inflate(5),
                        entity -> entity.hasCustomName() && entity.getCustomName().getString().equals("BreedingCat"))
                        .getFirst();
                mc.gameMode.interact(mc.player, cat, InteractionHand.MAIN_HAND);
            }
            if (age == 650 || age == 800) {
                int expected = age == 650 ? 1 : 2;
                int kittens = mc.level.getEntitiesOfClass(Cat.class, mc.player.getBoundingBox().inflate(10), Cat::isBaby).size();
                if (kittens != expected) throw new IllegalStateException("Breeding client expected " + expected + " kittens but saw " + kittens);
                Nekogenix.LOGGER.info("Nekogenix breeding client verified {} kittens after feeding through interaction packets", kittens);
            }
            if (age == 805) net.minecraft.client.Screenshot.grab(mc.gameDirectory, "breeding-prealpha6.png",
                    mc.getMainRenderTarget(), message -> {});
            if (age >= 840) {
                Nekogenix.LOGGER.info("Nekogenix breeding smoke passed: both pairings produced tracked kittens after normal client feeding");
                mc.stop();
            }
        }
    }
}
