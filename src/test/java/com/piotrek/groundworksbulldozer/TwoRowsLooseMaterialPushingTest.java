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

import static org.junit.jupiter.api.Assertions.*;

class TwoRowsLooseMaterialPushingTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Bulldozer with blade at ground level pushes 2 rows of loose material without getting stuck or climbing")
    void testBulldozerPushesTwoRowsOfLooseMaterial() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // 2 rows of loose material (DIRT) at Y=64, Z=2 and Z=3 across 3 blocks width (X=-1..1)
        // Total 6 full blocks = 6 * 512 = 3072 units
        for (int x = -1; x <= 1; x++) {
            terrain.createFullCell(new BlockPos(x, 64, 2), GranularMaterialRegistry.DIRT);
            terrain.createFullCell(new BlockPos(x, 64, 3), GranularMaterialRegistry.DIRT);
        }

        int initialWorldUnits = terrain.countTotalWorldUnits();
        assertEquals(3072, initialWorldUnits, "2 rows of 3 blocks should be exactly 3072 units");

        float bladeHeight = 0.0F; // Ground level grading
        float bladeAngle = 0.0F;

        int carriedUnits = 0;
        GranularMaterial carriedMaterial = GranularMaterial.EMPTY;

        // Drive forward from Z=0 to Z=2.0 (blade cutting edge advances from Z=2.1 to Z=4.1)
        double currentZ = 0.0D;
        for (int tick = 0; tick < 20; tick++) {
            double nextZ = currentZ + 0.13D;

            BladeTransform prevTransform = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, currentZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
            );
            BladeTransform currTransform = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, nextZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
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

            // Strict volume conservation check at EVERY tick
            int currentWorldUnits = terrain.countTotalWorldUnits();
            assertEquals(
                    initialWorldUnits,
                    currentWorldUnits + carriedUnits,
                    "Volume must be strictly conserved at tick " + tick
            );

            currentZ = nextZ;
        }

        // Verify that BOTH row 1 (Z=2) and row 2 (Z=3) have been excavated and cleared
        for (int x = -1; x <= 1; x++) {
            var cellZ2 = terrain.getCell(new BlockPos(x, 64, 2));
            assertNull(cellZ2, "Row 1 at Z=2 must be completely cleared from the ground");

            var cellZ3 = terrain.getCell(new BlockPos(x, 64, 3));
            assertNull(cellZ3, "Row 2 at Z=3 must be completely cleared from the ground");
        }

        // Material from the 2 rows of blocks must have turned into a physical heap in the world in front of the blade!
        int worldUnitsAfter = terrain.countTotalWorldUnits();
        assertTrue(worldUnitsAfter > 0, "The 2 rows of blocks must have turned into a physical heap of material in the world in front of the blade");
        assertTrue(carriedUnits > 0, "A rolling surcharge must be retained in the blade");
        assertEquals(initialWorldUnits, worldUnitsAfter + carriedUnits, "Total system volume must strictly equal 3072 units");
        assertEquals(GranularMaterialRegistry.DIRT.id(), carriedMaterial.id());
    }

    @Test
    @DisplayName("Bulldozer pushes 3 rows of loose material (beyond capacity) into forward berm with strict volume conservation")
    void testBulldozerPushesThreeRowsIntoForwardBerm() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // 3 rows of loose material (SAND) at Y=64, Z=2, 3, 4 across 3 blocks width (X=-1..1)
        // Total 9 full blocks = 9 * 512 = 4608 units (exceeds 3072 unit blade capacity!)
        for (int x = -1; x <= 1; x++) {
            terrain.createFullCell(new BlockPos(x, 64, 2), GranularMaterialRegistry.SAND);
            terrain.createFullCell(new BlockPos(x, 64, 3), GranularMaterialRegistry.SAND);
            terrain.createFullCell(new BlockPos(x, 64, 4), GranularMaterialRegistry.SAND);
        }

        int initialWorldUnits = terrain.countTotalWorldUnits();
        assertEquals(4608, initialWorldUnits, "3 rows of 3 blocks should be exactly 4608 units");

        float bladeHeight = 0.0F;
        float bladeAngle = 0.0F;

        int carriedUnits = 0;
        GranularMaterial carriedMaterial = GranularMaterial.EMPTY;

        // Drive forward across Z=0 to Z=3.0 (cutting edge advances to Z=5.1)
        double currentZ = 0.0D;
        for (int tick = 0; tick < 25; tick++) {
            double nextZ = currentZ + 0.13D;

            BladeTransform prevTransform = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, currentZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
            );
            BladeTransform currTransform = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, nextZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
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

            // Strict volume conservation check at EVERY tick
            int currentWorldUnits = terrain.countTotalWorldUnits();
            assertEquals(
                    initialWorldUnits,
                    currentWorldUnits + carriedUnits,
                    "Volume must be strictly conserved at tick " + tick
            );

            currentZ = nextZ;
        }

        // Verify that Row 1 and Row 2 are completely cleared
        for (int x = -1; x <= 1; x++) {
            assertNull(terrain.getCell(new BlockPos(x, 64, 2)), "Row 1 must be cleared");
            assertNull(terrain.getCell(new BlockPos(x, 64, 3)), "Row 2 must be cleared");
        }

        // Live rolling surcharge is retained in the blade
        assertTrue(carriedUnits >= 500, "Blade must retain live surcharge (was " + carriedUnits + ")");
        // And the heap of material is actively pushed in the world in front of the blade
        int worldUnitsAfter = terrain.countTotalWorldUnits();
        assertTrue(worldUnitsAfter > 3000, "Major heap of material must be pushed into the world (was " + worldUnitsAfter + ")");
        assertEquals(initialWorldUnits, worldUnitsAfter + carriedUnits,
                "Total system volume must strictly equal initial volume");
    }
}
