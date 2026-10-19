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
    private static final boolean PREVIEW = Boolean.getBoolean("nekogenix.catPreview");
    private static final boolean EQUIPMENT = Boolean.getBoolean("nekogenix.catEquipmentPreview");
    private static final boolean CONTROLS = Boolean.getBoolean("nekogenix.controlSmokeTest");
    private static final BlockPos STAND = new BlockPos(128, -60, 128);

    @SubscribeEvent
    public static void server(PlayerTickEvent.Post event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer player)) return;
        int age = player.tickCount + (PREVIEW ? 530 : 0);
        var level = player.serverLevel();
        if (age == 30 || PREVIEW && player.tickCount == 20) {
            level.setDayTime(6000);
            level.setWeatherParameters(12000, 0, false, false);
            player.removeAllEffects();
            player.getData(Content.NEKO).deserializeNBT(level.registryAccess(), new net.minecraft.nbt.CompoundTag());
            io.github.nekogenix.gameplay.TransformationHandler.refreshEffects(player);
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().flying = false;
            player.getFoodData().setFoodLevel(17);
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -60, 125.0, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.INJECTION.toStack());
            if (level.getBlockEntity(STAND) instanceof PneumaticStandBlockEntity previous) previous.clearContent();
            level.setBlockAndUpdate(STAND, Blocks.AIR.defaultBlockState());
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(STAND).inflate(12)).forEach(Entity -> Entity.discard());
            level.setBlockAndUpdate(STAND, Content.PNEUMATIC_STAND.get().defaultBlockState());
            for (int i = 0; i < 5; i++) player.getInventory().setItem(9 + i, new ItemStack(new net.minecraft.world.item.Item[] {
                    Content.SYRINGE.get(), Content.USED_SYRINGE.get(), Content.INJECTION.get(), Content.STABILIZING_INJECTION.get(), Content.NEKO_STABILIZER.get()}[i]));
        }
        if (PREVIEW && player.tickCount == 20) {
            CatTransformationTests.set(player, NekoState.Phase.NEKO, 0, 8);
            player.setHealth(17);
        }
        if (PREVIEW && age == 830) player.setItemInHand(InteractionHand.MAIN_HAND, Items.FILLED_MAP.getDefaultInstance());
        if (PREVIEW && age == 870) Nekogenix.LOGGER.info("Nekogenix cat server smoke passed: feline presentation preview");
        if (age == 340) {
            player.removeAllEffects();
            CatTransformationTests.set(player, NekoState.Phase.CAT_TRANSFORMING, 400, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, Content.STABILIZING_INJECTION.toStack());
        }
        if (age == 560) {
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            check(player.getHealth() == 17F, "Three needle contacts hurt once each without extra completion damage");
            check(player.getData(Content.NEKO).phase() == NekoState.Phase.NEKO, "A real client stabilized Cat Transformation");
            CatTransformationTests.set(player, NekoState.Phase.CAT_TRANSFORMING, NekoState.DAY - 1, 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -60, 125, 0, 0);
        }
        if (age == 650) {
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.DIAMOND_PICKAXE.getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, Items.TORCH.getDefaultInstance());
        }
        if (age == 675) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            if (CONTROLS) {
                for (int z = 120; z <= 155; z++) {
                    level.setBlockAndUpdate(new BlockPos(132, -61, z), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(132, -60, z), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(132, -59, z), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(131, -60, z), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(133, -60, z), Blocks.STONE.defaultBlockState());
                }
                player.teleportTo(level, 132.5, -60, 122, 0, 0);
            }
        }
        if (CONTROLS && age == 705) {
            check(player.isSprinting() && player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .hasModifier(Content.id("feline_sprint")) && player.getZ() > 123,
                    "Sprint starts inside the one block tunnel and receives feline acceleration on the server");
            Nekogenix.LOGGER.info("Nekogenix tunnel server smoke passed: sprint and feline acceleration start under a one block ceiling");
        }
        if (age == 750) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -57, 125, 0, 15);
        }
        if (age == 790) {
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -60, 125, 0, 15);
        }
        if (age == 1230) {
            player.setItemInHand(InteractionHand.MAIN_HAND, Items.FILLED_MAP.getDefaultInstance());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        if (!PREVIEW && age == 760) {
            var stand = (PneumaticStandBlockEntity) level.getBlockEntity(STAND);
            stand.setItem(0, Content.SYRINGE.toStack());
            stand.setItem(2, Content.SYRINGE.toStack());
            stand.setItem(3, Content.NEKO_STABILIZER.toStack());
            stand.setItem(4, Items.WIND_CHARGE.getDefaultInstance());
        }
        if (!PREVIEW && age == 820) player.openMenu((PneumaticStandBlockEntity) level.getBlockEntity(STAND));
        if (age == 1190) {
            check(player.getData(Content.NEKO).isCat() && Math.abs(player.getBbHeight() - .7F) < .001,
                    "Full cat form retains server collision dimensions");
            Nekogenix.LOGGER.info("Nekogenix cat server smoke passed: injection stabilization final cat form and synchronized dimensions");
        }
        if (EQUIPMENT && age == 1290) {
            player.teleportTo(level, 140.5, 4, 125, 0, 0);
            player.setDeltaMovement(.12, -.05, .08);
            player.hurtMarked = true;
        }
        if (EQUIPMENT && age == 1360) {
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 128.5, -54, 125, 0, 0);
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
        private static int emptyMainArms, emptyOffArms;

        @SubscribeEvent
        public static void render(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) {
            if (ENABLED && event.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat
                    && cat.getMainHandItem().is(Items.DIAMOND_PICKAXE)) sawCatRender = true;
        }

        @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void paw(net.neoforged.neoforge.client.event.RenderArmEvent event) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (!ENABLED || player == null || !player.getData(Content.CAT_FORM) || !event.isCanceled()) return;
            sawPaw = true;
            if (player.tickCount + (PREVIEW ? 530 : 0) >= 600 && player.tickCount + (PREVIEW ? 530 : 0) < 625) {
                if (event.getArm() == player.getMainArm()) emptyMainArms++;
                else emptyOffArms++;
            }
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
            if (PREVIEW && mc.player.tickCount < 25) return;
            int age = mc.player.tickCount + (PREVIEW ? 530 : 0);
            mc.options.setCameraType((age >= 655 && age < 665 || !CONTROLS && age >= 680 && age <= 730) ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            mc.options.fov().set(70);
            mc.options.screenEffectScale().set(0D);
            mc.options.keyAttack.setDown(age >= 60 && age < 100 || age >= 160 && age < 270 || age >= 400 && age < 510);
            if (age == 85 || age == 205 || age == 445) needle(mc);
            if (age == 68 || age == 75 || age == 85 || age == 105 || age == 205 || age == 445) capture(mc, "cat-injection-" + age + ".png");
            if (age == 300) check(mc.player.getMainHandItem().is(Content.USED_SYRINGE) && mc.player.hasEffect(Content.SHOCK), "Neko injection still completes");
            if (age == 530) check(!mc.player.hasEffect(Content.CAT_TRANSFORMING) && mc.player.getMainHandItem().is(Content.USED_SYRINGE), "Stabilization clears Cat Transformation and returns a used syringe");
            if (age == 620) {
                check(mc.player.getData(Content.CAT_FORM) && Math.abs(mc.player.getEyeHeight() - io.github.nekogenix.gameplay.CatForm.STANDING_EYE_HEIGHT) < .001,
                        "Cat form and camera height arrive on the real client");
                capture(mc, "cat-first-person.png");

            }
            if (age == 625) {
                check(emptyMainArms > 0 && emptyOffArms == 0, "The unused empty offhand stays hidden");
                mc.options.keyShift.setDown(true);
            }
            if (age == 645) {
                check(Math.abs(mc.player.getEyeHeight() - io.github.nekogenix.gameplay.CatForm.CROUCHING_EYE_HEIGHT) < .001,
                        "Crouching lowers the client eye height");
                double eye = mc.gameRenderer.getMainCamera().getPosition().y - mc.player.getY();
                check(Math.abs(eye - io.github.nekogenix.gameplay.CatForm.CROUCHING_EYE_HEIGHT) < .005,
                        "The actual camera smoothly reaches the crouched eye height");
                capture(mc, "cat-crouching.png");
                mc.options.keyShift.setDown(false);
            }
            if (age == 660) capture(mc, "cat-third-person.png");
            if (age == 670) capture(mc, "cat-held-first-person.png");
            if (age == 679) {
                float yaw = CONTROLS ? 0 : 90;
                mc.player.setYRot(yaw); mc.player.yRotO = yaw;
                mc.player.yBodyRot = mc.player.yBodyRotO = yaw;
            }
            if (CONTROLS && age == 700) check(mc.player.isSprinting() && mc.player.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > .2,
                    "The client starts sprinting with its ordinary sprint key under the low ceiling");
            mc.options.keyUp.setDown(age >= 680 && age < 710);
            mc.options.keySprint.setDown(age >= 680 && age < 710);
            mc.options.keyJump.setDown(age == 716 || age == 735);
            if (age == 690 || age == 694) capture(mc, "cat-running-" + age + ".png");
            if (age == 721 || age == 727 || age == 741) capture(mc, "cat-airborne-" + age + ".png");
            if (age == 745) check(sawCatRender && sawPaw, "Cat body paws and both viewpoints render successfully");
            if (PREVIEW && age >= 800) {
                if (age == 800) mc.setScreen(new GripPreview(false));
                if (age == 820) capture(mc, "cat-item-grips-front.png");
                if (age == 840) mc.player.closeContainer();
                if (age == 850) capture(mc, "cat-map-paws.png");
                if (age == 860) mc.setScreen(new GripPreview(true));
                if (age == 870) {
                    capture(mc, "cat-item-grips-side.png");
                    Nekogenix.LOGGER.info("Nekogenix cat client smoke passed: native grips running crouching airborne paws and hidden empty offhand");
                }
                if (age >= 880) mc.stop();
                return;
            }
            if (age == 770 || age == 810) capture(mc, "cat-pneumatic-wind-" + age + ".png");
            if (age == 950) {
                check(mc.screen instanceof io.github.nekogenix.client.PneumaticStandScreen, "Pneumatic menu opens for a cat player");
                var menu = (io.github.nekogenix.pneumatic.PneumaticStandMenu) mc.player.containerMenu;
                check(menu.filling(0) && menu.filling(1), "Both syringes receive the stabilizer together");
                var stack = menu.getSlot(0).getItem();
                check(net.minecraft.client.renderer.item.ItemProperties.getProperty(stack, Content.id("stabilizing_progress"))
                        .call(stack, mc.level, mc.player, 0) > 0, "Stabilizer has its own synchronized filling animation");
                capture(mc, "cat-pneumatic-menu.png");
            }
            if (age == 1200) {
                var menu = mc.player.containerMenu;
                check(menu.getSlot(0).getItem().is(Content.STABILIZING_INJECTION) && menu.getSlot(1).getItem().is(Content.STABILIZING_INJECTION)
                        && menu.getSlot(2).getItem().is(Items.GLASS_BOTTLE) && windSounds >= 4,
                        "One stabilizer bottle fills both syringes and returns glass with vanilla wind sounds");
                capture(mc, "cat-pneumatic-complete.png");
                Nekogenix.LOGGER.info("Nekogenix cat client smoke passed: both injections cat model paws inventory camera and pneumatic recipe");
            }
            if (age == 1210) mc.setScreen(EQUIPMENT ? new CatEquipmentSmokeTest(false) : new GripPreview(false));
            if (age == 1220) capture(mc, "cat-item-grips-front.png");
            if (age == 1240) mc.player.closeContainer();
            if (age == 1250) capture(mc, "cat-map-paws.png");
            if (age == 1260) mc.setScreen(EQUIPMENT ? new CatEquipmentSmokeTest(true) : new GripPreview(true));
            if (age == 1270) capture(mc, "cat-item-grips-side.png");
            if (EQUIPMENT) {
                if (age == 1280) {
                    check(CatEquipmentSmokeTest.customModels > 0, "Modded armor model hooks render against the actual feline player");
                    mc.player.closeContainer();
                }
                if (age >= 1295) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                if (age == 1300 || age == 1320 || age == 1330 || age == 1380) capture(mc, "cat-flight-" + age + ".png");
                if (age == 1400) {
                    Nekogenix.LOGGER.info("Nekogenix equipment smoke passed: armor dye trims glint custom model hooks heads elytra and progressive falling");
                    mc.stop();
                }
            } else if (age >= 1280) mc.stop();
        }

        private static final class GripPreview extends net.minecraft.client.gui.screens.Screen {
            private final boolean side;
            private GripPreview(boolean side) { super(net.minecraft.network.chat.Component.literal("Feline item grips")); this.side = side; }
            @Override public boolean isPauseScreen() { return false; }
            @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                graphics.fill(0, 0, width, height, 0xff75828b);
                var player = minecraft.player;
                var main = player.getMainHandItem(); var off = player.getOffhandItem();
                var items = new net.minecraft.world.item.Item[] {Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL,
                        Items.BOW, Items.CROSSBOW, Items.SHIELD, Items.TRIDENT, Items.FISHING_ROD, Items.SPYGLASS, Items.APPLE,
                        Items.STONE, Items.TORCH, Items.POTION, Content.STABILIZING_INJECTION.get(), Items.FILLED_MAP};
                int cellWidth = width / 4, cellHeight = height / 4;
                try {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    for (int i = 0; i < items.length; i++) {
                        int x = i % 4 * cellWidth, y = i / 4 * cellHeight;
                        player.setItemInHand(InteractionHand.MAIN_HAND, items[i].getDefaultInstance());
                        graphics.drawString(font, items[i].getDescription(), x + 3, y + 3, 0xffffff, false);
                        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                                x + 2, y + 13, x + cellWidth - 2, y + cellHeight, (int)(cellHeight * .7), 0,
                                side ? 3.3F : .8F, .2F, player);
                    }
                } finally {
                    player.setItemInHand(InteractionHand.MAIN_HAND, main);
                    player.setItemInHand(InteractionHand.OFF_HAND, off);
                }
            }
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
            var texture = quad.getSprite().contents().name();
            try (var input = mc.getResourceManager().getResourceOrThrow(texture.withPath("textures/" + texture.getPath() + ".png")).open();
                    var image = com.mojang.blaze3d.platform.NativeImage.read(input)) {
                check(java.util.stream.IntStream.range(0, 16).anyMatch(x -> (image.getPixelRGBA(x, 0) >>> 24) != 0),
                        "The original needle remains in the injection texture");
                for (int py = 0; py < 16; py++) for (int px = 0; px < 16; px++) {
                    int alpha = image.getPixelRGBA(px, py) >>> 24;
                    if (alpha == 0) continue;
                    for (int corner = 0; corner < 4; corner++) {
                        float u = quad.getSprite().getU((px + corner % 2) / 16F), v = quad.getSprite().getV((py + corner / 2) / 16F);
                        float determinant = (b[4]-a[4])*(c[5]-a[5])-(b[5]-a[5])*(c[4]-a[4]);
                        float x = ((u-a[4])*(c[5]-a[5])-(v-a[5])*(c[4]-a[4]))/determinant;
                        float y = ((b[4]-a[4])*(v-a[5])-(b[5]-a[5])*(u-a[4]))/determinant;
                        var tip = new org.joml.Vector4f(a[0]+x*(b[0]-a[0])+y*(c[0]-a[0]),
                                a[1]+x*(b[1]-a[1])+y*(c[1]-a[1]), a[2]+x*(b[2]-a[2])+y*(c[2]-a[2]), 1);
                        tip.mul(pose.last().pose());
                        float vertical = -tip.z * (float)Math.tan(Math.toRadians(35));
                        if (py < 4) {
                            check(tip.y <= -vertical + .00001F, "The intact needle passes below the first person view");
                        } else {
                            check(tip.z < -.05F && Math.abs(tip.y) <= vertical + .00001F
                                    && Math.abs(tip.x) <= vertical * mc.getWindow().getWidth()/mc.getWindow().getHeight(),
                                    "The complete syringe body stays visible while the needle is inserted: " + px + "," + py + " corner " + corner + " position " + tip + " vertical " + vertical);
                        }
                    }
                }
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
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
