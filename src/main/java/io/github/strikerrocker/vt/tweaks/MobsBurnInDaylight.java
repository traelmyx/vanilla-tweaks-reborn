package io.github.strikerrocker.vt.tweaks;

import io.github.strikerrocker.vt.base.ForgeFeature;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class MobsBurnInDaylight extends ForgeFeature {
    private ModConfigSpec.BooleanValue babyZombieBurnInDaylight;
    private ModConfigSpec.BooleanValue creeperBurnInDaylight;

    @Override
    public void setupConfig(ModConfigSpec.Builder builder) {
        babyZombieBurnInDaylight = builder
                .translation("config.vanillatweaks:babyZombieBurnInDaylight")
                .comment("Want baby zombies to burn by the light of the day?")
                .define("babyZombieBurnInDaylight", true);
        creeperBurnInDaylight = builder
                .translation("config.vanillatweaks:creeperBurnInDaylight")
                .comment("Want creeper's to burn in daylight?")
                .define("creeperBurnInDaylight", true);
    }

    @Override
    public boolean usesEvents() {
        return true;
    }


    @SubscribeEvent
    public void onLivingUpdate(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        TweaksImpl.triggerMobsBurnInSun(entity, creeperBurnInDaylight.get(), babyZombieBurnInDaylight.get());
    }
}