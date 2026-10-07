package com.piotrek.groundworksbulldozer.vehicle;

/** Uses the same two-layer diesel mix as the Groundworks excavator. */
public final class EngineSoundProfile {

    private static final float IDLE_VOLUME = 0.42F;
    private static final float DRIVE_VOLUME = 0.64F;
    private static final float IDLE_PITCH = 0.94F;
    private static final float DRIVE_PITCH = 1.12F;
    private static final float LOAD_LAYER_VOLUME = 0.86F;

    private EngineSoundProfile() {}

    public record Mix(float volume, float pitch, float loadVolume, float loadPitch) {}

    public static Mix forMachineLoad(
            float machineLoad,
            float leftSpeed,
            float rightSpeed
    ) {
        float speed = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
        float driveLoad = Math.min(
                1.0F,
                speed / (float) BulldozerTrackController.MAX_SPEED
        );
        float bladeLoad = Math.clamp(machineLoad, 0.0F, 1.0F);
        float load = Math.max(bladeLoad, driveLoad * 0.78F);
        float lug = bladeLoad * (1.0F - driveLoad);

        return new Mix(
                IDLE_VOLUME + (DRIVE_VOLUME - IDLE_VOLUME) * load,
                IDLE_PITCH + (DRIVE_PITCH - IDLE_PITCH) * load,
                LOAD_LAYER_VOLUME * load,
                0.88F + 0.16F * driveLoad - 0.06F * lug
        );
    }

    public static Mix forTrackSpeeds(float leftSpeed, float rightSpeed) {
        return forMachineLoad(0.0F, leftSpeed, rightSpeed);
    }
}
