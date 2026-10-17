package com.piotrek.groundworksbulldozer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.client.GroundworksBulldozerClient;
import com.piotrek.groundworksbulldozer.client.model.HeavyBulldozerModel;
import com.piotrek.groundworksbulldozer.client.model.SpoilHeapModel;
import com.piotrek.groundworksbulldozer.entity.HeavyBulldozerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.3 entity renderer for the heavy mining crawler bulldozer with triangular elevated tracks.
 */
public class HeavyBulldozerRenderer extends EntityRenderer<HeavyBulldozerEntity, HeavyBulldozerRenderState> {

    public static final Identifier TEXTURE = GroundworksBulldozerMod.id("textures/entity/heavy_bulldozer.png");

    private final HeavyBulldozerModel model;
    private final SpoilHeapModel spoil;
    private final Map<HeavyBulldozerEntity, TrackAnimationState> trackAnimations = new WeakHashMap<>();

    private static final class TrackAnimationState {
        float lastRenderTick = Float.NaN;
        float leftTravel;
        float rightTravel;
    }

    public HeavyBulldozerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HeavyBulldozerModel(context.bakeLayer(GroundworksBulldozerClient.HEAVY_BULLDOZER_LAYER));
        this.spoil = SpoilHeapModel.heavy();
        this.shadowRadius = 3.6F;
    }

    @Override
    public HeavyBulldozerRenderState createRenderState() {
        return new HeavyBulldozerRenderState();
    }

    @Override
    public void extractRenderState(HeavyBulldozerEntity entity, HeavyBulldozerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot(partialTick);
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.bladeHeight = entity.getBladeHeight();
        state.bladeAngle = entity.getBladeAngle();

        state.leftTrackSpeed = entity.getTrackLeftSpeed();
        state.rightTrackSpeed = entity.getTrackRightSpeed();

        TrackAnimationState trackAnimation =
                this.trackAnimations.computeIfAbsent(entity, ignored -> new TrackAnimationState());
        float renderTick = entity.tickCount + partialTick;
        if (Float.isNaN(trackAnimation.lastRenderTick)) {
            trackAnimation.lastRenderTick = renderTick;
        } else {
            float deltaTicks = Math.max(0.0F, Math.min(5.0F, renderTick - trackAnimation.lastRenderTick));
            trackAnimation.leftTravel += state.leftTrackSpeed * 16.0F * deltaTicks;
            trackAnimation.rightTravel += state.rightTrackSpeed * 16.0F * deltaTicks;
            trackAnimation.leftTravel = wrapTrackTravel(trackAnimation.leftTravel);
            trackAnimation.rightTravel = wrapTrackTravel(trackAnimation.rightTravel);
            trackAnimation.lastRenderTick = renderTick;
        }
        state.leftTrackTravel = trackAnimation.leftTravel;
        state.rightTrackTravel = trackAnimation.rightTravel;

        state.carriedMaterialId = entity.getCarriedMaterialId();
        state.carriedUnits = entity.getCarriedUnits();
        state.fillRatio = (float) entity.getCarriedUnits() / (float) entity.getMaxBladeCapacity();

        state.isPushing = entity.isPushing();
        state.isEngineRunning = entity.isEngineRunning();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);

        state.ripperLift = entity.getRipperLift();
    }

    private static float wrapTrackTravel(float travel) {
        float loop = HeavyBulldozerModel.TRACK_LOOP_LENGTH;
        if (travel > loop || travel < -loop) {
            travel %= loop;
        }
        return travel;
    }

    @Override
    public void submit(HeavyBulldozerRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        stack.pushPose();

        // Rotate heading (facing where tracks drive)
        stack.mulPose(Axis.YP.rotationDegrees(-state.baseYaw));

        // Apply ground pitch and roll
        if (Math.abs(state.basePitch) > 0.01F) {
            stack.mulPose(Axis.XP.rotationDegrees(state.basePitch));
        }
        if (Math.abs(state.baseRoll) > 0.01F) {
            stack.mulPose(Axis.ZP.rotationDegrees(state.baseRoll));
        }

        // Standard Minecraft entity model coordinate transform
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);

        collector.submitModel(
                this.model,
                state,
                stack,
                RenderTypes.entityTranslucent(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
        );
        if (state.carriedUnits > 0) {
            this.spoil.setupAnim(state);
            collector.submitModel(
                    this.spoil,
                    state,
                    stack,
                    RenderTypes.entityCutout(SpoilHeapModel.texture(state.carriedMaterialId)),
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor,
                null
            );
        }

        stack.popPose();
    }
}
