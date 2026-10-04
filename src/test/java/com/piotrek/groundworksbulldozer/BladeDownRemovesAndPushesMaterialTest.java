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

class BladeDownRemovesAndPushesMaterialTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Blade down removes and pushes material from a granular dirt pile")
    void testBladeDownRemovesMaterial() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // Create a dirt pile in front of the machine (cutting edge hits Z=2 at Y=65)
        for (int x = -1; x <= 1; x++) {
            terrain.createPile(new BlockPos(x, 65, 2), GranularMaterialRegistry.DIRT, 6);
        }

        int initialUnits = terrain.countTotalWorldUnits();
        assertTrue(initialUnits > 0);

        // Lower blade to -0.15m (grading depth)
        float bladeHeight = -0.15F;
        float bladeAngle = -2.0F;

        Vec3 pos0 = new Vec3(0.0D, 65.0D, 0.0D);
        Vec3 pos1 = new Vec3(0.0D, 65.0D, 0.4D);

        BladeTransform transform0 = BladeTransform.compute(pos0, 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle);
        BladeTransform transform1 = BladeTransform.compute(pos1, 0.0F, 0.0F, 0.0F, bladeHeight, bladeAngle);

        BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                transform0,
                transform1,
                0,
                GranularMaterial.EMPTY
        );

        assertTrue(result.unitsExcavated() > 0, "Lowered blade must remove material from pile");
        assertTrue(result.carriedUnitsAfter() > 0, "Excavated material must accumulate in front of blade");
        assertEquals(GranularMaterialRegistry.DIRT.id(), result.carriedMaterialAfter().id());
        assertTrue(result.isPushing(), "Status must indicate active pushing");
    }

    @Test
    @DisplayName("Continuous blade height affects cutting volume: deep cut removes more than shallow cut")
    void testBladeHeightContinuousEffect() {
        // Run two identical scenarios with different blade heights
        // 1. Shallow cut: bladeHeight = -0.05
        TestGranularTerrain terrainShallow = new TestGranularTerrain();
        terrainShallow.createFullCell(new BlockPos(0, 64, 2), GranularMaterialRegistry.DIRT);
        terrainShallow.createPile(new BlockPos(0, 65, 2), GranularMaterialRegistry.DIRT, 4);

        BladeTransform s0 = BladeTransform.compute(new Vec3(0, 65, 0), 0, 0, 0, -0.05F, 0);
        BladeTransform s1 = BladeTransform.compute(new Vec3(0, 65, 0.4), 0, 0, 0, -0.05F, 0);
        BladeTickResult shallowResult = BulldozerBladeController.tick(terrainShallow, s0, s1, 0, GranularMaterial.EMPTY);

        // 2. Deep cut: bladeHeight = -0.40
        TestGranularTerrain terrainDeep = new TestGranularTerrain();
        terrainDeep.createFullCell(new BlockPos(0, 64, 2), GranularMaterialRegistry.DIRT);
        terrainDeep.createPile(new BlockPos(0, 65, 2), GranularMaterialRegistry.DIRT, 4);

        BladeTransform d0 = BladeTransform.compute(new Vec3(0, 65, 0), 0, 0, 0, -0.40F, 0);
        BladeTransform d1 = BladeTransform.compute(new Vec3(0, 65, 0.4), 0, 0, 0, -0.40F, 0);
        BladeTickResult deepResult = BulldozerBladeController.tick(terrainDeep, d0, d1, 0, GranularMaterial.EMPTY);

        assertTrue(shallowResult.unitsExcavated() > 0);
        assertTrue(deepResult.unitsExcavated() > shallowResult.unitsExcavated(),
                "Deep blade position must excavate more material than shallow grading position");
    }

    @Test
    @DisplayName("Supports dirt, sand, and gravel materials")
    void testMaterialSupport() {
        for (GranularMaterial mat : new GranularMaterial[]{
                GranularMaterialRegistry.DIRT,
                GranularMaterialRegistry.SAND,
                GranularMaterialRegistry.GRAVEL
        }) {
            TestGranularTerrain terrain = new TestGranularTerrain();
            terrain.createFullCell(new BlockPos(0, 64, 2), mat);
            terrain.createPile(new BlockPos(0, 65, 2), mat, 4);

            BladeTransform b0 = BladeTransform.compute(new Vec3(0, 65, 0), 0, 0, 0, -0.10F, 0);
            BladeTransform b1 = BladeTransform.compute(new Vec3(0, 65, 0.3), 0, 0, 0, -0.10F, 0);

            BladeTickResult res = BulldozerBladeController.tick(terrain, b0, b1, 0, GranularMaterial.EMPTY);

            assertTrue(res.unitsExcavated() > 0, "Must excavate " + mat.name());
            assertEquals(mat.id(), res.carriedMaterialAfter().id(), "Carried material must match " + mat.name());
        }
    }
}
