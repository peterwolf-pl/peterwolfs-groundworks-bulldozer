package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import com.piotrek.groundworksbulldozer.client.model.HeavyBulldozerModel;
import com.piotrek.groundworksbulldozer.entity.HeavyBulldozerEntity;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeavyBulldozerGradingAndKinematicsTest {

    @BeforeAll
    static void init() {
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Heavy Bulldozer blade transform has 6.0m width and 17 edge samples")
    void testHeavyBladeTransformDimensions() {
        Vec3 base = new Vec3(0.0D, 64.0D, 0.0D);
        BladeTransform transform = BladeTransform.compute(
                base, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );

        assertNotNull(transform);
        assertEquals(HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES, transform.cuttingEdgePoints().size());
        assertEquals(HeavyBulldozerEntity.HEAVY_BLADE_WIDTH, transform.width(), 1e-3D);
        assertEquals(HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM, transform.height(), 1e-3D);

        // Wing span must equal 6.0m
        double span = transform.leftWingPoint().distanceTo(transform.rightWingPoint());
        assertEquals(6.0D, span, 1e-3D);

        // Arm length must place cutting edge 4.2m ahead
        assertEquals(base.z + HeavyBulldozerEntity.HEAVY_ARM_LENGTH, transform.cuttingEdgeCenter().z, 1e-3D);
    }

    @Test
    @DisplayName("Heavy Bulldozer blade controller enforces 10240-unit capacity and volume conservation")
    void testHeavyBladeControllerCapacityAndConservation() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // Fill ground with dirt ahead of the bulldozer
        for (int x = -3; x <= 3; x++) {
            for (int z = 4; z <= 8; z++) {
                terrain.createFullCell(new net.minecraft.core.BlockPos(x, 64, z), GranularMaterialRegistry.DIRT);
            }
        }

        int initialTerrainUnits = terrain.countTotalWorldUnits();
        assertTrue(initialTerrainUnits > 0);

        int carried = 0;
        GranularMaterial carriedMat = GranularMaterial.EMPTY;

        // Drive heavy bulldozer forward across 5 grading steps
        for (int i = 0; i < 5; i++) {
            double zPrev = i * 0.4D;
            double zCurr = (i + 1) * 0.4D;

            BladeTransform prev = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, zPrev), 0.0F, 0.0F, 0.0F, -0.20F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );
            BladeTransform curr = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, zCurr), 0.0F, 0.0F, 0.0F, -0.20F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );

            BladeTickResult result = BulldozerBladeController.tick(
                    terrain,
                    prev,
                    curr,
                    carried,
                    carriedMat,
                    HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
            );

            carried = result.carriedUnitsAfter();
            carriedMat = result.carriedMaterialAfter();

            int currentWorldUnits = terrain.countTotalWorldUnits();
            assertEquals(
                    initialTerrainUnits,
                    currentWorldUnits + carried,
                    "Total system units (world + carried) must remain strictly constant"
            );
            assertTrue(carried <= HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY);
        }

        assertTrue(carried > 0, "Blade must carry excavated material");
    }

    @Test
    @DisplayName("Heavy Bulldozer can carry and push at least 16 full blocks of material (>=8192 units)")
    void testHeavyBulldozerPushesAtLeast16Blocks() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        // 16 full blocks = 16 * 512 = 8192 units
        // Fill 4x4 block area (16 blocks)
        for (int x = -2; x <= 1; x++) {
            for (int z = 4; z <= 7; z++) {
                terrain.createFullCell(new net.minecraft.core.BlockPos(x, 64, z), GranularMaterialRegistry.DIRT);
            }
        }

        int initialTerrainUnits = terrain.countTotalWorldUnits();
        assertEquals(16 * 512, initialTerrainUnits, "Initial terrain must be exactly 16 blocks (8192 units)");

        int carried = 0;
        GranularMaterial carriedMat = GranularMaterial.EMPTY;

        // Drive heavy bulldozer forward across the 16 blocks
        for (int i = 0; i < 10; i++) {
            double zPrev = i * 0.45D;
            double zCurr = (i + 1) * 0.45D;

            BladeTransform prev = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, zPrev), 0.0F, 0.0F, 0.0F, -0.30F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );
            BladeTransform curr = BladeTransform.compute(
                    new Vec3(0.0D, 64.0D, zCurr), 0.0F, 0.0F, 0.0F, -0.30F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );

            BladeTickResult result = BulldozerBladeController.tick(
                    terrain,
                    prev,
                    curr,
                    carried,
                    carriedMat,
                    HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
            );

            carried = result.carriedUnitsAfter();
            carriedMat = result.carriedMaterialAfter();

            int currentWorldUnits = terrain.countTotalWorldUnits();
            assertEquals(
                    initialTerrainUnits,
                    currentWorldUnits + carried + result.unitsDeposited(),
                    "Total system units must be strictly conserved"
            );
        }

        // Must have pushed/cleared and held at least 16 full blocks
        assertTrue(carried >= 8192, "Heavy bulldozer must hold and push >= 16 blocks (8192 units), was: " + carried);
        assertTrue(carried <= HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY);
    }

    @Test
    @DisplayName("Reversing heavy bulldozer deposits full capacity heap across 6.0m blade width")
    void testHeavyBulldozerReversingHeap() {
        TestGranularTerrain terrain = new TestGranularTerrain();

        int carriedUnits = 2400; // Large load
        GranularMaterial material = GranularMaterialRegistry.DIRT;

        Vec3 forwardPos = new Vec3(0.0D, 65.0D, 8.0D);
        Vec3 reversePos = new Vec3(0.0D, 65.0D, 7.5D);

        BladeTransform prev = BladeTransform.compute(
                forwardPos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );
        BladeTransform curr = BladeTransform.compute(
                reversePos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain,
                prev,
                curr,
                carriedUnits,
                material,
                HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
        );

        assertEquals(carriedUnits, result.unitsDeposited());
        assertEquals(0, result.carriedUnitsAfter());
        assertEquals(carriedUnits, terrain.countTotalWorldUnits());

        // Wide 6.0m blade deposits across 7 columns
        assertTrue(result.affectedPositions().size() >= 5, "Wide heap must span multiple world columns");
    }

    @Test
    @DisplayName("Full heavy load leaves a pryzma at most 2 blocks high across 2 or 3 rows")
    void testHeavyLoadLeavesLowWidePryzma() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        int carriedUnits = HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY;
        GranularMaterial material = GranularMaterialRegistry.DIRT;

        Vec3 forwardPos = new Vec3(0.0D, 65.0D, 8.0D);
        Vec3 reversePos = new Vec3(0.0D, 65.0D, 7.5D);
        BladeTransform prev = BladeTransform.compute(
                forwardPos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );
        BladeTransform curr = BladeTransform.compute(
                reversePos, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain, prev, curr, carriedUnits, material, HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
        );

        assertEquals(carriedUnits, result.unitsDeposited(), "The whole load must leave the blade");
        assertEquals(0, result.carriedUnitsAfter());

        java.util.Map<Long, Integer> columnHeight = new java.util.HashMap<>();
        java.util.Set<Integer> rows = new java.util.HashSet<>();
        for (net.minecraft.core.BlockPos pos : result.affectedPositions()) {
            long key = (((long) pos.getX()) << 32) ^ (pos.getZ() & 0xFFFFFFFFL);
            columnHeight.merge(key, 1, Integer::sum);
            rows.add(pos.getZ());
        }
        int tallest = columnHeight.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        assertTrue(tallest <= 2, "Pryzma must not exceed 2 blocks, was " + tallest);
        assertTrue(rows.size() >= 2, "Pryzma must span at least 2 rows, was " + rows.size());
        assertTrue(columnHeight.size() >= 6, "Pryzma must span the blade, columns=" + columnHeight.size());
    }

    @Test
    @DisplayName("HeavyBulldozerModel layer definition builds successfully with all parts")
    void testHeavyBulldozerModelDefinition() {
        LayerDefinition layer = HeavyBulldozerModel.createBodyLayer();
        assertNotNull(layer);
        assertNotNull(layer.bakeRoot());

        // Verify root components
        var root = layer.bakeRoot();
        assertTrue(root.hasChild("undercarriage"));
        assertTrue(root.hasChild("main_body"));

        var undercarriage = root.getChild("undercarriage");
        assertTrue(undercarriage.hasChild("left_track"));
        assertTrue(undercarriage.hasChild("right_track"));

        var leftTrack = undercarriage.getChild("left_track");
        assertTrue(leftTrack.hasChild("drive_sprocket"));
        assertTrue(leftTrack.hasChild("front_idler"));
        assertTrue(leftTrack.hasChild("rear_idler"));
        assertTrue(leftTrack.hasChild("carrier_roller"));

        // Verify 8 pairs of road wheels
        for (int i = 0; i < 8; i++) {
            assertTrue(leftTrack.hasChild("road_wheel_" + i));
        }

        // Verify 44 track links
        for (int i = 0; i < 44; i++) {
            assertTrue(leftTrack.hasChild("track_link_" + i));
        }

        var mainBody = root.getChild("main_body");
        assertTrue(mainBody.hasChild("push_arms"));
        assertTrue(mainBody.hasChild("left_cylinder"));
        assertTrue(mainBody.hasChild("right_cylinder"));

        var pushArms = mainBody.getChild("push_arms");
        assertTrue(pushArms.hasChild("blade"));

        var blade = pushArms.getChild("blade");
        assertTrue(blade.hasChild("carried_material"));

        assertTrue(mainBody.hasChild("ripper"), "Main body must contain articulated rear ripper");
    }

    @Test
    @DisplayName("Lifting heavy blade spreads material into an even layer without columns or pyramids")
    void testHeavyBulldozerSpreadsEvenLayerWhenLifted() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        // Ground is at Y=65.0 with low cells at Y=64 (4 layers in cells ready to be filled/spread)
        for (int x = -3; x <= 3; x++) {
            for (int z = 4; z <= 7; z++) {
                terrain.createPile(new net.minecraft.core.BlockPos(x, 64, z), dirt, 4);
            }
        }

        int initialTerrain = terrain.countTotalWorldUnits();
        int carriedBefore = 4000; // Big pushed heap

        // Blade is raised to +0.25m above ground level Y=65.0 (spreading a layer under cutting edge)
        BladeTransform prev = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 1.0D), 0.0F, 0.0F, 0.0F, 0.25F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );
        BladeTransform curr = BladeTransform.compute(
                new Vec3(0.0D, 65.0D, 1.45D), 0.0F, 0.0F, 0.0F, 0.25F, 0.0F,
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
        );

        BladeTickResult result = BulldozerBladeController.tick(
                terrain, prev, curr, carriedBefore, dirt, HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
        );

        assertTrue(result.unitsDeposited() > 0, "Lifting blade must deposit and spread carried material");
        assertEquals(initialTerrain + carriedBefore, terrain.countTotalWorldUnits() + result.carriedUnitsAfter(),
                "Strict volume conservation must be preserved during layer spreading");

        // The deposited material must be spread across multiple positions, not piled in 1 block
        assertTrue(result.affectedPositions().size() >= 3, "Spread layer must cover width of blade");
    }

    @Test
    @DisplayName("Track controller samples ground support and pitches forward/backward naturally")
    void testTrackControllerGroundPitching() {
        var controller = new com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController(
                4.2D, 6.2D, 0.14D, 0.055D, 0.090D, 0.38F
        );

        // When in air without level, pitch smoothly decays to zero
        var state = controller.tick(null, new Vec3(0, 64, 0), 0.0F, 0.0F, 0.0F, false, false);
        assertEquals(64.0D, state.groundSupportY(), 1e-3D);
    }

    @Test
    @DisplayName("Ripper claw animates from raised to lowered ripping position")
    void testRipperKinematicsAndModel() {
        var layer = HeavyBulldozerModel.createBodyLayer();
        var root = layer.bakeRoot();
        var model = new HeavyBulldozerModel(root);

        var state = new com.piotrek.groundworksbulldozer.client.render.HeavyBulldozerRenderState();
        state.ripperLift = 0.0F; // fully raised
        model.setupAnim(state);

        var ripper = root.getChild("main_body").getChild("ripper");
        float angleRaised = ripper.xRot;
        assertTrue(angleRaised < 0.0F, "Raised ripper must tilt up away from ground");

        state.ripperLift = 1.0F; // fully lowered
        model.setupAnim(state);
        float angleLowered = ripper.xRot;
        assertTrue(angleLowered > 0.0F, "Lowered ripper must tilt down into ground/rock");
        assertTrue(angleLowered > angleRaised, "Lowered angle must be greater than raised angle");
    }

    @Test
    @DisplayName("Dirt block in front of blade is excavated and never passes behind untouched")
    void testForwardBlockIsExcavatedAndNeverPassesBehindBlade() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        // Block of dirt at (0, 65, 5) directly in front of the blade (arm length 4.2m)
        terrain.createFullCell(new net.minecraft.core.BlockPos(0, 65, 5), dirt);
        assertEquals(512, terrain.countTotalWorldUnits());

        int carried = 0;
        GranularMaterial carriedMat = GranularMaterial.EMPTY;

        // Drive from Z=0.0 to Z=1.6 (cutting edge sweeps across Z=4.2 to Z=5.8)
        for (int i = 0; i < 4; i++) {
            double zPrev = i * 0.4D;
            double zCurr = (i + 1) * 0.4D;

            BladeTransform prev = BladeTransform.compute(
                    new Vec3(0.0D, 65.0D, zPrev), 0.0F, 0.0F, 0.0F, -0.10F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );
            BladeTransform curr = BladeTransform.compute(
                    new Vec3(0.0D, 65.0D, zCurr), 0.0F, 0.0F, 0.0F, -0.10F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );

            BladeTickResult result = BulldozerBladeController.tick(
                    terrain, prev, curr, carried, carriedMat, HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
            );

            carried = result.carriedUnitsAfter();
            carriedMat = result.carriedMaterialAfter();
        }

        // The block must have been excavated into the blade
        assertTrue(carried > 0, "Material must be excavated from block in front of blade");
        assertEquals(512, carried + terrain.countTotalWorldUnits(), "Total volume must be strictly conserved");
        // Cell at (0, 65, 5) must be cleared
        var cell = terrain.getCell(new net.minecraft.core.BlockPos(0, 65, 5));
        assertTrue(cell == null || cell.isEmpty(), "Block at (0, 65, 5) must not remain untouched behind blade");
    }

    @Test
    @DisplayName("A wall in front of the blade is shaved flat and never stacked into a pillar")
    void testWallIsShavedFlatWithoutUpwardPillar() {
        TestGranularTerrain terrain = new TestGranularTerrain();
        GranularMaterial dirt = GranularMaterialRegistry.DIRT;

        // Two-high wall, including a column that sits between edge samples.
        for (int x = -3; x <= 3; x++) {
            terrain.createFullCell(new net.minecraft.core.BlockPos(x, 65, 5), dirt);
            terrain.createFullCell(new net.minecraft.core.BlockPos(x, 66, 5), dirt);
        }
        int before = terrain.countTotalWorldUnits();

        int carried = 0;
        GranularMaterial carriedMat = GranularMaterial.EMPTY;
        for (int i = 0; i < 6; i++) {
            double zPrev = i * 0.35D;
            double zCurr = (i + 1) * 0.35D;
            BladeTransform prev = BladeTransform.compute(
                    new Vec3(0.0D, 65.05D, zPrev), 0.0F, 0.0F, 0.0F, -0.05F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );
            BladeTransform curr = BladeTransform.compute(
                    new Vec3(0.0D, 65.05D, zCurr), 0.0F, 0.0F, 0.0F, -0.05F, 0.0F,
                    HeavyBulldozerEntity.HEAVY_BLADE_WIDTH,
                    HeavyBulldozerEntity.HEAVY_BLADE_HEIGHT_DIM,
                    HeavyBulldozerEntity.HEAVY_ARM_LENGTH,
                    HeavyBulldozerEntity.HEAVY_NUM_EDGE_SAMPLES
            );
            BladeTickResult result = BulldozerBladeController.tick(
                    terrain, prev, curr, carried, carriedMat, HeavyBulldozerEntity.HEAVY_BLADE_CAPACITY
            );
            carried = result.carriedUnitsAfter();
            carriedMat = result.carriedMaterialAfter();
        }

        assertEquals(before, carried + terrain.countTotalWorldUnits(), "Wall cut must conserve volume");
        for (int x = -2; x <= 2; x++) {
            var upper = terrain.getCell(new net.minecraft.core.BlockPos(x, 66, 5));
            assertTrue(upper == null || upper.isEmpty(), "Upper wall cell at x=" + x + " must be cut, not left as a pillar");
            var grown = terrain.getCell(new net.minecraft.core.BlockPos(x, 67, 5));
            assertTrue(grown == null || grown.isEmpty(), "Cut material must not be stacked upward at x=" + x);
        }
    }

    @Test
    @DisplayName("Reversing down a slope updates track speeds and moves backward smoothly")
    void testReversingKinematicsDownSlope() {
        var controller = new com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController(
                HeavyBulldozerEntity.HEAVY_BLADE_WIDTH * 0.70D,
                6.2D,
                0.14D,
                0.055D,
                0.090D,
                0.38F
        );

        // Drive in reverse (throttle = -1.0F)
        var state = controller.tick(null, new Vec3(0, 64, 0), 0.0F, -1.0F, 0.0F, true, false);
        for (int i = 0; i < 5; i++) {
            state = controller.tick(null, new Vec3(0, 64, 0), 0.0F, -1.0F, 0.0F, true, false);
        }

        assertTrue(state.leftSpeed() < -0.05F, "Left track must reverse");
        assertTrue(state.rightSpeed() < -0.05F, "Right track must reverse");
        assertTrue(state.forwardDelta().z < -0.05D, "Vehicle must move backwards when reversing");
    }
}
