package com.piotrek.groundworksbulldozer.client.model;

import com.piotrek.groundworksbulldozer.client.render.BulldozerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometric hierarchy model for the medium tracked industrial bulldozer.
 *
 * <p>Features:
 * <ul>
 *   <li>Heavy crawler tracks with drive sprockets, front idlers, and bottom track rollers.</li>
 *   <li>Diesel engine compartment with ventilation louvers, exhaust stack, and air cleaner.</li>
 *   <li>Hollow ROPS safety cab with panoramic tinted safety glass and driver seat.</li>
 *   <li>Pivoting heavy C-frame push arms with animated hydraulic lift cylinders.</li>
 *   <li>Wide curved industrial moldboard blade with wear cutting lip and side spill wings.</li>
 *   <li>Dynamic carried material layer visible inside the blade during grading.</li>
 *   <li>Flashing rotary amber safety beacon on the cab roof.</li>
 * </ul>
 */
public class BulldozerModel extends EntityModel<BulldozerRenderState> {

    private final ModelPart undercarriage;
    private final ModelPart mainBody;
    private final ModelPart pushArms;
    private final ModelPart blade;
    private final ModelPart carriedMaterial;
    private final ModelPart leftCylinder;
    private final ModelPart rightCylinder;
    private final ModelPart beaconReflector;

    public BulldozerModel(ModelPart root) {
        super(root);
        this.undercarriage = root.getChild("undercarriage");
        this.mainBody = root.getChild("main_body");
        this.pushArms = this.mainBody.getChild("push_arms");
        this.blade = this.pushArms.getChild("blade");
        this.carriedMaterial = this.blade.getChild("carried_material");
        this.leftCylinder = this.mainBody.getChild("left_cylinder");
        this.rightCylinder = this.mainBody.getChild("right_cylinder");
        this.beaconReflector = this.mainBody.getChild("beacon_base").getChild("beacon_reflector");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 1. Undercarriage & Tracks (Ground level Y = 24 in model space) ──
        PartDefinition undercarriage = root.addOrReplaceChild(
                "undercarriage",
                CubeListBuilder.create()
                        // Central chassis belly plate & cross members: UV [144, 0]
                        .texOffs(144, 0).addBox(-10.0F, 12.0F, -18.0F, 20.0F, 6.0F, 36.0F)
                        // Rear drawbar / towing hitch
                        .texOffs(144, 0).addBox(-6.0F, 13.0F, -22.0F, 12.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Left track crawler assembly: UV [0, 0]
        undercarriage.addOrReplaceChild(
                "left_track",
                CubeListBuilder.create()
                        // Track belt loop: 8x12x50
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, -25.0F, 8.0F, 12.0F, 50.0F)
                        // Front idler wheel (tension wheel)
                        .texOffs(144, 0).addBox(-4.5F, -5.0F, 20.0F, 9.0F, 10.0F, 6.0F)
                        // Rear drive sprocket
                        .texOffs(144, 0).addBox(-4.5F, -5.0F, -26.0F, 9.0F, 10.0F, 6.0F)
                        // Bottom road track rollers
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, -16.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, -6.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, 4.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, 14.0F, 7.0F, 3.0F, 5.0F)
                        // Top carrier return roller
                        .texOffs(144, 0).addBox(-3.5F, -7.0F, -1.0F, 7.0F, 2.0F, 4.0F),
                PartPose.offset(-15.0F, 18.0F, 0.0F)
        );

        // Right track crawler assembly: UV [0, 0]
        undercarriage.addOrReplaceChild(
                "right_track",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -6.0F, -25.0F, 8.0F, 12.0F, 50.0F)
                        .texOffs(144, 0).addBox(-4.5F, -5.0F, 20.0F, 9.0F, 10.0F, 6.0F)
                        .texOffs(144, 0).addBox(-4.5F, -5.0F, -26.0F, 9.0F, 10.0F, 6.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, -16.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, -6.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, 4.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, 4.0F, 14.0F, 7.0F, 3.0F, 5.0F)
                        .texOffs(144, 0).addBox(-3.5F, -7.0F, -1.0F, 7.0F, 2.0F, 4.0F),
                PartPose.offset(15.0F, 18.0F, 0.0F)
        );

