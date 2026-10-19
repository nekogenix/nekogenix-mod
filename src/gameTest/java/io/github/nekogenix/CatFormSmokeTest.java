package io.github.nekogenix;

import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import io.github.nekogenix.state.NekoState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Nekogenix.ID)
public final class CatFormSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("nekogenix.catSmokeTest");
    private static final BlockPos STAND = new BlockPos(128, -60, 128);

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount;
        var level = player.serverLevel();
        if (age == 30) {
            level.setDayTime(6000);
            level.setWeatherParameters(12000, 0, false, false);
            player.removeAllEffects();
            player.getData(Content.NEKO).deserializeNBT(level.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -59.0, 125.0, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
            if (level.getBlockEntity(STAND) instanceof PneumaticStandBlockEntity previous) previous.clearContent();
            level.setBlockAndUpdate(STAND, Blocks.AIR.defaultBlockState());
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(STAND).inflate(12)).forEach(Entity -> Entity.discard());
            level.setBlockAndUpdate(STAND, Content.PNEUMATIC_STAND.get().defaultBlockState());
            for (int i = 0; i < 5; i++) player.getInventory().setItem(9 + i, new ItemStack(new net.minecraft.world.item.Item[] {
                    Content.SYRINGE.get(), Content.USED_SYRINGE.get(), Content.INJECTION.get(), Content.STABILIZING_INJECTION.get(), Content.NEKO_STABILIZER.get()}[i]));
        }
        if (age == 340) {
            player.removeAllEffects();
            CatTransformationTests.set(player, NekoState.Phase.CAT_TRANSFORMING, 400, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
        }
        if (age == 560) {
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            check(player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO, "A real client stabilized stage four");
            CatTransformationTests.set(player, NekoState.Phase.CAT_TRANSFORMING, NekoState.DAY - 1, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.DIAMOND_PICKAXE.getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, Items.TORCH.getDefaultInstance());
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -60, 125, 0, 0);
        }
        if (age == 760) {
            var stand = (PneumaticStandBlockEntity) level.getBlockEntity(STAND);
            stand.setItem(0, Content.SYRINGE.toStack());
            stand.setItem(2, Content.SYRINGE.toStack());
            stand.setItem(3, Content.NEKO_STABILIZER.toStack());
            stand.setItem(4, Items.WIND_CHARGE.getDefaultInstance());
        }
        if (age == 820) player.openMenu((PneumaticStandBlockEntity) level.getBlockEntity(STAND));
        if (age == 1190) {
            check(player.getData(Content.NEKO).isCat() && Math.abs(player.getBbHeight() - .7F) < .001,
                    "Full cat form retains server collision dimensions");
            Nekogenix.LOGGER.info("Nekogenix cat server smoke passed: injection stabilization final cat form and synchronized dimensions");
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("Nekogenix cat smoke failed: " + message);
    }

    @EventBusSubscriber(modid = Nekogenix.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private static String pendingCapture;
        private static boolean sawCatRender;
        private static boolean sawPaw;
        private static int windSounds;

        @SubscribeEvent
        public static void render(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
            if (ENABLED && event.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat
                    && cat.getMainHandItem().is(Items.DIAMOND_PICKAXE)) sawCatRender = true;
        }

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void paw(net.neoforged.neoforge.client.event.RenderHandEvent event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (ENABLED && player != null && player.getData(Content.CAT_FORM) && event.isCanceled()) sawPaw = true;
        }

        @SubscribeEvent
        public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
            if (ENABLED && event.getSound() != null && event.getSound().getLocation().getPath().startsWith("entity.breeze.")) windSounds++;
        }

        @SubscribeEvent
        public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
            if (!ENABLED) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            int age = mc.player.tickCount;
            mc.options.setCameraType(age >= 630 && age <= 675 ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.screenEffectScale().set(0D);
            mc.options.keyAttack.setDown(age >= 60 && age < 100 || age >= 160 && age < 270 || age >= 400 && age < 510);
            if (age == 85 || age == 205 || age == 445) needle(mc);
            if (age == 85 || age == 105 || age == 205 || age == 445) capture(mc, "cat-injection-" + age + ".png");
            if (age == 300) check(mc.player.getMainHandItem().is(Content.USED_SYRINGE) && mc.player.hasEffect(Content.SHOCK), "Neko injection still completes");
            if (age == 530) check(!mc.player.hasEffect(Content.TRANSFORMING) && mc.player.getMainHandItem().is(Content.USED_SYRINGE), "Stabilization clears stage four and returns a used syringe");
            if (age == 620) {
                check(mc.player.getData(Content.CAT_FORM) && Math.abs(mc.player.getEyeHeight() - .35F) < .001,
                        "Cat form and camera height arrive on the real client");
                capture(mc, "cat-first-person.png");

            }
            if (age == 660) capture(mc, "cat-third-person.png");
            if (age == 690) {
                check(sawCatRender && sawPaw, "Third person cat renderer and first person paws both run");
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            }
            if (age == 710) capture(mc, "cat-inventory.png");
            if (age == 730) {
                mc.player.closeContainer();

                mc.player.setXRot(15);
            }
            if (age == 770 || age == 810) capture(mc, "cat-pneumatic-wind-" + age + ".png");
            if (age == 950) {
                check(mc.screen instanceof io.github.nekogenix.client.PneumaticStandScreen, "Pneumatic menu opens for a cat player");
                var menu = (io.github.nekogenix.pneumatic.PneumaticStandMenu) mc.player.containerMenu;
                check(menu.filling(0) && !menu.filling(1), "Only one syringe receives the stabilizer");
                var stack = menu.getSlot(0).getItem();
                check(net.minecraft.client.renderer.item.ItemProperties.getProperty(stack, Content.id("stabilizing_progress"))
                        .call(stack, mc.level, mc.player, 0) > 0, "Stabilizer has its own synchronized filling animation");
                capture(mc, "cat-pneumatic-menu.png");
            }
            if (age == 1200) {
                var menu = mc.player.containerMenu;
                check(menu.getSlot(0).getItem().is(Content.STABILIZING_INJECTION) && menu.getSlot(1).getItem().is(Content.SYRINGE)
                        && menu.getSlot(2).getItem().is(Items.GLASS_BOTTLE) && windSounds >= 4,
                        "One stabilizer bottle produces one syringe and returns glass with vanilla wind sounds");
                capture(mc, "cat-pneumatic-complete.png");
                Nekogenix.LOGGER.info("Nekogenix cat client smoke passed: both injections cat model paws inventory camera and pneumatic recipe");
            }
            if (age >= 1220) mc.stop();
        }

        private static void needle(net.minecraft.client.Minecraft mc) {
            var stack = mc.player.getMainHandItem();
            var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack)
                    .applyForgeHandTransform(pose, mc.player, mc.player.getMainArm(), stack, 0, 0, 0);
            model.applyTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, pose, false);
            pose.translate(-.5, -.5, -.5);
            var quad = model.getQuads(null, null, net.minecraft.util.RandomSource.create()).stream()
                    .filter(q -> q.getDirection() == net.minecraft.core.Direction.SOUTH).findFirst().orElseThrow();
            int[] vertices = quad.getVertices();
            float[] a = new float[6], b = new float[6], c = new float[6];
            for (int i = 0; i < 6; i++) {
                a[i] = Float.intBitsToFloat(vertices[i]);
                b[i] = Float.intBitsToFloat(vertices[8 + i]);
                c[i] = Float.intBitsToFloat(vertices[16 + i]);
            }
            float u = quad.getSprite().getU(1.5F / 16F), v = quad.getSprite().getV(1.5F / 16F);
            float determinant = (b[4]-a[4])*(c[5]-a[5])-(b[5]-a[5])*(c[4]-a[4]);
            float x = ((u-a[4])*(c[5]-a[5])-(v-a[5])*(c[4]-a[4]))/determinant;
            float y = ((b[4]-a[4])*(v-a[5])-(b[5]-a[5])*(u-a[4]))/determinant;
            var tip = new org.joml.Vector4f(a[0]+x*(b[0]-a[0])+y*(c[0]-a[0]),
                    a[1]+x*(b[1]-a[1])+y*(c[1]-a[1]), a[2]+x*(b[2]-a[2])+y*(c[2]-a[2]), 1);
            tip.mul(pose.last().pose());
            float vertical = -tip.z * (float)Math.tan(Math.toRadians(35));
            boolean hidden = tip.z >= -.05F || Math.abs(tip.y) > vertical || Math.abs(tip.x) > vertical * mc.getWindow().getWidth()/mc.getWindow().getHeight();
            Nekogenix.LOGGER.info("Nekogenix needle position {} hidden={}", tip, hidden);
            check(hidden, "The needle tip stays outside the view during injection");
        }

        private static void capture(net.minecraft.client.Minecraft mc, String name) { pendingCapture = name; }

        @SubscribeEvent
        public static void frame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
            if (!ENABLED || pendingCapture == null) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots").resolve(pendingCapture));
            } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
            Nekogenix.LOGGER.info("Nekogenix visual frame {} camera={} invisible={} height={} catRendered={} pawRendered={}",
                    pendingCapture, mc.options.getCameraType(), mc.player.isInvisible(), mc.player.getBbHeight(), sawCatRender, sawPaw);
            pendingCapture = null;
        }
    }
}
