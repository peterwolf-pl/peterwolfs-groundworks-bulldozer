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

class ChunkBoundaryPushingTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Bulldozer pushes and grades material across chunk boundaries (e.g. X=15 -> X=16, Z=15 -> Z=16)")
    void testChunkBoundaryPushing() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // Place a pile of gravel right at the chunk border: X=14..15 (Chunk [0,0]), extending to X=16..17 (Chunk [1,0])
        BlockPos borderChunk0 = new BlockPos(15, 65, 15);
        BlockPos borderChunk1 = new BlockPos(16, 65, 15);

        terrain.createPile(borderChunk0, GranularMaterialRegistry.GRAVEL, 6);
        terrain.createPile(borderChunk1, GranularMaterialRegistry.GRAVEL, 6);

        int initialUnits = terrain.countTotalWorldUnits();
        assertTrue(initialUnits > 0);

        // Position bulldozer in chunk 0 (facing east along +X direction, yaw = 270 deg)
        // Driving east across X=14 -> X=17
        float yawEast = 270.0F; // Facing +X
        float bladeHeight = -0.20F;

        Vec3 posA = new Vec3(13.0D, 65.0D, 15.5D);
        Vec3 posB = new Vec3(14.5D, 65.0D, 15.5D);

        BladeTransform transformA = BladeTransform.compute(posA, yawEast, 0.0F, 0.0F, bladeHeight, 0.0F);
        BladeTransform transformB = BladeTransform.compute(posB, yawEast, 0.0F, 0.0F, bladeHeight, 0.0F);

        BladeTickResult step1 = BulldozerBladeController.tick(
                terrain,
                transformA,
                transformB,
                0,
                GranularMaterial.EMPTY
        );

        assertTrue(step1.unitsExcavated() > 0, "Must excavate material across chunk border");

        // Continue driving across into Chunk 1
        Vec3 posC = new Vec3(16.0D, 65.0D, 15.5D);
        BladeTransform transformC = BladeTransform.compute(posC, yawEast, 0.0F, 0.0F, bladeHeight, 0.0F);

        BladeTickResult step2 = BulldozerBladeController.tick(
                terrain,
                transformB,
                transformC,
                step1.carriedUnitsAfter(),
                step1.carriedMaterialAfter()
        );

        // Verify total system conservation across chunk boundaries
        int worldUnitsAfter = terrain.countTotalWorldUnits();
        int bladeUnitsAfter = step2.carriedUnitsAfter();

        assertEquals(initialUnits, worldUnitsAfter + bladeUnitsAfter,
                "Material volume must be strictly conserved when pushing across chunk boundaries");
    }
}
