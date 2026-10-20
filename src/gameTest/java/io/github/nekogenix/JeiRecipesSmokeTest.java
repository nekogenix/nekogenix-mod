package io.github.nekogenix;

import io.github.nekogenix.integration.jei.NekogenixJei;
import io.github.nekogenix.recipe.ProcessingRecipes;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class JeiRecipesSmokeTest implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return Content.id("jei_smoke_test"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    @net.neoforged.fml.common.EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static int age;
        private static boolean checked;

        @net.neoforged.bus.api.SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!Boolean.getBoolean("nekogenix.jeiSmokeTest")) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            if (mc.player == null || mc.level == null) return;
            if (!net.neoforged.fml.ModList.get().isLoaded("jei")) {
                if (++age == 60) {
                    check(mc.level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.CENTRIFUGING.get()).size() == 1
                            && mc.level.getRecipeManager().getAllRecipesFor(ProcessingRecipes.PNEUMATIC_FILLING.get()).size() == 2,
                            "Processing recipes synchronize without JEI installed");
                    Nekogenix.LOGGER.info("Nekogenix recipes without JEI smoke passed");
                    mc.stop();
                }
                return;
            }
            if (runtime == null) return;
            var manager = runtime.getRecipeManager();
            if (!checked) {
                var extraction = manager.createRecipeLookup(NekogenixJei.CENTRIFUGING).get().toList();
                var fillings = manager.createRecipeLookup(NekogenixJei.PNEUMATIC_FILLING).get().toList();
                check(extraction.size() == 1 && fillings.size() == 4, "JEI loads extraction and both batch sizes for both injections");
                var focus = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
                var layout = manager.createRecipeLayoutDrawable(manager.getRecipeCategory(NekogenixJei.CENTRIFUGING), extraction.getFirst(), focus).orElseThrow();
                var slots = layout.getRecipeSlotsView();
                check(slots.getSlotViews(RecipeIngredientRole.INPUT).size() == 8 && slots.getSlotViews(RecipeIngredientRole.OUTPUT).size() == 2,
                        "The extraction page includes six bottles, a card, fuel, and both outputs");
                check(slots.findSlotByName("fuel").orElseThrow().getItemStacks().findFirst().orElseThrow().getCount() == 64
                        && slots.findSlotByName("bottles").orElseThrow().getItemStacks().findFirst().orElseThrow().getCount() == 6,
                        "The recipe shows the complete redstone cost and returned bottles");
                for (var filling : fillings) {
                    var filled = manager.createRecipeLayoutDrawable(manager.getRecipeCategory(NekogenixJei.PNEUMATIC_FILLING), filling, focus).orElseThrow();
                    check(filled.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).size() == filling.syringes() + 2
                            && filled.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT).size() == filling.syringes() + 1,
                            "Each batch shows its actual syringe count, one wind charge, and one returned source");
                }
                mc.options.guiScale().set(3);
                mc.resizeDisplay();
                runtime.getRecipesGui().showTypes(List.of(NekogenixJei.CENTRIFUGING));
                checked = true;
            }
            age++;
            if (age == 25) capture("jei-centrifuge.png");
            if (age == 35 || age == 85) {
                var recipes = manager.createRecipeLookup(NekogenixJei.PNEUMATIC_FILLING).get()
                        .filter(recipe -> recipe.syringes() == 2 && recipe.recipe().result().is(age == 35 ? Content.INJECTION : Content.STABILIZING_INJECTION)).toList();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(NekogenixJei.PNEUMATIC_FILLING), recipes, List.of());
            }
            if (age == 60) capture("jei-pneumatic-neko.png");
            if (age == 110) capture("jei-pneumatic-crystallizing.png");
            if (age == 125) {
                Nekogenix.LOGGER.info("Nekogenix JEI recipe client smoke passed: registered recipes, ingredient lookup, batch sizes and visible pages");
                mc.stop();
            }
        }

        private static void capture(String name) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
        }

        private static void check(boolean condition, String message) {
            if (!condition) throw new IllegalStateException("JEI recipes smoke failed: " + message);
        }
    }
}
