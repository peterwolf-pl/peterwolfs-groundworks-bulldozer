package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumeConservationTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Pushed material volume equals removed volume (material is neither lost nor duplicated)")
    void testStrictVolumeConservation() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // 1. Setup a tall granular pile and surrounding depressions
        // Pile at Z=2..4, height 7 layers
        for (int x = -1; x <= 1; x++) {
            for (int z = 2; z <= 4; z++) {
                terrain.createPile(new BlockPos(x, 65, z), GranularMaterialRegistry.DIRT, 7);
            }
        }

        // Small depression at Z=6
        terrain.createPile(new BlockPos(0, 65, 6), GranularMaterialRegistry.DIRT, 1);

        int initialWorldUnits = terrain.countTotalWorldUnits();
        assertTrue(initialWorldUnits > 0, "Initial terrain must contain units");

        int carriedUnits = 0;
        GranularMaterial carriedMaterial = GranularMaterial.EMPTY;

        double dozerZ = 0.0D;
        float bladeHeight = -0.20F; // Grader cut depth
        float bladeAngle = -2.5F;

        // Simulate 15 consecutive bulldozer movement steps forward
        for (int step = 0; step < 15; step++) {
            double nextZ = dozerZ + 0.35D; // ~0.35m per tick

            BladeTransform prevTransform = BladeTransform.compute(
                    new Vec3(0.0D, 65.0D, dozerZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
            );
            BladeTransform currTransform = BladeTransform.compute(
                    new Vec3(0.0D, 65.0D, nextZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
            );

            BladeTickResult result = BulldozerBladeController.tick(
                    terrain,
                    prevTransform,
                    currTransform,
                    carriedUnits,
                    carriedMaterial
            );

            carriedUnits = result.carriedUnitsAfter();
            carriedMaterial = result.carriedMaterialAfter();
            dozerZ = nextZ;

            // Invariant check: Total material in world + material in blade must EXACTLY equal initial amount
            int currentWorldUnits = terrain.countTotalWorldUnits();
            int totalSystemUnits = currentWorldUnits + carriedUnits;

            assertEquals(
                    initialWorldUnits,
                    totalSystemUnits,
                    String.format("Step %d: Material volume violated! Initial=%d, World=%d, Blade=%d (Sum=%d)",
                            step, initialWorldUnits, currentWorldUnits, carriedUnits, totalSystemUnits)
            );
        }

        // Verify that material actually moved and accumulated
        assertTrue(carriedUnits > 0 || terrain.simulatedPositions().size() > 0,
                "Material must have been pushed, carried, or deposited into forward berms/lateral spills");
    }

    @Test
    @DisplayName("Lateral spill and forward berm conserve 100% of material volume")
    void testSpillAndBermVolumeConservation() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // Start with blade pre-filled near capacity (500 units)
        int initialBladeUnits = 500;
        GranularMaterial initialMaterial = GranularMaterialRegistry.SAND;

        BladeTransform prev = BladeTransform.compute(new Vec3(0, 65, 10.0), 0, 0, 0, 0.0F, 0.0F);
        BladeTransform curr = BladeTransform.compute(new Vec3(0, 65, 10.5), 0, 0, 0, 0.0F, 0.0F);

        BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                prev,
                curr,
                initialBladeUnits,
                initialMaterial
        );

        int worldUnitsAfter = terrain.countTotalWorldUnits();
        int bladeUnitsAfter = result.carriedUnitsAfter();

        assertEquals(initialBladeUnits, worldUnitsAfter + bladeUnitsAfter,
                "Pushed forward and spilled units plus remaining blade units must match initial blade units");
        assertTrue(worldUnitsAfter > 0, "Some overloaded material must have spilled or pushed into berm");
        assertTrue(!terrain.simulatedPositions().isEmpty(), "Spill/berm positions must be marked for simulation");
    }
}
