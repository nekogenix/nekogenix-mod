package io.github.nekogenix.client.recipe;

import io.github.nekogenix.Content;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.centrifuge.CentrifugeMenu;
import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import io.github.nekogenix.recipe.ProcessingRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public record ProcessingDisplay(ResourceLocation id, Machine machine, List<Slot> slots, int syringes) {
    public enum Role { INPUT, OUTPUT }
    public enum Viewer { JEI, EMI, REI }
    public enum Machine {
        CENTRIFUGE("centrifuging", "block.nekogenix.centrifuge", 174, CentrifugeBlockEntity.PROCESS_TICKS),
        PNEUMATIC("pneumatic_filling", "block.nekogenix.pneumatic_stand", 94, PneumaticStandBlockEntity.PROCESS_TICKS);
        private final String path, title;
        private final int height, ticks;
        Machine(String path, String title, int height, int ticks) { this.path = path; this.title = title; this.height = height; this.ticks = ticks; }
        public ResourceLocation id() { return Content.id(path); }
        public Component title() { return Component.translatable(title); }
        public int width() { return 242; }
        public int height() { return height; }
        public int ticks() { return ticks; }
        public ItemStack icon() { return (this == CENTRIFUGE ? Content.CENTRIFUGE_ITEM : Content.PNEUMATIC_ITEM).toStack(); }
    }
    public record Slot(Role role, String name, int x, int y, List<ItemStack> items, boolean returned) {
        public Slot { items = items.stream().map(ItemStack::copy).toList(); }
    }

    public ProcessingDisplay { slots = List.copyOf(slots); }

    public static boolean active(Viewer viewer) {
        var mods = ModList.get();
        Viewer selected = mods.isLoaded("emi") ? Viewer.EMI : mods.isLoaded("roughlyenoughitems") ? Viewer.REI : Viewer.JEI;
        return viewer == selected;
    }

    public static List<ProcessingDisplay> centrifuging(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.CENTRIFUGING.get()).stream().map(holder -> {
            var recipe = holder.value();
            var slots = new ArrayList<Slot>();
            for (int i = 0; i < CentrifugeBlockEntity.BOTTLES; i++) {
                slots.add(input("sample_" + i, CentrifugeMenu.BOTTLE_POSITIONS[i][0], CentrifugeMenu.BOTTLE_POSITIONS[i][1], recipe.sample()));
            }
            slots.add(input("card", 80, 143, recipe.card()));
            slots.add(new Slot(Role.INPUT, "fuel", 24, 143, List.of(new ItemStack(Items.REDSTONE, CentrifugeBlockEntity.REDSTONE_PER_CARD)), false));
            slots.add(output("result", 212, 63, recipe.result(), false));
            slots.add(output("bottles", 212, 108, new ItemStack(Items.GLASS_BOTTLE, CentrifugeBlockEntity.BOTTLES), true));
            return new ProcessingDisplay(holder.id(), Machine.CENTRIFUGE, slots, 0);
        }).toList();
    }

    public static List<ProcessingDisplay> filling(Level level) {
        var displays = new ArrayList<ProcessingDisplay>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.PNEUMATIC_FILLING.get())) {
            var recipe = holder.value();
            for (int count = 1; count <= 2; count++) {
                var slots = new ArrayList<Slot>();
                slots.add(new Slot(Role.INPUT, "fuel", 17, 17, List.of(Items.WIND_CHARGE.getDefaultInstance()), false));
                slots.add(input("source", 79, 17, recipe.source()));
                slots.add(output("remainder", 212, 17, recipe.remainder(), true));
                for (int i = 0; i < count; i++) {
                    slots.add(new Slot(Role.INPUT, "syringe_" + i, 56 + 46 * i, 51, List.of(Content.SYRINGE.toStack()), false));
                    slots.add(output("injection_" + i, count == 1 ? 212 : 194 + 22 * i, 51, recipe.result(), false));
                }
                displays.add(new ProcessingDisplay(holder.id().withSuffix("_" + count), Machine.PNEUMATIC, slots, count));
            }
        }
        return List.copyOf(displays);
    }

    private static Slot input(String name, int x, int y, Ingredient ingredient) {
        return new Slot(Role.INPUT, name, x, y, Arrays.asList(ingredient.getItems()), false);
    }
    private static Slot output(String name, int x, int y, ItemStack item, boolean returned) {
        return new Slot(Role.OUTPUT, name, x, y, List.of(item), returned);
    }
}
