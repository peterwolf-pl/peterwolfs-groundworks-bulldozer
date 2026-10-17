package com.piotrek.groundworksbulldozer.integration.groundworks;

import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;

/**
 * Testable machine-facing subset of the public Groundworks grading API.
 */
public interface IGranularTerrainAccess {

    boolean isDiggable(BlockPos pos);

    GranularMaterial getMaterial(BlockPos pos);

    ExcavationResult excavateMicrovoxelsAbove(
            BlockPos pos,
            double worldCutY,
            int maxUnits,
            GranularMaterial requiredMaterial
    );

    int fillDepressionBelow(
            BlockPos pos,
            double targetWorldY,
            GranularMaterial material,
            int availableUnits
    );

    int deposit(BlockPos pos, GranularMaterial material, int units);

    /**
     * Deposit into this cell only. Must not walk upward when the cell is full.
     * A full cell returns 0 so the caller can place the rest in the next row.
     */
    int depositInCell(BlockPos pos, GranularMaterial material, int units);

    void markSimulate(BlockPos pos);
}
