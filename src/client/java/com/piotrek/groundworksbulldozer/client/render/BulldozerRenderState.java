package com.piotrek.groundworksbulldozer.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Client-side render state for the tracked bulldozer.
 */
public class BulldozerRenderState extends EntityRenderState {

    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    public float bladeHeight;
    public float bladeAngle;

    public float leftTrackSpeed;
    public float rightTrackSpeed;
    public float leftTrackTravel;
    public float rightTrackTravel;

    public int carriedMaterialId;
    public int carriedUnits;
    public float fillRatio;

    public boolean isPushing;
    public boolean isEngineRunning;

    public float beaconSpin;
    public boolean beaconFlash;
}
