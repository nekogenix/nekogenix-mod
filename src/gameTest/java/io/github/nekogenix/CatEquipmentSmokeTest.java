package io.github.nekogenix;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class CatEquipmentSmokeTest extends Screen {
    private final boolean side;
    static int customModels;

    CatEquipmentSmokeTest(boolean side) { super(Component.literal("Feline equipment")); this.side = side; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xff75828b);
        var player = minecraft.player;
        var slots = new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        var saved = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) saved[i] = player.getItemBySlot(slots[i]);
        Item[][] sets = {
                {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
                {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
                {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
                {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS},
                {Items.TURTLE_HELMET, Items.ELYTRA}, {Items.CARVED_PUMPKIN}, {Items.PLAYER_HEAD}, {Items.DRAGON_HEAD},
                {Items.CREEPER_HEAD}, {Items.SKELETON_SKULL}};
        int cellWidth = width / 4, cellHeight = height / 3;
        try {
            for (int i = 0; i < sets.length; i++) {
                for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
                for (int j = 0; j < sets[i].length; j++) {
                    var stack = sets[i][j].getDefaultInstance();
                    if (i == 0) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(0x45b5d5, false));
                    if (i == 4) {
                        var registries = minecraft.level.registryAccess();
                        stack.set(DataComponents.TRIM, new ArmorTrim(
                                registries.registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.REDSTONE),
                                registries.registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(TrimPatterns.SENTRY)));
                        stack.enchant(registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION), 1);
                    }
                    player.setItemSlot(slots[j], stack);
                }
                int x = i % 4 * cellWidth, y = i / 4 * cellHeight;
                graphics.drawString(font, sets[i][0].getDescription(), x + 3, y + 3, 0xffffff, false);
                InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x + 2, y + 13,
                        x + cellWidth - 2, y + cellHeight, (int)(cellHeight * .75), 0, side ? 3.3F : .8F, .2F, player);
            }
        } finally {
            for (int i = 0; i < slots.length; i++) player.setItemSlot(slots[i], saved[i]);
        }
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Extensions {
        @SubscribeEvent
        public static void register(RegisterClientExtensionsEvent event) {
            if (!Boolean.getBoolean("nekogenix.catEquipmentPreview")) return;
            event.registerItem(new IClientItemExtensions() {
                private HumanoidModel<?> custom;
                @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                        EquipmentSlot slot, HumanoidModel<?> original) {
                    if (custom == null) {
                        var mesh = HumanoidModel.createMesh(new CubeDeformation(1), 0);
                        mesh.getRoot().getChild("head").addOrReplaceChild("crest",
                                CubeListBuilder.create().texOffs(0, 0).addBox(-1, -11, -3, 2, 3, 6), PartPose.ZERO);
                        custom = new HumanoidModel<>(LayerDefinition.create(mesh, 64, 32).bakeRoot());
                    }
                    if (entity instanceof net.minecraft.world.entity.player.Player player && io.github.nekogenix.gameplay.CatForm.active(player)) {
                        if (original.head.xScale >= 1)
                            throw new IllegalStateException("Equipment hooks must receive the actual player and fitted skeleton");
                        customModels++;
                    }
                    return custom;
                }
            }, Items.GOLDEN_HELMET);
        }
    }
}
