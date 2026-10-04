package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReversingHeapDepositTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Reversing backwards deposits full carried blade contents as a blade-wide heap")
    void testReversingDepositsFullHeap() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        int carriedUnits = 600; // Almost full blade
        GranularMaterial material = GranularMaterialRegistry.DIRT;

        // Position at forward point (where bulldozer finished pushing)
        Vec3 forwardPos = new Vec3(0.0D, 65.0D, 5.0D);
        // Reverse motion: backing up to Z=4.6m (heading is +Z, so backing up is -Z)
        Vec3 reversePos = new Vec3(0.0D, 65.0D, 4.6D);

        BladeTransform prevTransform = BladeTransform.compute(forwardPos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        BladeTransform currTransform = BladeTransform.compute(reversePos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        int worldUnitsBefore = terrain.countTotalWorldUnits();
        assertEquals(0, worldUnitsBefore);

        BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                prevTransform,
                currTransform,
                carriedUnits,
                material
        );

        // 1. All carried units must be deposited into the ground
        assertEquals(carriedUnits, result.unitsDeposited(), "All carried units must be deposited upon reversing");
        assertEquals(0, result.carriedUnitsAfter(), "Blade carry buffer must be emptied onto the ground");
        assertEquals(GranularMaterial.EMPTY, result.carriedMaterialAfter(), "Carried material must reset to empty");

        // 2. Total system volume is strictly conserved
        int worldUnitsAfter = terrain.countTotalWorldUnits();
        assertEquals(carriedUnits, worldUnitsAfter, "World units must match exact deposited volume");

        // 3. Heap spans across multiple positions (blade width)
        assertTrue(result.affectedPositions().size() >= 3, "Heap must span across blade width positions");
        assertTrue(!terrain.simulatedPositions().isEmpty(), "Heap positions must be marked for Groundworks relaxation");
    }
}
