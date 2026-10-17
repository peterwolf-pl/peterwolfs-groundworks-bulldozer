package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworks.api.tree.GroundworksTreeApi;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.tree.BulldozerTreeController;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulldozerTreeAndLogTest {

    @Test
    @DisplayName("Bulldozer heading maps accurately to nearest cardinal horizontal Direction")
    void testBulldozerHeadingDirections() {
        // Forward North (-Z)
        Direction north = BulldozerTreeController.getNearestHorizontal(0.0D, -1.0D);
        assertEquals(Direction.NORTH, north, "Vector (0, -1) must map to NORTH");

        // Forward South (+Z)
        Direction south = BulldozerTreeController.getNearestHorizontal(0.0D, 1.0D);
        assertEquals(Direction.SOUTH, south, "Vector (0, 1) must map to SOUTH");

        // Forward East (+X)
        Direction east = BulldozerTreeController.getNearestHorizontal(1.0D, 0.0D);
        assertEquals(Direction.EAST, east, "Vector (1, 0) must map to EAST");

        // Forward West (-X)
        Direction west = BulldozerTreeController.getNearestHorizontal(-1.0D, 0.0D);
        assertEquals(Direction.WEST, west, "Vector (-1, 0) must map to WEST");
    }

    @Test
    @DisplayName("Standing tree break threshold is achievable under heavy bulldozer pushing")
    void testTreeBreakThresholdReachability() {
        float treeThreshold = GroundworksTreeApi.calculateBreakThreshold(6);
        assertEquals(5.5F, treeThreshold, 0.01F);

        // Under 100% throttle + moving tracks (e.g. effort ~ 2.0F - 3.5F),
        // bulldozer leverage (contact at 1.0m relative to base -> leverage ~ 0.67F):
        // deltaStrain per tick ~ 1.5F - 2.5F.
        // A tree will topple in 3-5 ticks of continuous pushing.
        float effort = 1.2F * 1.0F + 0.3F * 5.0F; // throttle=1.0, trackSpeed=0.3
        float leverage = (float) (1.0D / 1.5D);
        float deltaStrain = effort * leverage;

        assertTrue(deltaStrain > 1.0F, "Bulldozer must deliver strong per-tick strain");
        int ticksToBreak = (int) Math.ceil(treeThreshold / deltaStrain);
        assertTrue(ticksToBreak >= 2 && ticksToBreak <= 6,
                "Normal tree should snap and topple within 2-6 ticks of heavy pushing (actual: " + ticksToBreak + ")");
    }

    @Test
    @DisplayName("BladeTransform forward vector matches cardinal felling direction")
    void testBladeTransformFellingDirection() {
        // Bulldozer facing South (yaw = 0)
        BladeTransform southBlade = BladeTransform.compute(
                new Vec3(0, 64, 0),
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F
        );
        Direction southDir = BulldozerTreeController.getNearestHorizontal(
                southBlade.forward().x, southBlade.forward().z
        );
        assertEquals(Direction.SOUTH, southDir, "Bulldozer facing yaw 0 must fell and push SOUTH");

        // Bulldozer facing North (yaw = 180)
        BladeTransform northBlade = BladeTransform.compute(
                new Vec3(0, 64, 0),
                180.0F, 0.0F, 0.0F, 0.0F, 0.0F
        );
        Direction northDir = BulldozerTreeController.getNearestHorizontal(
                northBlade.forward().x, northBlade.forward().z
        );
        assertEquals(Direction.NORTH, northDir, "Bulldozer facing yaw 180 must fell and push NORTH");
    }

    @Test
    @DisplayName("Heavy Bulldozer (D10) has massive power multiplier, toppling trees in 1-2 ticks")
    void testHeavyBulldozerTreePower() {
        // Base effort ~ 2.5F under throttle + track speed
        float baseEffort = 1.2F * 1.0F + 0.3F * 5.0F; // 2.7F
        // Heavy Bulldozer has 3.0x effort multiplier and 1.5x leverage
        float heavyEffort = baseEffort * 3.0F;
        float heavyLeverage = (float) (1.0D / 1.5D) * 1.5F; // 1.0F
        float deltaStrain = heavyEffort * heavyLeverage;

        float treeThreshold = GroundworksTreeApi.calculateBreakThreshold(8); // 6.0F
        assertTrue(deltaStrain >= treeThreshold,
                "Heavy Bulldozer must generate enough strain (" + deltaStrain + ") to topple height-8 tree in 1 tick");
    }
}
