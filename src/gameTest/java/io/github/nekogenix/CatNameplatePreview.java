package io.github.nekogenix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class CatNameplatePreview extends Screen {
    private static CatNameplatePreview active;
    private final Actor[] actors = new Actor[3];
    private final boolean[] rendered = new boolean[3];

    CatNameplatePreview() {
        super(Component.literal("Cat player nameplates"));
        active = this;
        var mc = Minecraft.getInstance();
        for (int i = 0; i < actors.length; i++) {
            var actor = new Actor(i);
            actor.setData(Content.CAT_FORM, true);
            actor.setPos(mc.player.position());
            actor.setOnGround(true);
            actor.setShiftKeyDown(i == 1);
            if (i == 1) actor.setPose(Pose.CROUCHING);
            actor.refreshDimensions();
            actors[i] = actor;
        }
    }

    void tickActors(int age) {
        for (var actor : actors) actor.tickCount = age;
        // Change the scale after the proxy has already rendered at its original size.
        if (age == 225) {
            actors[2].getAttribute(Attributes.SCALE).setBaseValue(2);
            actors[2].refreshDimensions();
        }
    }

    void verify() {
        for (int i = 0; i < actors.length; i++) check(rendered[i], "Every player cat nameplate reached the renderer");
        active = null;
        Nekogenix.LOGGER.info("Nekogenix nameplate smoke passed: cat height scale changes and sneaking visibility");
    }

    @SubscribeEvent public static void nameplate(RenderNameTagEvent event) {
        if (active == null || !(event.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat)) return;
        for (int i = 0; i < active.actors.length; i++) {
            var actor = active.actors[i];
            if (!actor.getUUID().equals(cat.getUUID())) continue;
            var anchor = cat.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, 0);
            var expected = EntityType.CAT.getDimensions().scale(actor.getScale()).attachments().getNullable(EntityAttachment.NAME_TAG, 0, 0);
            check(anchor != null && expected != null && Math.abs(anchor.y - expected.y) < .001, "The nickname follows cat height and scale");
            check(cat.isDiscrete() == actor.isDiscrete(), "Sneaking uses the player's nameplate visibility");
            check(event.getContent().getString().equals(actor.getDisplayName().getString()), "The player nickname is preserved");
            active.rendered[i] = true;
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xff293238);
        graphics.drawCenteredString(font, title, width / 2, 22, 0xffffff);
        String[] labels = {"Standing", "Sneaking", "Scale changed to 2"};
        int cell = width / actors.length;
        for (int i = 0; i < actors.length; i++) {
            graphics.drawCenteredString(font, labels[i], cell * i + cell / 2, height - 40, 0xbac6ca);
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, cell * i + 3, 50, cell * (i + 1) - 3,
                    height - 60, 90, 0, .7F, .1F, actors[i]);
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException("Cat nameplate smoke failed: " + message);
    }

    private static final class Actor extends RemotePlayer {
        Actor(int index) {
            super(Minecraft.getInstance().level, new com.mojang.authlib.GameProfile(
                    java.util.UUID.nameUUIDFromBytes(("cat-nameplate-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)), "CatPlayer" + (index + 1)));
        }
        @Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(Pose pose) { return EntityType.CAT.getDimensions(); }
        @Override public net.minecraft.client.resources.PlayerSkin getSkin() {
            return new net.minecraft.client.resources.PlayerSkin(net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                    "textures/entity/player/wide/steve.png"), null, null, null, net.minecraft.client.resources.PlayerSkin.Model.WIDE, false);
        }
    }
}
