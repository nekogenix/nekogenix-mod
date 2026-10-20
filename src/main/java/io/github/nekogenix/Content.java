package io.github.nekogenix;

import com.mojang.serialization.Codec;
import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.gameplay.FelineJump;
import io.github.nekogenix.item.SalivaBottleItem;
import io.github.nekogenix.item.NekoInjectionItem;
import io.github.nekogenix.item.NucleocardItem;
import io.github.nekogenix.item.SalivaProjectile;
import io.github.nekogenix.state.NekoState;
import io.github.nekogenix.state.BreedingState;
import io.github.nekogenix.centrifuge.CentrifugeBlock;
import io.github.nekogenix.centrifuge.CentrifugeBlockEntity;
import io.github.nekogenix.centrifuge.CentrifugeMenu;
import io.github.nekogenix.pneumatic.PneumaticStandBlock;
import io.github.nekogenix.pneumatic.PneumaticStandBlockEntity;
import io.github.nekogenix.pneumatic.PneumaticStandMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Content {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nekogenix.ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Nekogenix.ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nekogenix.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Nekogenix.ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Nekogenix.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Nekogenix.ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nekogenix.ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nekogenix.ID);

    public static final DeferredItem<Item> NEEDLE = ITEMS.registerSimpleItem("syringe_needle");
    public static final DeferredItem<Item> BARREL = ITEMS.registerSimpleItem("syringe_barrel");
    public static final DeferredItem<Item> PLUNGER = ITEMS.registerSimpleItem("syringe_plunger");
    public static final DeferredItem<Item> SYRINGE = ITEMS.registerSimpleItem("syringe", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> USED_SYRINGE = ITEMS.registerSimpleItem("used_syringe", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> COTTON_SWAB = ITEMS.registerSimpleItem("cotton_swab");
    public static final DeferredItem<Item> SALIVA_SWAB = ITEMS.registerSimpleItem("cat_saliva_swab");
    public static final DeferredItem<SalivaBottleItem> SALIVA_BOTTLE = ITEMS.register("bottle_of_cat_saliva",
            () -> new SalivaBottleItem(false));
    public static final DeferredItem<SalivaBottleItem> SPLASH_SALIVA = ITEMS.register("splash_bottle_of_cat_saliva",
            () -> new SalivaBottleItem(true));
    public static final DeferredItem<NekoInjectionItem> INJECTION = ITEMS.register("neko_injection", () -> new NekoInjectionItem());
    public static final DeferredItem<Item> NEKO_STABILIZER = ITEMS.register("neko_stabilizer", io.github.nekogenix.item.SweetAmethystItem::new);
    public static final DeferredItem<io.github.nekogenix.item.ThrownBottleItem> LINGERING_SALIVA = ITEMS.register("lingering_bottle_of_cat_saliva", () -> new io.github.nekogenix.item.ThrownBottleItem(false, true));
    public static final DeferredItem<io.github.nekogenix.item.ThrownBottleItem> SPLASH_SWEET_AMETHYST = ITEMS.register("splash_potion_of_sweet_amethyst", () -> new io.github.nekogenix.item.ThrownBottleItem(true, false));
    public static final DeferredItem<io.github.nekogenix.item.ThrownBottleItem> LINGERING_SWEET_AMETHYST = ITEMS.register("lingering_potion_of_sweet_amethyst", () -> new io.github.nekogenix.item.ThrownBottleItem(true, true));
    public static final DeferredItem<NekoInjectionItem> STABILIZING_INJECTION = ITEMS.register("stabilizing_injection", () -> new NekoInjectionItem(true));
    public static final DeferredItem<NucleocardItem> BLANK_NUCLEOCARD = ITEMS.register("blank_nucleocard", () -> new NucleocardItem(false));
    public static final DeferredItem<NucleocardItem> CAT_NUCLEOCARD = ITEMS.register("cat_nucleocard", () -> new NucleocardItem(true));
    public static final DeferredBlock<CentrifugeBlock> CENTRIFUGE = BLOCKS.registerBlock("centrifuge", CentrifugeBlock::new,
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CENTRIFUGE_ITEM = ITEMS.registerSimpleBlockItem(CENTRIFUGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_ENTITY = BLOCK_ENTITIES.register("centrifuge",
            () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE_MENU = MENUS.register("centrifuge",
            () -> new MenuType<>(CentrifugeMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredBlock<PneumaticStandBlock> PNEUMATIC_STAND = BLOCKS.registerBlock("pneumatic_stand", PneumaticStandBlock::new,
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> PNEUMATIC_ITEM = ITEMS.registerSimpleBlockItem(PNEUMATIC_STAND);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticStandBlockEntity>> PNEUMATIC_ENTITY = BLOCK_ENTITIES.register("pneumatic_stand",
            () -> BlockEntityType.Builder.of(PneumaticStandBlockEntity::new, PNEUMATIC_STAND.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<PneumaticStandMenu>> PNEUMATIC_MENU = MENUS.register("pneumatic_stand",
            () -> new MenuType<>(PneumaticStandMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MobEffect, MobEffect> SHOCK = effect("injection_shock", MobEffectCategory.HARMFUL, 0xB769A2);
    public static final DeferredHolder<MobEffect, MobEffect> TRANSFORMING = effect("neko_transformation", MobEffectCategory.NEUTRAL, 0xD4A6DE);
    public static final DeferredHolder<MobEffect, MobEffect> CAT_TRANSFORMING = effect("cat_transformation", MobEffectCategory.NEUTRAL, 0xA98AD4);
    public static final DeferredHolder<MobEffect, MobEffect> AGILITY = effect("feline_agility", MobEffectCategory.BENEFICIAL, 0xA9D6D1);
    public static final DeferredHolder<MobEffect, MobEffect> VISION = effect("feline_vision", MobEffectCategory.BENEFICIAL, 0xBED487);
    public static final DeferredHolder<MobEffect, MobEffect> PAWS = effect("soft_paw_pads", MobEffectCategory.BENEFICIAL, 0xE9B6C9);
    public static final DeferredHolder<MobEffect, MobEffect> SWEETNESS = EFFECTS.register("sweetness", io.github.nekogenix.gameplay.SweetnessEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> CRYSTALLIZATION = EFFECTS.register("crystallization", io.github.nekogenix.gameplay.CrystallizationEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> SALIVA_CLOUD = EFFECTS.register("cat_saliva_exposure", io.github.nekogenix.gameplay.SalivaCloudEffect::new);

    public static final DeferredHolder<EntityType<?>, EntityType<SalivaProjectile>> SALIVA_PROJECTILE = ENTITIES.register("saliva_bottle",
            () -> EntityType.Builder.<SalivaProjectile>of(SalivaProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("nekogenix:saliva_bottle"));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NekoState>> NEKO = ATTACHMENTS.register("neko_state",
            () -> AttachmentType.serializable(NekoState::new).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> EXPOSURE = ATTACHMENTS.register("saliva_exposure",
            () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InjectionHandler.Session>> INJECTION_SESSION = ATTACHMENTS.register("injection_session",
            () -> AttachmentType.builder(InjectionHandler.Session::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> INJECTION_STARTED = ATTACHMENTS.register("injection_started",
            () -> AttachmentType.builder(() -> -1L).sync(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InjectionHandler.ReturnAnimation>> INJECTION_RETURN = ATTACHMENTS.register("injection_return",
            () -> AttachmentType.builder(() -> InjectionHandler.ReturnAnimation.NONE).sync(InjectionHandler.ReturnAnimation.STREAM_CODEC).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> OFFHAND_INJECTION_STARTED = ATTACHMENTS.register("offhand_injection_started",
            () -> AttachmentType.builder(() -> -1L).sync(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<InjectionHandler.ReturnAnimation>> OFFHAND_INJECTION_RETURN = ATTACHMENTS.register("offhand_injection_return",
            () -> AttachmentType.builder(() -> InjectionHandler.ReturnAnimation.NONE).sync(InjectionHandler.ReturnAnimation.STREAM_CODEC).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> FELINE_VOICE = ATTACHMENTS.register("feline_voice",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> CAT_FORM = ATTACHMENTS.register("cat_form",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<io.github.nekogenix.gameplay.CameraProjectiles.View>> CAMERA_VIEW = ATTACHMENTS.register("camera_view",
            () -> AttachmentType.builder(io.github.nekogenix.gameplay.CameraProjectiles.View::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<io.github.nekogenix.gameplay.CatForm.Session>> CAT_SIZE = ATTACHMENTS.register("cat_size",
            () -> AttachmentType.builder(io.github.nekogenix.gameplay.CatForm.Session::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> FELINE_DIET = ATTACHMENTS.register("feline_diet",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FelineJump.Session>> FELINE_JUMP = ATTACHMENTS.register("feline_jump",
            () -> AttachmentType.builder(FelineJump.Session::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BreedingState>> BREEDING = ATTACHMENTS.register("neko_breeding",
            () -> AttachmentType.serializable(BreedingState::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> FIRST_OCEAN_SOUND = ATTACHMENTS.register("first_ocean_sound",
            () -> AttachmentType.builder(() -> -1).serialize(Codec.INT).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("nekogenix",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.nekogenix"))
                    .icon(() -> INJECTION.toStack()).displayItems((parameters, output) -> {
                        output.accept(CENTRIFUGE_ITEM);
                        output.accept(PNEUMATIC_ITEM);
                        output.accept(COTTON_SWAB);
                        output.accept(SALIVA_SWAB);
                        output.accept(SALIVA_BOTTLE);
                        output.accept(SPLASH_SALIVA);
                        output.accept(LINGERING_SALIVA);
                        output.accept(BLANK_NUCLEOCARD);
                        output.accept(CAT_NUCLEOCARD);
                        output.accept(NEEDLE);
                        output.accept(BARREL);
                        output.accept(PLUNGER);
                        output.accept(SYRINGE);
                        output.accept(USED_SYRINGE);
                        output.accept(INJECTION);
                        output.accept(STABILIZING_INJECTION);
                        output.accept(NEKO_STABILIZER);
                        output.accept(SPLASH_SWEET_AMETHYST);
                        output.accept(LINGERING_SWEET_AMETHYST);
                    }).build());

    private static DeferredHolder<MobEffect, MobEffect> effect(String name, MobEffectCategory category, int color) {
        return EFFECTS.register(name, () -> new MobEffect(category, color) {});
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Nekogenix.ID, path);
    }

    public static int itemCount() { return ITEMS.getEntries().size(); }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        EFFECTS.register(bus);
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
    }

    private Content() {}
}
