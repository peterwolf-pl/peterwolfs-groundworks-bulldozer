package com.piotrek.groundworksbulldozer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.client.GroundworksBulldozerClient;
import com.piotrek.groundworksbulldozer.client.model.BulldozerModel;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * 26.3 entity renderer for the tracked industrial bulldozer.
 */
public class BulldozerRenderer extends EntityRenderer<GroundworksBulldozerEntity, BulldozerRenderState> {

    public static final Identifier TEXTURE = GroundworksBulldozerMod.id("textures/entity/bulldozer.png");

    private final BulldozerModel model;

    public BulldozerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BulldozerModel(context.bakeLayer(GroundworksBulldozerClient.BULLDOZER_LAYER));
        this.shadowRadius = 1.8F;
    }

    @Override
    public BulldozerRenderState createRenderState() {
        return new BulldozerRenderState();
    }

    @Override
    public void extractRenderState(GroundworksBulldozerEntity entity, BulldozerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot();
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.bladeHeight = entity.getBladeHeight();
        state.bladeAngle = entity.getBladeAngle();

        state.leftTrackSpeed = entity.getTrackLeftSpeed();
        state.rightTrackSpeed = entity.getTrackRightSpeed();

        state.carriedMaterialId = entity.getCarriedMaterialId();
        state.carriedUnits = entity.getCarriedUnits();
        state.fillRatio = (float) entity.getCarriedUnits() / 768.0F;

        state.isPushing = entity.isPushing();
        state.isEngineRunning = entity.isEngineRunning();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    @Override
    public void submit(BulldozerRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        stack.pushPose();

        // Rotate heading (facing where tracks drive)
        stack.rotateDegrees(Axis.YP, -state.baseYaw);

        // Apply ground pitch and roll
        if (Math.abs(state.basePitch) > 0.01F) {
            stack.rotateDegrees(Axis.XP, state.basePitch);
        }
        if (Math.abs(state.baseRoll) > 0.01F) {
            stack.rotateDegrees(Axis.ZP, state.baseRoll);
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
                state.outlineColor
        );

        stack.popPose();
    }
}
