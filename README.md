# Vanilla Tweaks Reborn

A bundle of small changes that make survival a bit less fiddly without changing what Minecraft feels like: a pedestal to show off your gear, hoes that clear a whole patch of crops, spawners you can pick up with Silk Touch, seven new enchantments, and a handful of recipes and tweaks. Everything can be turned off on its own.

Unofficial 1.21.1 port of [VanillaTweaks](https://www.curseforge.com/minecraft/mc-mods/vanillatweaks) by StrikerRocker, for NeoForge. Please report bugs [here](https://github.com/traelmyx/vanilla-tweaks-reborn/issues).

## Blocks and items

- **Pedestal.** Right-click with an item to put it on display, right-click with an empty hand to take it back, sneak-right-click to open its slot. Hoppers can feed it.
- **Charcoal, flint and sugar blocks.** Nine into one and back. A charcoal block burns as long as a coal block.
- **Dynamite.** Weaker TNT you can throw, or fire from a dispenser. If it has been in water it drops back as an item instead of going off.
- **Crafting Pad.** A crafting table that fits in your pocket.
- **Slime Bucket.** Right-click to see whether you're standing in a slime chunk.
- **Fried Egg.** Cook an egg in a furnace or smoker.

## Tweaks

- Hoes work like sickles: breaking a crop also harvests the ones around it (3×3 for most hoes, 5×5 for diamond, 7×7 for netherite).
- Mine a spawner with Silk Touch to pick it up, mob and all.
- Named mobs drop their name tag when you kill them, and you can shear a name tag off a living mob.
- Sneak-right-click an armor stand to swap your armor with it.
- Sneak-right-click an item frame to turn the item backwards.
- TNT next to lava or a magma block goes off on its own.
- Flaming arrows set the block they hit on fire.
- Creepers and baby zombies burn in daylight.
- Bats can drop leather.
- Saplings and other plants dropped on the ground plant themselves after a few seconds.
- Wolves, foxes and ocelots don't leave meat behind when they hunt.
- No hidden lava springs in the Nether.
- Bee nests and beehives show their bees and honey level in the advanced tooltip (F3+H).

## Enchantments

They show up in the enchanting table, in loot and from librarians, like vanilla ones.

- **Blazing** (mining tools): smelts what you mine.
- **Siphon** (mining tools): drops go straight into your inventory.
- **Homing** (bows): arrows curve towards the mob you're aiming near, and that mob glows while you draw.
- **Veteran** (helmets): pulls nearby experience orbs to you.
- **Vigor** (chestplates): more max health.
- **Nimble** (boots): faster walking.
- **Hops** (boots): higher jumps.

## Recipes

Easier recipes for chests (four from eight logs), trapped chests, repeaters and name tags, plus wool back into string.

## Config

Everything is in `config/vanillatweaks-common.toml`, one switch per feature, plus dynamite power and cooldown, bat leather chance and the self-planting delay and blacklist. Turning an enchantment, block or recipe off takes effect after a restart.

## FAQ

**Client or server?** Both. Install it on the server and on every client.

**Does it need anything else?** No.

**Can I use it in my modpack?** Yes, go ahead, no need to ask.

## Building

`./gradlew build` builds the jar and runs the game tests. `./gradlew runClientSmoke` starts a client, builds a small scene and takes a screenshot.

## License

MIT, same as the original. See [LICENSE](LICENSE).
