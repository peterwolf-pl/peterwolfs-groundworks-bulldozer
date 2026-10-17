package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.entity.HeavyBulldozerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlopePlungingAndSurchargeTest {

    @Test
    @DisplayName("Heavy Bulldozer on a ramp plunges cutting edge below slope surface and scoops surcharge")
    void heavyBulldozerPlungesIntoRampAndScoops() {
        // Vehicle at (0, 64, 0), heading South (yaw 0), pitched 20 degrees nose-down on a ramp
        float pitch = 20.0F;
        float plungedHeight = -0.80F; // Plunging blade 80cm below tracks
        double armLength = HeavyBulldozerEntity.HEAVY_ARM_LENGTH; // 4.2m
        double bladeWidth = HeavyBulldozerEntity.HEAVY_BLADE_WIDTH; // 6.0m
        double bladeHeightDim = HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM; // 1.8m

        BladeTransform prevTransform = BladeTransform.compute(
                new Vec3(0, 64, 0),
                0.0F, pitch, 0.0F, plungedHeight, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );

        // Vehicle moves forward down the ramp by 0.5 meters
        double rad = Math.toRadians(pitch);
        Vec3 forwardDelta = new Vec3(0.0D, -Math.sin(rad) * 0.5D, Math.cos(rad) * 0.5D);
        BladeTransform currTransform = BladeTransform.compute(
                new Vec3(0, 64, 0).add(forwardDelta),
                0.0F, pitch, 0.0F, plungedHeight, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );

        // Calculate expected slope elevation at cutting edge:
        // Ramp surface at Z = 4.2 is 64.0 - 4.2 * sin(20°) = 62.564
        double rampSurfaceY = 64.0D - armLength * Math.sin(rad);
        double actualCuttingEdgeY = prevTransform.cuttingEdgeY();

        assertTrue(actualCuttingEdgeY < rampSurfaceY,
                "Cutting edge must penetrate BELOW the ramp surface (ramp: " + rampSurfaceY + ", edge: " + actualCuttingEdgeY + ")");
        assertEquals(rampSurfaceY + Math.cos(rad) * plungedHeight, actualCuttingEdgeY, 1e-3D);

        // Setup test terrain: soil filling the ramp volume
        TestGranularTerrain terrain = new TestGranularTerrain();
        for (int x = -4; x <= 4; x++) {
            for (int z = 2; z <= 7; z++) {
                // Slope ground: blocks up to 63
                terrain.createFullCell(new BlockPos(x, 62, z), GranularMaterialRegistry.DIRT);
                terrain.createFullCell(new BlockPos(x, 63, z), GranularMaterialRegistry.DIRT);
            }
        }

        // Execute blade tick
        BulldozerBladeController.BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                prevTransform,
                currTransform,
                0,
                GranularMaterial.EMPTY,
                HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
        );

        assertTrue(result.unitsExcavated() > 0, "Blade must cut and excavate material on the ramp");
        assertTrue(result.carriedUnitsAfter() > 0, "Carried units must accumulate surcharge on the moldboard");
        assertEquals(GranularMaterialRegistry.DIRT, result.carriedMaterialAfter(), "Material carried must be DIRT");
        assertTrue(result.isPushing(), "Bulldozer must register active pushing");
    }

    @Test
    @DisplayName("Plunging deeper on a slope cuts deeper than a shallow blade")
    void deeperPlungeCutsMoreMaterial() {
        float pitch = 15.0F;
        double armLength = HeavyBulldozerEntity.HEAVY_ARM_LENGTH;
        double bladeWidth = HeavyBulldozerEntity.HEAVY_BLADE_WIDTH;
        double bladeHeightDim = HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM;

        // Shallow blade (-0.10m)
        BladeTransform shallowPrev = BladeTransform.compute(
                new Vec3(0, 64, 0),
                0.0F, pitch, 0.0F, -0.10F, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );
        BladeTransform shallowCurr = BladeTransform.compute(
                new Vec3(0, 63.9, 0.4),
                0.0F, pitch, 0.0F, -0.10F, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );

        // Deep plunged blade (-0.90m)
        BladeTransform deepPrev = BladeTransform.compute(
                new Vec3(0, 64, 0),
                0.0F, pitch, 0.0F, -0.90F, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );
        BladeTransform deepCurr = BladeTransform.compute(
                new Vec3(0, 63.9, 0.4),
                0.0F, pitch, 0.0F, -0.90F, 0.0F,
                bladeWidth, bladeHeightDim, armLength, 17
        );

        assertTrue(deepPrev.cuttingEdgeY() < shallowPrev.cuttingEdgeY() - 0.70D,
                "Deep plunged blade must be significantly lower than shallow blade");
    }
}
