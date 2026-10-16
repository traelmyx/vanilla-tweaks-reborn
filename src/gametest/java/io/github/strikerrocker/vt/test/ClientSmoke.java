package io.github.strikerrocker.vt.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import io.github.strikerrocker.vt.content.blocks.ForgeBlocks;
import io.github.strikerrocker.vt.content.blocks.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;

// Run with -Dsmoketest=true: creates a flat world, takes a screenshot and quits. Exit code 1 on failure.
public final class ClientSmoke {

    public static final boolean ENABLED = Boolean.getBoolean("smoketest");
    private static final long TIMEOUT_MS = 5 * 60 * 1000;

    private static int stage;
    private static int ticks;

    private ClientSmoke() {
    }

    public static boolean start() {
        if (!ENABLED) {
            return false;
        }
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT_MS);
                fail("timed out");
            } catch (InterruptedException ignored) {
            }
        }, "smoketest-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        return true;
    }

    public static void tick(Minecraft mc) {
        try {
            switch (stage) {
                case 0 -> {
                    Screen screen = mc.screen;
                    if (screen != null && mc.getOverlay() == null && ++ticks >= 20) {
                        if (screen.getClass().getSimpleName().equals("LoadingErrorScreen")) {
                            fail("mod loading errors");
                        }
                        ticks = 0;
                        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                        createWorld(mc);
                        stage = 1;
                    }
                }
                case 1 -> {
                    if (mc.level != null && mc.player != null && mc.screen == null) {
                        stage = 2;
                    }
                }
                case 2 -> {
                    ticks++;
                    if (ticks == 40) {
                        setup(mc);
                    }
                    if (ticks == 60) {
                        mc.player.setXRot(28);
                        mc.player.setYRot(mc.player.getDirection().toYRot());
                        mc.options.hideGui = true;
                    }
                    if (ticks == 140) {
                        Screenshot.grab(mc.gameDirectory, "pedestal.png", mc.getMainRenderTarget(), msg -> {});
                        mc.options.hideGui = false;
                    }
                    if (ticks >= 160) {
                        BlockPos pos = mc.player.blockPosition().relative(mc.player.getDirection(), 2);
                        if (!(mc.level.getBlockEntity(pos) instanceof PedestalBlockEntity be) || !be.inventory.getStackInSlot(0).is(Items.DIAMOND_SWORD)) {
                            fail("client does not see the sword on the pedestal");
                        }
                        Files.writeString(mc.gameDirectory.toPath().resolve("smoke-ok"), "ok\n");
                        stage = 3;
                        mc.stop();
                    }
                }
                default -> {
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail(t.toString());
        }
    }

    private static void createWorld(Minecraft mc) {
        LevelSettings settings = new LevelSettings("smoke", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("smoke", settings, new WorldOptions(0L, false, false),
            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
            mc.screen);
    }

    // three pedestals with things on them two blocks in front of the player, storage blocks behind
    private static void setup(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayers().get(0);
            var level = player.serverLevel();
            var facing = player.getDirection();
            var side = facing.getClockWise();
            BlockPos centre = player.blockPosition().relative(facing, 2);
            ItemStack[] shown = {new ItemStack(Items.TRIDENT), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.TOTEM_OF_UNDYING)};
            for (int i = -1; i <= 1; i++) {
                BlockPos pos = centre.relative(side, i * 2);
                level.setBlockAndUpdate(pos, ForgeBlocks.PEDESTAL_BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(pos) instanceof PedestalBlockEntity be) {
                    be.inventory.setStackInSlot(0, shown[i + 1]);
                }
            }
            BlockPos back = centre.relative(facing, 3);
            var blocks = new net.minecraft.world.level.block.Block[] {ForgeBlocks.CHARCOAL_BLOCK.get(), ForgeBlocks.FLINT_BLOCK.get(), ForgeBlocks.SUGAR_BLOCK.get()};
            for (int i = -1; i <= 1; i++) {
                level.setBlockAndUpdate(back.relative(side, i), blocks[i + 1].defaultBlockState());
            }
        });
    }

    public static void fail(String why) {
        System.err.println("Smoke test failed: " + why);
        System.out.println("[clientsmoke] FAILED: " + why);
        System.err.flush();
        Runtime.getRuntime().halt(1);
    }
}
