package com.piotrek.groundworksbulldozer.integration.groundworks;

import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;

/**
 * Testable machine-facing subset of the public Groundworks grading API.
 */
public interface IGranularTerrainAccess {

    boolean isDiggable(BlockPos pos);

    GranularMaterial getMaterial(BlockPos pos);

    int excavateMicrovoxelsAbove(BlockPos pos, double worldCutY, int maxUnits);

    int fillDepressionBelow(
            BlockPos pos,
            double targetWorldY,
            GranularMaterial material,
            int availableUnits
    );

    int deposit(BlockPos pos, GranularMaterial material, int units);

    void markSimulate(BlockPos pos);
}
