package com.piotrek.groundworksbulldozer.gametest;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Path;

/**
 * Visual regression test suite for Peterwolf's Groundworks Bulldozer.
 *
 * <p>Validates:
 * <ul>
 *   <li>Industrial tracked bulldozer with wide curved front blade and heavy C-frame.</li>
 *   <li>Hollow ROPS safety cab with clear safety glass and operator seat.</li>
 *   <li>Animated blade lifting and lowering with hydraulic cylinders.</li>
 *   <li>In-cab view showing direct line-of-sight to the blade and work area.</li>
 * </ul>
 *
 * <p>Run with: {@code ./gradlew runClientGameTest}
 */
public final class BulldozerVisualGameTest implements FabricClientGameTest {

    private static final int BASE_Y = 180;
    private static final Path SCREENSHOT_DIR = Path.of(
            System.getProperty("bulldozer.visualOutputDir", "visual-tests/current")
    ).toAbsolutePath().normalize();

    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();

        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .create()) {

            TestServerContext server = singleplayer.getServer();
            TestServerConnection connection = singleplayer.getConnection();

            configureWorld(server);
            setupViewingPlatform(server);

            final GroundworksBulldozerEntity[] dozerHolder = new GroundworksBulldozerEntity[1];
            server.runOnServer(minecraftServer -> {
                ServerLevel level = minecraftServer.overworld();
                GroundworksBulldozerEntity dozer = new GroundworksBulldozerEntity(
                        GroundworksBulldozerMod.BULLDOZER, level
                );
                dozer.setPos(0.5D, BASE_Y, 0.5D);
                dozer.setYRot(0.0F);
                level.addFreshEntity(dozer);
                dozerHolder[0] = dozer;
            });

            connection.waitForChunksRender();
            context.waitTicks(160);

            // ── Scene 1: Isometric Profile (Front-Left with Blade & Tracks) ──
            server.runCommand("teleport @a -4.8 182.8 5.2 -136 15");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_01_profile_isometric");

            // ── Scene 2: Front Blade & Hydraulic Lift Cylinders Close-Up ─────
            server.runCommand("teleport @a 0.0 181.8 4.2 180 12");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_02_front_blade_and_cylinders");

            // ── Scene 3: Cabin, Amber Beacon & Exhaust Stack ─────────────────
            server.runCommand("teleport @a -2.5 182.6 1.5 -135 8");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_03_cab_and_beacon");

            // ── Scene 4: Crawler Tracks & Sprockets ──────────────────────────
            server.runCommand("teleport @a -4.2 181.2 0.5 -90 8");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_04_tracks_and_rollers");

            // ── Scene 5: Operator Seated Inside Glass Cab (Exterior View) ────
            server.runOnServer(minecraftServer -> {
                var players = minecraftServer.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    players.get(0).startRiding(dozerHolder[0]);
                }
            });
            context.waitTicks(10);
            server.runCommand("teleport @a -2.8 182.8 3.5 -135 12");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_05_player_in_glass_cab");

            // ── Scene 6: Operator Work Area View through Windshield ──────────
            server.runCommand("teleport @a 0.0 182.35 0.8 0 14");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_06_in_cab_blade_view");

            // ── Scene 7: Lowered Blade in Grading Stance ─────────────────────
            server.runOnServer(minecraftServer -> {
                GroundworksBulldozerEntity dozer = dozerHolder[0];
                if (dozer != null) {
                    dozer.setBladeHeight(-0.35F);
                    dozer.setControlInputs(0.0F, 0.0F, -1.0F, 0.0F);
                }
            });
            context.waitTicks(15);
            server.runCommand("teleport @a 3.5 182.2 4.2 145 16");
            context.waitTicks(10);
            capture(context, connection, "bulldozer_07_blade_lowered_grading");

            // ── Scene 8: Dismount & Targetability / Removal Verification ────
            server.runOnServer(minecraftServer -> {
                GroundworksBulldozerEntity dozer = dozerHolder[0];
                var players = minecraftServer.getPlayerList().getPlayers();
                if (!players.isEmpty() && dozer != null) {
                    ServerPlayer player = players.get(0);
                    if (!dozer.isPickable()) {
                        throw new AssertionError("Bulldozer must be pickable!");
                    }
                    if (!dozer.isAttackable()) {
                        throw new AssertionError("Bulldozer must be attackable!");
                    }
                    player.stopRiding();
                    if (dozer.hasPassenger(player)) {
                        throw new AssertionError("Player must be able to dismount!");
                    }
                }
            });
            context.waitTicks(10);
        }
    }

    private static void configureWorld(TestServerContext server) {
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamerule doDaylightCycle false");
        server.runCommand("gamerule doWeatherCycle false");
    }

    private static void setupViewingPlatform(TestServerContext server) {
        server.runOnServer(minecraftServer -> {
            ServerLevel level = minecraftServer.overworld();
            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    level.setBlock(new BlockPos(x, BASE_Y - 1, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
        });
    }

    private static void capture(ClientGameTestContext context, TestServerConnection connection, String name) {
        context.waitTicks(3);
        connection.waitForClientboundPackets();
        context.takeScreenshot(TestScreenshotOptions.of(name)
                .disableCounterPrefix()
                .withSize(854, 480)
                .withDestinationDir(SCREENSHOT_DIR));
    }
}