        // ── 2. Main Body (Engine Compartment & ROPS Cab) ─────────────────
        PartDefinition mainBody = root.addOrReplaceChild(
                "main_body",
                CubeListBuilder.create()
                        // Main engine hood (front): UV [0, 76]
                        .texOffs(0, 76).addBox(-10.0F, -1.0F, 0.0F, 20.0F, 13.0F, 24.0F)
                        // Front radiator steel grille & guard: UV [124, 76]
                        .texOffs(124, 76).addBox(-9.5F, 0.0F, 24.0F, 19.0F, 12.0F, 2.0F)
                        // Left mudguard walkway: UV [144, 0]
                        .texOffs(144, 0).addBox(-19.0F, 10.0F, -22.0F, 8.0F, 2.0F, 44.0F)
                        // Right mudguard walkway: UV [144, 0]
                        .texOffs(144, 0).addBox(11.0F, 10.0F, -22.0F, 8.0F, 2.0F, 44.0F)
                        // Rear fuel / hydraulic tank: UV [0, 76]
                        .texOffs(0, 76).addBox(-11.0F, 3.0F, -22.0F, 22.0F, 9.0F, 6.0F)
                        // Exhaust stack pipe: UV [374, 90]
                        .texOffs(374, 90).addBox(6.0F, -14.0F, 8.0F, 2.5F, 13.0F, 2.5F)
                        .texOffs(374, 90).addBox(5.5F, -15.5F, 7.5F, 3.5F, 2.0F, 3.5F) // rain flapper
                        // Cyclone air cleaner canister: UV [374, 90]
                        .texOffs(374, 90).addBox(-8.5F, -7.0F, 10.0F, 4.0F, 6.0F, 4.0F)

                        // ── Hollow ROPS Safety Cab Frame ──
                        // Cab roof: UV [184, 76]
                        .texOffs(184, 76).addBox(-12.0F, -19.0F, -16.0F, 24.0F, 3.0F, 20.0F)
                        // Front A-pillars (roll cage corner posts)
                        .texOffs(184, 76).addBox(-11.5F, -16.0F, 2.5F, 2.0F, 15.0F, 2.0F)
                        .texOffs(184, 76).addBox(9.5F, -16.0F, 2.5F, 2.0F, 15.0F, 2.0F)
                        // Rear B-pillars
                        .texOffs(184, 76).addBox(-11.5F, -16.0F, -15.5F, 2.0F, 15.0F, 2.0F)
                        .texOffs(184, 76).addBox(9.5F, -16.0F, -15.5F, 2.0F, 15.0F, 2.0F)
                        // Rear bulkhead panel
                        .texOffs(184, 76).addBox(-10.0F, -5.0F, -15.5F, 20.0F, 8.0F, 1.5F)

                        // ── Panoramic Safety Glass Windows: UV [314, 0] ──
                        // Front windshield (facing work area & blade)
                        .texOffs(314, 0).addBox(-9.5F, -16.0F, 3.0F, 19.0F, 14.0F, 0.5F)
                        // Left door glass window
                        .texOffs(314, 0).addBox(-11.5F, -16.0F, -13.5F, 0.5F, 14.0F, 16.0F)
                        // Right door glass window
                        .texOffs(314, 0).addBox(11.0F, -16.0F, -13.5F, 0.5F, 14.0F, 16.0F)
                        // Rear window
                        .texOffs(314, 0).addBox(-9.5F, -16.0F, -15.0F, 19.0F, 11.0F, 0.5F)

                        // ── Interior Cab Details: UV [124, 234] ──
                        // Operator suspension seat
                        .texOffs(124, 234).addBox(-4.0F, 4.0F, -10.0F, 8.0F, 5.0F, 8.0F)
                        .texOffs(124, 234).addBox(-4.0F, -5.0F, -10.0F, 8.0F, 9.0F, 2.0F)
                        // Left steering lever
                        .texOffs(124, 234).addBox(-6.0F, 1.0F, -2.0F, 1.0F, 8.0F, 1.0F)
                        // Right steering lever
                        .texOffs(124, 234).addBox(5.0F, 1.0F, -2.0F, 1.0F, 8.0F, 1.0F)
                        // Hydraulic blade control joystick
                        .texOffs(124, 234).addBox(7.0F, 2.0F, -6.0F, 1.0F, 6.0F, 1.0F)
                        // Dashboard console
                        .texOffs(124, 234).addBox(-6.0F, 3.0F, 0.0F, 12.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Safety warning beacon base & spinning reflector: UV [444, 0]
        PartDefinition beaconBase = mainBody.addOrReplaceChild(
                "beacon_base",
                CubeListBuilder.create()
                        .texOffs(444, 0).addBox(-2.5F, -23.0F, -6.0F, 5.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        beaconBase.addOrReplaceChild(
                "beacon_reflector",
                CubeListBuilder.create()
                        .texOffs(444, 0).addBox(-1.5F, -22.5F, -5.5F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ── 3. Hydraulic Lift Cylinders (Angled from radiator to push arms) ──
        mainBody.addOrReplaceChild(
                "left_cylinder",
                CubeListBuilder.create()
                        // Cylinder outer barrel: UV [294, 90]
                        .texOffs(294, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 14.0F)
                        // Chrome hydraulic ram: UV [330, 90]
                        .texOffs(330, 90).addBox(-1.0F, -1.0F, 12.0F, 2.0F, 2.0F, 10.0F),
                PartPose.offset(-11.5F, 4.0F, 18.0F)
        );

        mainBody.addOrReplaceChild(
                "right_cylinder",
                CubeListBuilder.create()
                        .texOffs(294, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 14.0F)
                        .texOffs(330, 90).addBox(-1.0F, -1.0F, 12.0F, 2.0F, 2.0F, 10.0F),
                PartPose.offset(11.5F, 4.0F, 18.0F)
        );

        // ── 4. C-Frame Push Arms (Pivoting on track frame) ───────────────
        PartDefinition pushArms = mainBody.addOrReplaceChild(
                "push_arms",
                CubeListBuilder.create()
                        // Left push arm beam: UV [184, 144]
                        .texOffs(184, 144).addBox(-20.5F, -2.0F, 0.0F, 4.0F, 4.0F, 34.0F)
                        // Right push arm beam: UV [184, 144]
                        .texOffs(184, 144).addBox(16.5F, -2.0F, 0.0F, 4.0F, 4.0F, 34.0F)
                        // Cross-tie cross member behind blade
                        .texOffs(184, 144).addBox(-18.0F, -2.0F, 30.0F, 36.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F)
        );

        // ── 5. Front Bulldozer Blade (The Working Moldboard) ─────────────
        PartDefinition blade = pushArms.addOrReplaceChild(
                "blade",
                CubeListBuilder.create()
                        // Center curved moldboard (3.0m wide = 48 units X, height 15 = 0.94m Y): UV [0, 144]
                        .texOffs(0, 144).addBox(-24.0F, -8.0F, 0.0F, 48.0F, 14.0F, 4.0F)
                        // Bottom hardened cutting edge / wear lip (flush with ground)
                        .texOffs(0, 215).addBox(-24.5F, 5.0F, -0.5F, 49.0F, 3.0F, 5.0F)
                        // Top spill shield
                        .texOffs(0, 144).addBox(-24.0F, -11.0F, 1.0F, 48.0F, 3.0F, 2.0F)
                        // Left side spill wing plate (retains material)
                        .texOffs(0, 144).addBox(-24.0F, -8.0F, 4.0F, 2.0F, 15.0F, 6.0F)
                        // Right side spill wing plate
                        .texOffs(0, 144).addBox(22.0F, -8.0F, 4.0F, 2.0F, 15.0F, 6.0F)
                        // Rear reinforcing push ribs
                        .texOffs(0, 144).addBox(-12.0F, -6.0F, -4.0F, 3.0F, 12.0F, 4.0F)
                        .texOffs(0, 144).addBox(9.0F, -6.0F, -4.0F, 3.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 34.0F)
        );

        // ── 6. Dynamic Carried Granular Material Layer ───────────────────
        blade.addOrReplaceChild(
                "carried_material",
                CubeListBuilder.create()
                        // Visible surcharge inside the moldboard curvature: UV [0, 234]
                        .texOffs(0, 234).addBox(-22.0F, -2.0F, 1.0F, 44.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void setupAnim(BulldozerRenderState state) {
        // Continuous blade lifting animation: positive bladeHeight lifts the blade up
        float armAngle = state.bladeHeight * 0.45F;
        this.pushArms.xRot = armAngle;

        // Blade pitch rotation on push arm tip
        this.blade.xRot = (float) Math.toRadians(state.bladeAngle) - armAngle;

        // Angled hydraulic cylinders follow push arm motion
        this.leftCylinder.xRot = armAngle * 0.70F;
        this.rightCylinder.xRot = armAngle * 0.70F;

        // Live carried material visibility & dynamic volume scaling
        if (state.carriedUnits > 0) {
            this.carriedMaterial.visible = true;
            float fill = Math.min(1.0F, state.fillRatio);
            this.carriedMaterial.yScale = 0.35F + (fill * 0.65F);
            this.carriedMaterial.zScale = 0.40F + (fill * 0.60F);
        } else {
            this.carriedMaterial.visible = false;
        }

        // Amber rotary safety warning beacon
        this.beaconReflector.yRot = state.beaconSpin;
        this.beaconReflector.visible = state.beaconFlash;
    }
}
