package com.piotrek.groundworksbulldozer.gametest;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.integration.groundworks.GroundworksBulldozerAdapter;
import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Real Groundworks integration checks for the bulldozer.
 *
 * <p>This test mutates an actual ServerLevel through the public Groundworks API
 * and the production bulldozer adapter/controller. It is intentionally separate
 * from the visual screenshot suite.
 */
public final class BulldozerGroundworksGameTest implements FabricClientGameTest {

    private static final int BASE_Y = 180;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .setUseConsistentSettings(true)
                .create()) {

            TestServerContext server = singleplayer.getServer();

            server.runOnServer(minecraftServer -> {
                ServerLevel level = minecraftServer.overworld();
                testTrackSamplingUsesPartialGroundworksSurface(level);
                testBladeCutsRealGroundworksTerrain(level);
                testBladeRejectsForeignMaterial(level);
            });

            context.waitTicks(5);
        }
    }

    private static void testTrackSamplingUsesPartialGroundworksSurface(ServerLevel level) {
        Vec3 base = new Vec3(0.5D, BASE_Y, 0.5D);

        BlockPos frontLeft = new BlockPos(-1, BASE_Y - 1, 2);
        BlockPos frontRight = new BlockPos(1, BASE_Y - 1, 2);
        BlockPos rearLeft = new BlockPos(-1, BASE_Y - 1, -2);
        BlockPos rearRight = new BlockPos(1, BASE_Y - 1, -2);

        level.setBlock(frontLeft, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(frontRight, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(rearLeft, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(rearRight, Blocks.DIRT.defaultBlockState(), 3);

        // Rear track contacts sit on half-height Groundworks terrain.
        GroundworksApi.excavateAbove(level, rearLeft, BASE_Y - 0.5D, 512);
        GroundworksApi.excavateAbove(level, rearRight, BASE_Y - 0.5D, 512);

        double rearSurface = GroundworksApi.getSurfaceWorldY(
                level, rearLeft, -0.6D, -1.2D);
        if (Math.abs(rearSurface - (BASE_Y - 0.5D)) > 0.126D) {
            throw new AssertionError("Expected half-height Groundworks rear surface, got " + rearSurface);
        }

        BulldozerTrackController tracks = new BulldozerTrackController();
        BulldozerTrackController.TrackState state = tracks.tick(
                level, base, 0.0F, 0.0F, 0.0F, true, false);

        if (!(state.pitch() < -0.5F)) {
            throw new AssertionError(
                    "Front tracks are higher than rear tracks, expected nose-up pitch; got "
                            + state.pitch());
        }
    }

    private static void testBladeRejectsForeignMaterial(ServerLevel level) {
        int z = 20;
        int initialSandUnits = 0;
        for (int x = -2; x <= 2; x++) {
            BlockPos pos = new BlockPos(x, BASE_Y - 1, z);
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
            initialSandUnits += effectiveUnits(level, pos);
        }

        BladeTransform previous = BladeTransform.compute(
                new Vec3(0.5D, BASE_Y, z - 2.45D),
                0.0F, 0.0F, 0.0F, -0.20F, -2.5F);
        BladeTransform current = BladeTransform.compute(
                new Vec3(0.5D, BASE_Y, z - 2.10D),
                0.0F, 0.0F, 0.0F, -0.20F, -2.5F);

        var result = BulldozerBladeController.tick(
                GroundworksBulldozerAdapter.of(level),
                previous,
                current,
                200,
                GranularMaterialRegistry.DIRT
        );

        int sandUnitsAfter = 0;
        for (int x = -2; x <= 2; x++) {
            sandUnitsAfter += effectiveUnits(level, new BlockPos(x, BASE_Y - 1, z));
        }

        if (result.unitsExcavated() != 0) {
            throw new AssertionError(
                    "Dirt-loaded production blade excavated foreign sand: "
                            + result.unitsExcavated());
        }
        if (sandUnitsAfter != initialSandUnits) {
            throw new AssertionError(
                    "Foreign sand changed under dirt-loaded blade: before="
                            + initialSandUnits + ", after=" + sandUnitsAfter);
        }
        if (result.carriedMaterialAfter().id() != GranularMaterialRegistry.DIRT.id()) {
            throw new AssertionError("Bulldozer carry material changed away from dirt");
        }
    }

    private static int effectiveUnits(ServerLevel level, BlockPos pos) {
        var cell = GroundworksApi.queryCell(level, pos);
        if (cell != null) {
            return cell.unitCount();
        }
        return GroundworksApi.getMaterial(level, pos) != null ? 512 : 0;
    }

    private static void testBladeCutsRealGroundworksTerrain(ServerLevel level) {
        int z = 12;
        for (int x = -2; x <= 2; x++) {
            level.setBlock(
                    new BlockPos(x, BASE_Y - 1, z),
                    Blocks.DIRT.defaultBlockState(),
                    3);
        }

        BlockPos center = new BlockPos(0, BASE_Y - 1, z);
        double before = GroundworksApi.getSurfaceWorldY(
                level, center, 0.5D, z + 0.45D);

        BladeTransform previous = BladeTransform.compute(
                new Vec3(0.5D, BASE_Y, z - 2.45D),
                0.0F, 0.0F, 0.0F, -0.20F, -2.5F);
        BladeTransform current = BladeTransform.compute(
                new Vec3(0.5D, BASE_Y, z - 2.10D),
                0.0F, 0.0F, 0.0F, -0.20F, -2.5F);

        var result = BulldozerBladeController.tick(
                GroundworksBulldozerAdapter.of(level),
                previous,
                current,
                0,
                com.piotrek.groundworks.api.material.GranularMaterial.EMPTY);

        if (result.unitsExcavated() <= 0) {
            throw new AssertionError("Production bulldozer blade did not excavate real Groundworks terrain");
        }
        if (result.carriedUnitsAfter() <= 0) {
            throw new AssertionError("Excavated units should remain in front of the blade");
        }

        double after = GroundworksApi.getSurfaceWorldY(
                level, center, 0.5D, z + 0.45D);
        if (!(after < before)) {
            throw new AssertionError(
                    "Groundworks surface should be lower after grading cut: before="
                            + before + ", after=" + after);
        }
    }
}
