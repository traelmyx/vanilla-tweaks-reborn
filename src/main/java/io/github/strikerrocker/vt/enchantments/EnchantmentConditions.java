package io.github.strikerrocker.vt.enchantments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Loads an enchantment only when it is turned on in the config
 */
public final class EnchantmentConditions implements ICondition {
    public static final MapCodec<EnchantmentConditions> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Codec.STRING.fieldOf("object").forGetter(condition -> condition.object)
    ).apply(builder, EnchantmentConditions::new));
    public static final String NAME = "enchantment";
    private final String object;

    public EnchantmentConditions(String object) {
        this.object = object;
    }

    @Override
    public boolean test(IContext context) {
        return switch (object) {
            case "blazing" -> EnchantmentInit.enableBlazing.get();
            case "homing" -> EnchantmentInit.enableHoming.get();
            case "hops" -> EnchantmentInit.enableHops.get();
            case "nimble" -> EnchantmentInit.enableNimble.get();
            case "siphon" -> EnchantmentInit.enableSiphon.get();
            case "veteran" -> EnchantmentInit.enableVeteran.get();
            case "vigor" -> EnchantmentInit.enableVigor.get();
            default -> false;
        };
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
