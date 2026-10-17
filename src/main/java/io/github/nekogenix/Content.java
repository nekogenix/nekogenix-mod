package io.github.nekogenix;

import com.mojang.serialization.Codec;
import io.github.nekogenix.gameplay.InjectionHandler;
import io.github.nekogenix.item.SalivaBottleItem;
import io.github.nekogenix.item.NekoInjectionItem;
import io.github.nekogenix.item.SalivaProjectile;
import io.github.nekogenix.state.NekoState;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class Content {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Nekogenix.ID);
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
    public static final DeferredItem<NekoInjectionItem> INJECTION = ITEMS.register("neko_injection", NekoInjectionItem::new);

    public static final DeferredHolder<MobEffect, MobEffect> SHOCK = effect("injection_shock", MobEffectCategory.HARMFUL, 0xB769A2);
    public static final DeferredHolder<MobEffect, MobEffect> TRANSFORMING = effect("neko_transformation", MobEffectCategory.NEUTRAL, 0xD4A6DE);
    public static final DeferredHolder<MobEffect, MobEffect> AGILITY = effect("feline_agility", MobEffectCategory.BENEFICIAL, 0xA9D6D1);
    public static final DeferredHolder<MobEffect, MobEffect> VISION = effect("feline_vision", MobEffectCategory.BENEFICIAL, 0xBED487);
    public static final DeferredHolder<MobEffect, MobEffect> PAWS = effect("soft_paw_pads", MobEffectCategory.BENEFICIAL, 0xE9B6C9);

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
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> FELINE_VOICE = ATTACHMENTS.register("feline_voice",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("nekogenix",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.nekogenix"))
                    .icon(() -> INJECTION.toStack()).displayItems((parameters, output) -> {
                        ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    }).build());

    private static DeferredHolder<MobEffect, MobEffect> effect(String name, MobEffectCategory category, int color) {
        return EFFECTS.register(name, () -> new MobEffect(category, color) {});
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Nekogenix.ID, path);
    }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
        EFFECTS.register(bus);
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
    }

    private Content() {}
}
