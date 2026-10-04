package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcceptanceGradingTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Acceptance Test: Lower blade, drive forward into dirt pile -> pile pushed, material accumulates, lateral spill occurs, terrain flattens, volume conserved")
    void testAcceptanceDirtPileLeveling() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // 1. Create a dirt pile in front of the bulldozer (at Z=2..4, width X=-1..1, 6 microvoxel layers tall)
        for (int x = -1; x <= 1; x++) {
            for (int z = 2; z <= 4; z++) {
                terrain.createPile(new BlockPos(x, 65, z), GranularMaterialRegistry.DIRT, 6);
            }
        }

        int initialTotalVolume = terrain.countTotalWorldUnits();
        assertTrue(initialTotalVolume > 0, "Initial pile must have non-zero granular units");

        // 2. Lower the blade using Arrow Down (bladeHeight = -0.20m, cutting below pile crest)
        float bladeHeight = -0.20F;
        float bladeAngle = -2.5F;

        int carriedUnits = 0;
        GranularMaterial carriedMaterial = GranularMaterial.EMPTY;
        boolean sawLateralSpill = false;
        boolean sawAccumulation = false;
        boolean sawPushing = false;

        // 3. Drive forward with W
        double currentZ = 0.0D;
        for (int tick = 0; tick < 12; tick++) {
            double nextZ = currentZ + 0.30D;

            BladeTransform prevTransform = BladeTransform.compute(
                    new Vec3(0.0D, 65.0D, currentZ), 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle
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

            if (result.isPushing()) sawPushing = true;
            if (carriedUnits > 64) sawAccumulation = true;

            // Check if material spilled or deposited into surrounding cells
            if (result.unitsDeposited() > 0) {
                sawLateralSpill = true;
            }

            // Invariant check at EVERY tick:
            int currentWorldUnits = terrain.countTotalWorldUnits();
            assertEquals(
                    initialTotalVolume,
                    currentWorldUnits + carriedUnits,
                    "Total Groundworks material volume must remain 100% unchanged at tick " + tick
            );

            currentZ = nextZ;
        }

        // ── Verify All Acceptance Criteria ───────────────────────────
        // 1. Blade visibly pushes the pile
        assertTrue(sawPushing, "Expected result: the blade visibly pushes the pile");

        // 2. Material accumulates in front of the blade
        assertTrue(sawAccumulation, "Expected result: material accumulates in front of the blade");

        // 3. Some material escapes around the sides / deposits
        assertTrue(sawLateralSpill, "Expected result: some material escapes around the sides");

        // 4. Terrain behind the blade becomes flatter
        // Check the original peak cell at Z=2: its height was 6 layers, now reduced/graded flatter
        GranularCell cellZ2 = terrain.getCell(new BlockPos(0, 65, 2));
        if (cellZ2 != null) {
            assertTrue(cellZ2.unitCount() < 6 * 64, "Terrain behind the blade must become flatter");
        }

        // 5. Total Groundworks material volume remains unchanged
        int finalWorldUnits = terrain.countTotalWorldUnits();
        assertEquals(initialTotalVolume, finalWorldUnits + carriedUnits,
                "Expected result: total Groundworks material volume remains unchanged");
    }
}
