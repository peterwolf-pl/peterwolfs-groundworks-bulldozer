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

    void markSimulate(BlockPos pos);
}
