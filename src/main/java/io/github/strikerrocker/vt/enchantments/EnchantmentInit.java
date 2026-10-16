package io.github.strikerrocker.vt.enchantments;

import com.mojang.serialization.MapCodec;
import io.github.strikerrocker.vt.base.ForgeFeature;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static io.github.strikerrocker.vt.VanillaTweaks.MOD_ID;

public class EnchantmentInit extends ForgeFeature {
    // Deferred Registries
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM = DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    // GlobalLootModifierSerializer
    public static final Supplier<MapCodec<BlazingModifier>> BLAZING_MODIFIER = GLM.register("blazing", () -> BlazingModifier.CODEC);
    public static final Supplier<MapCodec<SiphonModifier>> SIPHON_MODIFIER = GLM.register("siphon", () -> SiphonModifier.CODEC);

    // Enchantments are data (data/vanillatweaks/enchantment); a disabled one is simply not loaded
    public static final ResourceKey<Enchantment> BLAZING = key("blazing");
    public static final ResourceKey<Enchantment> HOMING = key("homing");
    public static final ResourceKey<Enchantment> HOPS = key("hops");
    public static final ResourceKey<Enchantment> NIMBLE = key("nimble");
    public static final ResourceKey<Enchantment> SIPHON = key("siphon");
    public static final ResourceKey<Enchantment> VETERAN = key("veteran");
    public static final ResourceKey<Enchantment> VIGOR = key("vigor");

    public static ModConfigSpec.BooleanValue enableHops;
    public static ModConfigSpec.BooleanValue enableNimble;
    public static ModConfigSpec.BooleanValue enableSiphon;
    public static ModConfigSpec.BooleanValue enableVeteran;
    public static ModConfigSpec.BooleanValue enableVigor;
    public static ModConfigSpec.BooleanValue enableHoming;
    public static ModConfigSpec.BooleanValue enableBlazing;

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }

    /**
     * The enchantment in this level's registries, or null if it is turned off
     */
    public static Holder<Enchantment> get(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(registry -> registry.get(key)).orElse(null);
    }

    /**
     * Adds the glowing effect to the targeting entity when using the bow
     */
    @SubscribeEvent
    public void useItem(LivingEntityUseItemEvent.Tick event) {
        if (!event.getEntity().level().isClientSide() && enableHoming.get()) {
            LivingEntity player = event.getEntity();
            int homingLvl = EnchantmentImpl.level(get(player.level(), HOMING), event.getItem());
            if (homingLvl > 0) {
                LivingEntity target = EnchantmentImpl.getHomingTarget(player.level(), player, homingLvl);
                if (target != null)
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 4, 1, true, false, false));
            }
        }
    }

    /**
     * Handles the logic of Nimble, Vigor & Hops enchantment
     */
    @SubscribeEvent
    public void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            if (enableNimble.get() && event.getSlot() == EquipmentSlot.FEET) {
                EnchantmentImpl.triggerNimble(entity, get(entity.level(), NIMBLE));
            }
            if (enableVigor.get() && event.getSlot() == EquipmentSlot.CHEST) {
                EnchantmentImpl.triggerVigor(entity, get(entity.level(), VIGOR));
            }
            if (enableHops.get() && event.getSlot() == EquipmentSlot.FEET) {
                EnchantmentImpl.triggerHops(entity, get(entity.level(), HOPS));
            }
        }
    }

    @Override
    public void setupConfig(ModConfigSpec.Builder builder) {
        enableBlazing = builder.translation("config.vanillatweaks:enableBlazing").comment("Want to smelt things when you mine them?").define("enableBlazing", true);
        enableHops = builder.translation("config.vanillatweaks:enableHops").comment("Want to jump more than a block high with an enchantment?").define("enableHops", true);
        enableNimble = builder.translation("config.vanillatweaks:enableNimble").comment("Want more speed with an enchantment?").define("enableNimble", true);
        enableSiphon = builder.translation("config.vanillatweaks:enableSiphon").comment("Don't want the zombies stealing your items when you are mining?").define("enableSiphon", true);
        enableVeteran = builder.translation("config.vanillatweaks:enableVeteran").comment("Want all the experience in the nearby area?").define("enableVeteran", true);
        enableVigor = builder.translation("config.vanillatweaks:enableVigor").comment("Want more health with an enchant?").define("enableVigor", true);
        enableHoming = builder.translation("config.vanillatweaks:enableHoming").comment("Don't want to aim but love shooting arrows?").define("enableHoming", true);
    }

    @Override
    public boolean usesEvents() {
        return true;
    }

    /*
     * Retargets the arrow towards the targeted entity
     */
    @SubscribeEvent
    public void entityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && enableHoming.get() && event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity shooter) {
            EnchantmentImpl.triggerHoming(arrow, shooter, get(event.getLevel(), HOMING));
        }
    }

    @SubscribeEvent
    public void onTick(LevelTickEvent.Post event) {
        if (enableVeteran.get() && event.getLevel() instanceof ServerLevel world) {
            Holder<Enchantment> veteran = get(world, VETERAN);
            if (veteran != null) {
                world.getEntities(EntityType.EXPERIENCE_ORB, EntitySelector.ENTITY_STILL_ALIVE).forEach(experienceOrb -> EnchantmentImpl.moveXP(experienceOrb, veteran));
            }
        }
    }
}
