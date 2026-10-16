package io.github.strikerrocker.vt.test;

import io.github.strikerrocker.vt.content.blocks.ForgeBlocks;
import io.github.strikerrocker.vt.content.blocks.pedestal.PedestalBlockEntity;
import io.github.strikerrocker.vt.content.items.ForgeItems;
import io.github.strikerrocker.vt.content.items.dynamite.DynamiteEntity;
import io.github.strikerrocker.vt.enchantments.EnchantmentInit;
import io.github.strikerrocker.vt.tweaks.TweaksImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

@GameTestHolder(VanillaTweaksTests.MOD_ID)
@PrefixGameTestTemplate(false)
public class VanillaTweaksTests {
    public static final String MOD_ID = "vanillatweaks_gametest";

    private static Holder<Enchantment> ench(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(ench(helper, key), level);
        return stack;
    }

    private static List<ItemEntity> itemsNear(GameTestHelper helper, BlockPos rel, double radius) {
        BlockPos abs = helper.absolutePos(rel);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(radius));
    }

    @GameTest(template = "empty")
    public static void enchantmentsAreLoaded(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ResourceKey<Enchantment> key : List.of(EnchantmentInit.BLAZING, EnchantmentInit.HOMING, EnchantmentInit.HOPS,
                EnchantmentInit.NIMBLE, EnchantmentInit.SIPHON, EnchantmentInit.VETERAN, EnchantmentInit.VIGOR)) {
            Holder<Enchantment> holder = registry.get(key).orElse(null);
            helper.assertTrue(holder != null, "missing enchantment " + key.location());
            helper.assertTrue(holder.is(EnchantmentTags.IN_ENCHANTING_TABLE), key.location() + " is not in the enchanting table");
        }
        helper.assertTrue(EnchantmentInit.get(helper.getLevel(), EnchantmentInit.BLAZING) != null, "EnchantmentInit.get returned null");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesAreLoaded(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of("content/pedestal", "content/charcoal_block", "content/sugar_block", "content/flint_block",
                "content/crafting_pad", "content/dynamite", "content/fried_egg", "content/slime_bucket", "vanilla/better_chest", "vanilla/nametag")) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("vanillatweaks", id);
            helper.assertTrue(recipes.byKey(rl).isPresent(), "recipe not loaded: " + rl);
        }
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS)
                .containsKey(ResourceLocation.fromNamespaceAndPath("vanillatweaks", "no_more_lava_pocket_gen")), "biome modifier not loaded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void netherHasNoLavaPockets(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var biome = nether.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.NETHER_WASTES);
        var springClosed = nether.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                .getHolderOrThrow(net.minecraft.data.worldgen.placement.NetherPlacements.SPRING_CLOSED);
        boolean present = biome.value().getGenerationSettings().features().stream().anyMatch(set -> set.contains(springClosed));
        helper.assertFalse(present, "nether wastes still generate hidden lava springs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blazingSmeltsDrops(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack pick = enchanted(helper, new ItemStack(Items.IRON_PICKAXE), EnchantmentInit.BLAZING, 1);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.IRON_ORE.defaultBlockState());
        List<ItemStack> drops = Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), helper.getLevel(), pos, null, player, pick);
        helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(s -> s.is(Items.IRON_INGOT)), "blazing pickaxe did not smelt raw iron: " + drops);
        List<ItemStack> plain = Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), helper.getLevel(), pos, null, player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(plain.stream().allMatch(s -> s.is(Items.RAW_IRON)), "plain pickaxe was changed too: " + plain);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void siphonPutsDropsInInventory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        ItemStack shovel = enchanted(helper, new ItemStack(Items.IRON_SHOVEL), EnchantmentInit.SIPHON, 1);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        List<ItemStack> drops = Block.getDrops(Blocks.DIRT.defaultBlockState(), helper.getLevel(), pos, null, player, shovel);
        helper.assertTrue(drops.isEmpty(), "siphon left drops on the ground: " + drops);
        helper.assertTrue(player.getInventory().countItem(Items.DIRT) == 1, "dirt did not go into the inventory");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void armorEnchantmentsApplyOnEquip(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        double baseSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
        zombie.setItemSlot(EquipmentSlot.CHEST, enchanted(helper, new ItemStack(Items.IRON_CHESTPLATE), EnchantmentInit.VIGOR, 2));
        ItemStack boots = enchanted(helper, new ItemStack(Items.IRON_BOOTS), EnchantmentInit.NIMBLE, 1);
        boots.enchant(ench(helper, EnchantmentInit.HOPS), 2);
        zombie.setItemSlot(EquipmentSlot.FEET, boots);
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(zombie.getMaxHealth() - 24.0f) < 0.01f, "vigor II max health is " + zombie.getMaxHealth());
            helper.assertTrue(zombie.getAttribute(Attributes.MOVEMENT_SPEED).getValue() > baseSpeed * 1.19, "nimble did not speed up");
            helper.assertTrue(zombie.hasEffect(MobEffects.JUMP) && zombie.getEffect(MobEffects.JUMP).getAmplifier() == 2, "hops did not give jump boost");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void armorEnchantmentsRemovedOnUnequip(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setItemSlot(EquipmentSlot.CHEST, enchanted(helper, new ItemStack(Items.IRON_CHESTPLATE), EnchantmentInit.VIGOR, 2));
        zombie.setItemSlot(EquipmentSlot.FEET, enchanted(helper, new ItemStack(Items.IRON_BOOTS), EnchantmentInit.HOPS, 1));
        helper.runAfterDelay(10, () -> {
            zombie.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            zombie.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            helper.succeedWhen(() -> {
                helper.assertTrue(Math.abs(zombie.getMaxHealth() - 20.0f) < 0.01f, "vigor stayed after unequip: " + zombie.getMaxHealth());
                helper.assertFalse(zombie.hasEffect(MobEffects.JUMP), "hops stayed after unequip");
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void veteranPullsExperience(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(EquipmentSlot.HEAD, enchanted(helper, new ItemStack(Items.IRON_HELMET), EnchantmentInit.VETERAN, 1));
        Vec3 at = player.position();
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), at.x + 12, at.y + 1, at.z, 5);
        orb.setNoGravity(true);
        helper.getLevel().addFreshEntity(orb);
        double start = orb.distanceTo(player);
        helper.succeedWhen(() -> helper.assertTrue(!orb.isAlive() || orb.distanceTo(player) < start - 3, "orb did not move towards the player"));
    }

    @GameTest(template = "empty")
    public static void pedestalHoldsAndSavesItem(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ForgeBlocks.PEDESTAL_BLOCK.get());
        BlockPos abs = helper.absolutePos(rel);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.UP);
        helper.assertTrue(handler != null, "pedestal exposes no item handler");
        handler.insertItem(0, new ItemStack(Items.DIAMOND_SWORD), false);
        PedestalBlockEntity be = (PedestalBlockEntity) helper.getBlockEntity(rel);
        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        PedestalBlockEntity copy = new PedestalBlockEntity(abs, be.getBlockState());
        copy.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(copy.inventory.getStackInSlot(0).is(Items.DIAMOND_SWORD), "item lost on save/load: " + saved);
        helper.getLevel().destroyBlock(abs, false);
        helper.assertTrue(itemsNear(helper, rel, 2).stream().anyMatch(e -> e.getItem().is(Items.DIAMOND_SWORD)), "breaking the pedestal did not drop its item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pedestalRightClick(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, ForgeBlocks.PEDESTAL_BLOCK.get());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 3));
        helper.useBlock(rel, player);
        PedestalBlockEntity be = (PedestalBlockEntity) helper.getBlockEntity(rel);
        helper.assertTrue(be.inventory.getStackInSlot(0).getCount() == 3, "right-click did not place the stack: " + be.inventory.getStackInSlot(0));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "hand not emptied");
        helper.useBlock(rel, player);
        helper.assertTrue(player.getMainHandItem().is(Items.GOLD_INGOT) && be.inventory.getStackInSlot(0).isEmpty(), "empty-hand click did not take the stack back");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hopperFeedsPedestal(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), ForgeBlocks.PEDESTAL_BLOCK.get());
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.HOPPER);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getBlockEntity(new BlockPos(1, 3, 1));
        hopper.setItem(0, new ItemStack(Items.EMERALD));
        helper.succeedWhen(() -> {
            PedestalBlockEntity be = (PedestalBlockEntity) helper.getBlockEntity(new BlockPos(1, 2, 1));
            helper.assertTrue(be.inventory.getStackInSlot(0).is(Items.EMERALD), "hopper did not insert into the pedestal");
        });
    }

    @GameTest(template = "empty")
    public static void silkTouchMovesSpawner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.SPAWNER);
        BlockPos abs = helper.absolutePos(rel);
        ((SpawnerBlockEntity) helper.getBlockEntity(rel)).setEntityId(EntityType.SKELETON, level.getRandom());
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.SILK_TOUCH, 1));
        boolean handled = TweaksImpl.triggerSpawnerBreak(level, abs, level.getBlockState(abs), player, true, true);
        helper.assertTrue(handled, "silk touch break was not handled");
        helper.assertBlockPresent(Blocks.AIR, rel);
        ItemStack drop = itemsNear(helper, rel, 2).stream().map(ItemEntity::getItem).filter(s -> s.is(Items.SPAWNER)).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(drop.has(DataComponents.CUSTOM_DATA), "spawner item has no spawner data");

        BlockPos rel2 = new BlockPos(1, 2, 3);
        helper.setBlock(rel2, Blocks.SPAWNER);
        TweaksImpl.triggerSpawnerPlacement(level, helper.absolutePos(rel2), drop, true);
        CompoundTag data = ((SpawnerBlockEntity) helper.getBlockEntity(rel2)).getSpawner().save(new CompoundTag());
        String id = data.getCompound("SpawnData").getCompound("entity").getString("id");
        helper.assertTrue(id.equals("minecraft:skeleton"), "placed spawner spawns " + id);

        ServerPlayer noSilk = helper.makeMockServerPlayerInLevel();
        noSilk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        BlockPos abs2 = helper.absolutePos(rel2);
        helper.assertFalse(TweaksImpl.triggerSpawnerBreak(level, abs2, level.getBlockState(abs2), noSilk, true, true), "spawner moved without silk touch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoeHarvestsAround(GameTestHelper helper) {
        BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
                helper.setBlock(new BlockPos(x, 2, z), ripe);
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, 2, 1)));
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.assertBlockNotPresent(Blocks.WHEAT, new BlockPos(x, 2, z));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tntNextToMagmaIgnites(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.MAGMA_BLOCK);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TNT));
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 2, 3.5)));
        helper.placeAt(player, new ItemStack(Items.TNT), new BlockPos(1, 1, 1), Direction.UP);
        helper.assertBlockNotPresent(Blocks.TNT, new BlockPos(1, 2, 1));
        helper.assertEntityPresent(EntityType.TNT, new BlockPos(1, 2, 1));
        helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(new BlockPos(1, 2, 1))).inflate(2)).forEach(e -> e.discard());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void namedMobsDropTheirNameTag(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setCustomName(Component.literal("Gerald"));
        zombie.hurt(helper.getLevel().damageSources().playerAttack(player), 1000);
        ItemStack tag = itemsNear(helper, new BlockPos(1, 2, 1), 3).stream().map(ItemEntity::getItem).filter(s -> s.is(Items.NAME_TAG)).findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!tag.isEmpty() && tag.getHoverName().getString().equals("Gerald"), "no name tag named Gerald dropped");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shearsTakeTheNameTagOff(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setCustomName(Component.literal("Gerald"));
        ItemStack shears = new ItemStack(Items.SHEARS);
        TweaksImpl.triggerShearNametag(player, InteractionHand.MAIN_HAND, shears, zombie, helper.getLevel(), true);
        helper.assertFalse(zombie.hasCustomName(), "zombie kept its name");
        helper.assertTrue(itemsNear(helper, new BlockPos(1, 2, 1), 3).stream().anyMatch(e -> e.getItem().is(Items.NAME_TAG)
                && e.getItem().getHoverName().getString().equals("Gerald")), "no name tag dropped");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorStandSwapAndFrameReverse(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        helper.assertTrue(TweaksImpl.triggerArmorStandSwap(player, stand, true), "swap not triggered");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE) && stand.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "armor not swapped");

        helper.setBlock(new BlockPos(1, 2, 3), Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 4)), Direction.SOUTH);
        helper.getLevel().addFreshEntity(frame);
        frame.setItem(new ItemStack(Items.ARROW));
        helper.assertTrue(TweaksImpl.triggerItemFrameReverse(frame, player, true), "reverse not triggered");
        helper.assertTrue(frame.getRotation() == 7, "frame rotation is " + frame.getRotation());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dispenserThrowsDynamite(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
        var dispenser = (net.minecraft.world.level.block.entity.DispenserBlockEntity) helper.getBlockEntity(rel);
        dispenser.setItem(0, new ItemStack(ForgeItems.DYNAMITE.get()));
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            var found = helper.getLevel().getEntitiesOfClass(DynamiteEntity.class, new AABB(helper.absolutePos(rel)).inflate(16));
            helper.assertTrue(!found.isEmpty(), "no dynamite thrown");
            found.forEach(e -> e.discard());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void droppedSaplingsPlantThemselves(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
        ItemEntity sapling = new ItemEntity(helper.getLevel(), helper.absoluteVec(new Vec3(1.5, 2.2, 1.5)).x, helper.absoluteVec(new Vec3(1.5, 2.2, 1.5)).y,
                helper.absoluteVec(new Vec3(1.5, 2.2, 1.5)).z, new ItemStack(Items.OAK_SAPLING));
        sapling.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(sapling);
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.OAK_SAPLING, new BlockPos(1, 2, 1)));
    }

    @GameTest(template = "empty")
    public static void friedEggIsFood(GameTestHelper helper) {
        var food = new ItemStack(ForgeItems.FRIED_EGG.get()).get(DataComponents.FOOD);
        helper.assertTrue(food != null && food.nutrition() == 5, "fried egg food: " + food);
        helper.succeed();
    }
}
