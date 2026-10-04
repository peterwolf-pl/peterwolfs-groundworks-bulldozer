package com.piotrek.groundworksbulldozer.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Encapsulated state record for serializing and deserializing bulldozer machine parameters.
 */
public record BulldozerState(
        float bladeHeight,
        float bladeAngle,
        int carriedUnits,
        int carriedMaterialId,
        float leftTrackSpeed,
        float rightTrackSpeed,
        float vehiclePitch,
        float vehicleRoll
) {

    public void save(ValueOutput output) {
        output.putFloat("BladeHeight", bladeHeight);
        output.putFloat("BladeAngle", bladeAngle);
        output.putInt("CarriedUnits", carriedUnits);
        output.putInt("CarriedMaterialId", carriedMaterialId);
        output.putFloat("LeftTrackSpeed", leftTrackSpeed);
        output.putFloat("RightTrackSpeed", rightTrackSpeed);
        output.putFloat("VehiclePitch", vehiclePitch);
        output.putFloat("VehicleRoll", vehicleRoll);
    }

    public static BulldozerState load(ValueInput input) {
        return new BulldozerState(
                input.getFloatOr("BladeHeight", GroundworksBulldozerEntity.DEFAULT_BLADE_HEIGHT),
                input.getFloatOr("BladeAngle", 0.0F),
                input.getIntOr("CarriedUnits", 0),
                input.getIntOr("CarriedMaterialId", 0),
                input.getFloatOr("LeftTrackSpeed", 0.0F),
                input.getFloatOr("RightTrackSpeed", 0.0F),
                input.getFloatOr("VehiclePitch", 0.0F),
                input.getFloatOr("VehicleRoll", 0.0F)
        );
    }
}
