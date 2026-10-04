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

class BladeUpNoTerrainModificationTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Blade up does not modify terrain when driving forward over ground")
    void testBladeUpNoModification() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // Populate flat dirt terrain at Y=64 (from X=-2..2, Z=0..5)
        for (int x = -2; x <= 2; x++) {
            for (int z = 0; z <= 5; z++) {
                terrain.createFullCell(new BlockPos(x, 64, z), GranularMaterialRegistry.DIRT);
            }
        }

        int initialUnits = terrain.countTotalWorldUnits();
        assertTrue(initialUnits > 0);

        // Blade raised to transport position (+0.50m above track ground level Y=65.0)
        float raisedBladeHeight = 0.50F;
        float bladeAngle = 5.0F;

        // Drive forward across Z=0 to Z=3
        Vec3 pos0 = new Vec3(0.0D, 65.0D, 0.0D);
        Vec3 pos1 = new Vec3(0.0D, 65.0D, 0.5D);

        BladeTransform transform0 = BladeTransform.compute(pos0, 0.0F, 0.0F, 0.0F, raisedBladeHeight, bladeAngle);
        BladeTransform transform1 = BladeTransform.compute(pos1, 0.0F, 0.0F, 0.0F, raisedBladeHeight, bladeAngle);

        BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                transform0,
                transform1,
                0,
                GranularMaterial.EMPTY
        );

        // Blade is up: must not modify terrain
        assertEquals(0, result.unitsExcavated(), "Raised blade must not excavate flat terrain");
        assertEquals(0, result.unitsDeposited(), "Raised blade must not deposit when empty");
        assertEquals(0, result.carriedUnitsAfter(), "Blade should carry 0 units");
        assertEquals(initialUnits, terrain.countTotalWorldUnits(), "Total terrain units must remain unchanged");
    }
}
