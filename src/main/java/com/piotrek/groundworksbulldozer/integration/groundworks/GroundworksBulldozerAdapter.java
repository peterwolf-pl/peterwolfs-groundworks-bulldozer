package com.piotrek.groundworksbulldozer.integration.groundworks;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.deposit.DepositResult;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworks.api.material.GranularMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Thin server-side bridge from the Bulldozer to the public Groundworks API.
 *
 * <p>The Groundworks core owns lazy conversion, microvoxel mutation,
 * synchronization, dirty flags, block entities, and relaxation scheduling.
 */
public class GroundworksBulldozerAdapter implements IGranularTerrainAccess {

    private final ServerLevel level;

    public GroundworksBulldozerAdapter(ServerLevel level) {
        this.level = level;
    }

    public static GroundworksBulldozerAdapter of(ServerLevel level) {
        return new GroundworksBulldozerAdapter(level);
    }

    @Override
    public boolean isDiggable(BlockPos pos) {
        return GroundworksApi.isDiggable(level, pos);
    }

    @Override
    public GranularMaterial getMaterial(BlockPos pos) {
        GranularMaterial material = GroundworksApi.getMaterial(level, pos);
        return material != null ? material : GranularMaterial.EMPTY;
    }

    @Override
    public ExcavationResult excavateMicrovoxelsAbove(
            BlockPos pos,
            double worldCutY,
            int maxUnits,
            GranularMaterial requiredMaterial
    ) {
        if (maxUnits <= 0) {
            return ExcavationResult.NONE;
        }
        return GroundworksApi.excavateAbove(
                level,
                pos,
                worldCutY,
                maxUnits,
                requiredMaterial
        );
    }

    @Override
    public int fillDepressionBelow(
            BlockPos pos,
            double targetWorldY,
            GranularMaterial material,
            int availableUnits
    ) {
        if (availableUnits <= 0 || material == null || material.id() == 0) {
            return 0;
        }
        DepositResult result =
                GroundworksApi.fillBelow(level, pos, targetWorldY, material, availableUnits);
        return result.unitsDeposited();
    }

    @Override
    public int deposit(BlockPos pos, GranularMaterial material, int units) {
        if (units <= 0 || material == null || material.id() == 0) {
            return 0;
        }
        DepositResult result =
                GroundworksApi.depositWithOverflow(level, pos, material, units);
        return result.unitsDeposited();
    }

    @Override
    public void markSimulate(BlockPos pos) {
        GroundworksApi.markForSimulation(level, pos);
    }
}
