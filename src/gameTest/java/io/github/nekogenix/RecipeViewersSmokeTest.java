package io.github.nekogenix;

import io.github.nekogenix.client.recipe.ProcessingDisplay;
import io.github.nekogenix.integration.emi.ProcessingEmiRecipe;
import io.github.nekogenix.integration.rei.ProcessingReiDisplay;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID, value = Dist.CLIENT)
public final class RecipeViewersSmokeTest {
    private static int age, waiting;
    private static boolean checked;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("nekogenix.viewersSmokeTest")) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        mc.getToasts().clear();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        if (mc.player == null || mc.level == null) return;
        if (!checked) {
            if (++waiting > 1200) throw new IllegalStateException("Recipe viewer did not finish registration");
            boolean ready = ModList.get().isLoaded("emi") ? Emi.check() : Rei.check();
            if (!ready) return;
            checked = true;
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            show(ProcessingDisplay.Machine.CENTRIFUGE, false);
        }
        age++;
        if (age == 25) capture("centrifuge");
        if (age == 35) show(ProcessingDisplay.Machine.PNEUMATIC, false);
        if (age == 60) capture("pneumatic-neko");
        if (age == 85) show(ProcessingDisplay.Machine.PNEUMATIC, true);
        if (age == 110) capture("pneumatic-crystallizing");
        if (age == 125) {
            Nekogenix.LOGGER.info("Nekogenix standalone recipe viewer smoke passed: native registration, costs, batches, lookup and rendering");
            mc.stop();
        }
    }
    private static void show(ProcessingDisplay.Machine machine, boolean crystal) {
        if (ModList.get().isLoaded("emi")) Emi.show(machine, crystal);
        else Rei.show(machine, crystal);
    }
    private static void capture(String page) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, (ModList.get().isLoaded("emi") ? "emi-" : "rei-") + page + ".png", mc.getMainRenderTarget(), message -> {});
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException("Recipe viewer smoke failed: " + message);
    }
    private static void common(ProcessingDisplay display) {
        var machine = display.machine();
        for (var slot : display.slots()) check(slot.x() >= 0 && slot.y() >= 0
                && slot.x() + 16 <= machine.width() && slot.y() + 16 <= machine.height(), "Slot stays within shared panel");
    }
    private static final class Emi {
        private static List<ProcessingEmiRecipe> recipes;
        private static boolean check() {
            var manager = dev.emi.emi.api.EmiApi.getRecipeManager();
            if (manager == null) return false;
            recipes = manager.getRecipes().stream().filter(ProcessingEmiRecipe.class::isInstance).map(ProcessingEmiRecipe.class::cast).toList();
            if (recipes.isEmpty()) return false;
            RecipeViewersSmokeTest.check(recipes.size() == 5, "EMI has one extraction and four filling pages");
            for (var recipe : recipes) {
                common(recipe.display());
                if (recipe.display().machine() == ProcessingDisplay.Machine.CENTRIFUGE) {
                    RecipeViewersSmokeTest.check(recipe.getInputs().size() == 8 && recipe.getOutputs().size() == 2, "EMI extraction roles");
                    RecipeViewersSmokeTest.check(recipe.getInputs().getLast().getAmount() == 64 && recipe.getOutputs().getLast().getAmount() == 6, "EMI fuel and bottle counts");
                } else RecipeViewersSmokeTest.check(recipe.getInputs().size() == recipe.display().syringes() + 2
                        && recipe.getOutputs().size() == recipe.display().syringes() + 1, "EMI batch counts");
            }
            RecipeViewersSmokeTest.check(manager.getRecipesByOutput(dev.emi.emi.api.stack.EmiStack.of(Content.CAT_NUCLEOCARD)).stream().anyMatch(ProcessingEmiRecipe.class::isInstance), "EMI output lookup");
            return true;
        }
        private static void show(ProcessingDisplay.Machine machine, boolean crystal) {
            var recipe = recipes.stream().filter(r -> r.display().machine() == machine && (machine == ProcessingDisplay.Machine.CENTRIFUGE
                    || r.display().syringes() == 2 && r.getOutputs().stream().anyMatch(item -> item.getItemStack().is(crystal ? Content.STABILIZING_INJECTION : Content.INJECTION)))).findFirst().orElseThrow();
            dev.emi.emi.api.EmiApi.displayRecipe(recipe);
        }
    }
    private static final class Rei {
        private static boolean check() {
            if (me.shedaniel.rei.api.common.plugins.PluginManager.areAnyReloading()) return false;
            var registry = me.shedaniel.rei.api.client.registry.display.DisplayRegistry.getInstance();
            var recipes = registry.getAll().values().stream().flatMap(List::stream).filter(ProcessingReiDisplay.class::isInstance).map(ProcessingReiDisplay.class::cast).toList();
            if (recipes.isEmpty()) return false;
            RecipeViewersSmokeTest.check(recipes.size() == 5, "REI has one extraction and four filling pages");
            for (var recipe : recipes) {
                common(recipe.display());
                if (recipe.display().machine() == ProcessingDisplay.Machine.CENTRIFUGE) {
                    RecipeViewersSmokeTest.check(recipe.getInputEntries().size() == 8 && recipe.getOutputEntries().size() == 2, "REI extraction roles");
                    RecipeViewersSmokeTest.check(recipe.getInputEntries().getLast().getFirst().<net.minecraft.world.item.ItemStack>castValue().getCount() == 64
                            && recipe.getOutputEntries().getLast().getFirst().<net.minecraft.world.item.ItemStack>castValue().getCount() == 6, "REI fuel and bottle counts");
                } else RecipeViewersSmokeTest.check(recipe.getInputEntries().size() == recipe.display().syringes() + 2
                        && recipe.getOutputEntries().size() == recipe.display().syringes() + 1, "REI batch counts");
            }
            return true;
        }
        private static void show(ProcessingDisplay.Machine machine, boolean crystal) {
            var builder = me.shedaniel.rei.api.client.view.ViewSearchBuilder.builder().addCategory(me.shedaniel.rei.api.common.category.CategoryIdentifier.of(machine.id()));
            if (machine == ProcessingDisplay.Machine.PNEUMATIC) builder.addRecipesFor(me.shedaniel.rei.api.common.util.EntryStacks.of(crystal ? Content.STABILIZING_INJECTION : Content.INJECTION));
            RecipeViewersSmokeTest.check(builder.open(), "REI opens native category");
        }
    }
}
