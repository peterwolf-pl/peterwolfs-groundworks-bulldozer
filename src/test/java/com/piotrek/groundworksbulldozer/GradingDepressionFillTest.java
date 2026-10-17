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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingDepressionFillTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Blade uses carried material to fill a depression exactly to cutting grade")
    void fillsDepressionToCuttingGrade() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        // Current cutting edge is at world Y=65.0 and crosses Z=2.
        // Keep the rest of the blade width level, but leave the center cell
        // half full: four missing 1/8-block layers = 256 units.
        terrain.createFullCell(new BlockPos(-2, 64, 2), dirt);
        terrain.createFullCell(new BlockPos(-1, 64, 2), dirt);
        terrain.createPile(new BlockPos(0, 64, 2), dirt, 4);
        terrain.createFullCell(new BlockPos(1, 64, 2), dirt);

        int carriedBefore = 256;
        int worldBefore = terrain.countTotalWorldUnits();

        BladeTransform prev = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.0D),
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F
        );
        BladeTransform curr = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.30D),
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain, prev, curr, carriedBefore, dirt
        );

        GranularCell filled = terrain.getCell(new BlockPos(0, 64, 2));
        assertEquals(256, result.unitsDeposited());
        assertEquals(0, result.carriedUnitsAfter());
        assertEquals(GranularMaterial.EMPTY, result.carriedMaterialAfter());
        assertEquals(GranularCell.TOTAL_UNITS, filled.unitCount(),
                "Depression should be filled to the Y=65.0 cutting grade");
        assertEquals(worldBefore + carriedBefore, terrain.countTotalWorldUnits(),
                "All carried grading material must end up in the terrain");
        assertTrue(terrain.simulatedPositions().contains(new BlockPos(0, 64, 2)),
                "Filled grading cell should be scheduled for Groundworks relaxation");
    }

    @Test
    @DisplayName("Raised blade in full transport position does not fill depressions")
    void raisedBladeDoesNotGrade() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        terrain.createPile(new BlockPos(0, 64, 2), dirt, 4);
        int worldBefore = terrain.countTotalWorldUnits();
        int carriedBefore = 256;

        BladeTransform prev = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.0D),
                0.0F, 0.0F, 0.0F, 0.75F, 0.0F
        );
        BladeTransform curr = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.30D),
                0.0F, 0.0F, 0.0F, 0.75F, 0.0F
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain, prev, curr, carriedBefore, dirt
        );

        assertEquals(0, result.unitsDeposited());
        assertEquals(carriedBefore, result.carriedUnitsAfter());
        assertEquals(worldBefore, terrain.countTotalWorldUnits());
    }

    @Test
    @DisplayName("Lifting blade up spreads carried material in an even layer under the cutting edge")
    void liftingBladeSpreadsLayerUnderCuttingEdge() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        // Ground is at Y=64.0 (half cell of 4 layers = 256 units)
        terrain.createPile(new BlockPos(0, 64, 2), dirt, 4);
        int worldBefore = terrain.countTotalWorldUnits();
        int carriedBefore = 256;

        // Blade raised to +0.20m to spread a layer
        BladeTransform prev = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.0D),
                0.0F, 0.0F, 0.0F, 0.20F, 0.0F
        );
        BladeTransform curr = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 0.30D),
                0.0F, 0.0F, 0.0F, 0.20F, 0.0F
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain, prev, curr, carriedBefore, dirt
        );

        assertTrue(result.unitsDeposited() > 0, "Lifting blade must deposit and spread carried material");
        assertEquals(worldBefore + carriedBefore, terrain.countTotalWorldUnits() + result.carriedUnitsAfter(),
                "Volume must be strictly conserved while spreading");
    }
}
