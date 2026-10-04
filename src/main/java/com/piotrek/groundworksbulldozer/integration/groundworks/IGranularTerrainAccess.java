package com.piotrek.groundworksbulldozer.integration.groundworks;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import net.minecraft.core.BlockPos;

/**
 * Common abstraction for granular terrain access and grading operations.
 * Allows pure unit testing without a running server level while providing
 * direct integration with Peterwolf's Groundworks on live worlds.
 */
public interface IGranularTerrainAccess {

    /**
     * Checks if the position has convertible block state or existing granular terrain.
     */
    boolean isDiggable(BlockPos pos);

    /**
     * Returns the granular cell if converted, or null.
     */
    GranularCell getCell(BlockPos pos);

    /**
     * Returns the cell or converts the vanilla block (dirt/sand/gravel) to a full granular cell.
     */
    GranularCell getOrConvert(BlockPos pos);

    /**
     * Shaves microvoxels at or above worldCutY from the specified cell.
     *
     * @param pos         Block position
     * @param worldCutY   Absolute world Y level of the blade cutting edge
     * @param maxUnits    Maximum units to shave off
     * @return Number of units removed
     */
    int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits);

    /**
     * Fills depressions below targetWorldY in the specified cell with the given material.
     *
     * @param pos            Block position
     * @param targetWorldY   Target world Y grade level to fill up to
     * @param material       Granular material to deposit
     * @param availableUnits Maximum units available in the blade
     * @return Number of units deposited into the cell
     */
    int fillDepressionBelow(BlockPos pos, double targetWorldY, GranularMaterial material, int availableUnits);

    /**
     * Deposits units at the position with upward overflow.
     *
     * @return Number of units actually deposited
     */
    int deposit(BlockPos pos, GranularMaterial material, int units);

    /**
     * Marks the position for physics simulation and relaxation.
     */
    void markSimulate(BlockPos pos);
}
