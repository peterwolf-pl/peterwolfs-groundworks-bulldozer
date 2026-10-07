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
    private final ModelPart leftDriveSprocket;
    private final ModelPart rightDriveSprocket;
    private final ModelPart leftIdler;
    private final ModelPart rightIdler;
    private final ModelPart[] leftRoadWheels;
    private final ModelPart[] rightRoadWheels;
    private final ModelPart[] leftCarrierRollers;
    private final ModelPart[] rightCarrierRollers;
    private final ModelPart[] leftTrackLinks;
    private final ModelPart[] rightTrackLinks;
    private final ModelPart mainBody;
    private final ModelPart pushArms;
    private final ModelPart blade;
    private final ModelPart carriedMaterial;
    private final ModelPart leftCylinder;
    private final ModelPart rightCylinder;
    private final ModelPart leftCylinderRod;
    private final ModelPart rightCylinderRod;
    private final ModelPart beaconReflector;

    private static final float PUSH_ARM_PIVOT_Y = 15.0F;
    private static final float CYLINDER_BASE_Y = 5.0F;
    private static final float CYLINDER_BASE_Z = 10.0F;
    private static final float CYLINDER_ARM_ATTACH_Z = 22.0F;
    private static final float CYLINDER_ROD_START_Z = 8.5F;
    private static final float CYLINDER_ROD_MODEL_LENGTH = 12.0F;

    private static final float DRIVE_SPROCKET_RADIUS = 4.8F;
    private static final float IDLER_RADIUS = 4.8F;
    private static final float ROAD_WHEEL_RADIUS = 2.75F;
    private static final float CARRIER_ROLLER_RADIUS = 2.0F;
    private static final int TRACK_LINK_COUNT = 36;
    private static final float TRACK_CENTER_FRONT_Z = 21.0F;
    private static final float TRACK_CENTER_REAR_Z = -21.0F;
    private static final float TRACK_LOOP_RADIUS = 6.3F;
    private static final float TRACK_STRAIGHT_LENGTH = TRACK_CENTER_FRONT_Z - TRACK_CENTER_REAR_Z;
    private static final float TRACK_ARC_LENGTH = (float) Math.PI * TRACK_LOOP_RADIUS;
    public static final float TRACK_LOOP_LENGTH =
            (2.0F * TRACK_STRAIGHT_LENGTH) + (2.0F * TRACK_ARC_LENGTH);

    public BulldozerModel(ModelPart root) {
        super(root);
        this.undercarriage = root.getChild("undercarriage");

        ModelPart leftTrack = this.undercarriage.getChild("left_track");
        ModelPart rightTrack = this.undercarriage.getChild("right_track");
        this.leftDriveSprocket = leftTrack.getChild("drive_sprocket");
        this.rightDriveSprocket = rightTrack.getChild("drive_sprocket");
        this.leftIdler = leftTrack.getChild("idler");
        this.rightIdler = rightTrack.getChild("idler");
        this.leftRoadWheels = children(leftTrack, "road_wheel_", 5);
        this.rightRoadWheels = children(rightTrack, "road_wheel_", 5);
        this.leftCarrierRollers = children(leftTrack, "carrier_roller_", 2);
        this.rightCarrierRollers = children(rightTrack, "carrier_roller_", 2);
        this.leftTrackLinks = children(leftTrack, "track_link_", TRACK_LINK_COUNT);
        this.rightTrackLinks = children(rightTrack, "track_link_", TRACK_LINK_COUNT);

        this.mainBody = root.getChild("main_body");
        this.pushArms = this.mainBody.getChild("push_arms");
        this.blade = this.pushArms.getChild("blade");
        this.carriedMaterial = this.blade.getChild("carried_material");
        this.leftCylinder = this.mainBody.getChild("left_cylinder");
        this.rightCylinder = this.mainBody.getChild("right_cylinder");
        this.leftCylinderRod = this.leftCylinder.getChild("rod");
        this.rightCylinderRod = this.rightCylinder.getChild("rod");
        this.beaconReflector = this.mainBody.getChild("beacon_base").getChild("beacon_reflector");
    }

    private static ModelPart[] children(ModelPart parent, String prefix, int count) {
        ModelPart[] parts = new ModelPart[count];
        for (int i = 0; i < count; i++) {
            parts[i] = parent.getChild(prefix + i);
        }
        return parts;
    }

    private static CubeListBuilder crawlerStaticGeometry() {
        return CubeListBuilder.create()
                // Only the inner roller frame stays static. The belt itself is built from
                // animated links, so no slab can hide motion on the top or end wraps.
                .texOffs(144, 0).addBox(-1.2F, -2.0F, -22.5F, 2.4F, 5.0F, 45.0F)
                // Rear recoil/tensioner body behind the idler.
                .texOffs(144, 0).addBox(-3.2F, -1.8F, -19.0F, 6.4F, 3.6F, 9.0F)
                .texOffs(144, 0).addBox(-3.6F, -2.2F, -17.2F, 7.2F, 4.4F, 1.2F)
                .texOffs(144, 0).addBox(-3.6F, -2.2F, -14.6F, 7.2F, 4.4F, 1.2F)
                .texOffs(144, 0).addBox(-3.6F, -2.2F, -12.0F, 7.2F, 4.4F, 1.2F);
    }

    private static void addCrawlerMovingParts(PartDefinition track) {
        // Large front drive sprocket with visible cross spokes and hub.
        track.addOrReplaceChild(
                "drive_sprocket",
                CubeListBuilder.create()
                        .texOffs(144, 0).addBox(-4.8F, -4.8F, -3.2F, 9.6F, 9.6F, 6.4F)
                        .texOffs(0, 0).addBox(-5.1F, -1.0F, -4.0F, 10.2F, 2.0F, 8.0F)
                        .texOffs(0, 0).addBox(-5.1F, -4.0F, -1.0F, 10.2F, 8.0F, 2.0F)
                        .texOffs(330, 90).addBox(-5.4F, -2.0F, -2.0F, 10.8F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 21.0F)
        );

        // Large recoil idler / tension wheel with a prominent center hub.
        track.addOrReplaceChild(
                "idler",
                CubeListBuilder.create()
                        .texOffs(144, 0).addBox(-4.8F, -4.8F, -3.2F, 9.6F, 9.6F, 6.4F)
                        .texOffs(144, 0).addBox(-5.2F, -0.9F, -3.8F, 10.4F, 1.8F, 7.6F)
                        .texOffs(144, 0).addBox(-5.2F, -3.8F, -0.9F, 10.4F, 7.6F, 1.8F)
                        .texOffs(330, 90).addBox(-5.5F, -2.1F, -2.1F, 11.0F, 4.2F, 4.2F),
                PartPose.offset(0.0F, 0.0F, -21.0F)
        );

        float[] roadZ = {-15.0F, -7.5F, 0.0F, 7.5F, 15.0F};
        for (int i = 0; i < roadZ.length; i++) {
            track.addOrReplaceChild(
                    "road_wheel_" + i,
                    CubeListBuilder.create()
                            .texOffs(144, 0).addBox(-4.2F, -2.75F, -2.75F, 8.4F, 5.5F, 5.5F)
                            .texOffs(330, 90).addBox(-4.6F, -1.0F, -1.0F, 9.2F, 2.0F, 2.0F),
                    PartPose.offset(0.0F, 3.0F, roadZ[i])
            );
        }

        float[] carrierZ = {-8.0F, 8.0F};
        for (int i = 0; i < carrierZ.length; i++) {
            track.addOrReplaceChild(
                    "carrier_roller_" + i,
                    CubeListBuilder.create()
                            .texOffs(144, 0).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                            .texOffs(330, 90).addBox(-4.4F, -0.8F, -0.8F, 8.8F, 1.6F, 1.6F),
                    PartPose.offset(0.0F, -4.0F, carrierZ[i])
            );
        }

        // Full animated belt loop. Every link follows the same continuous rounded
        // rectangle, including the front drive-sprocket wrap and rear idler wrap.
        for (int i = 0; i < TRACK_LINK_COUNT; i++) {
            track.addOrReplaceChild(
                    "track_link_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-5.0F, -0.75F, -1.8F, 10.0F, 1.5F, 3.6F),
                    PartPose.offset(0.0F, -TRACK_LOOP_RADIUS, TRACK_CENTER_REAR_Z)
            );
        }
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

        // Left track crawler assembly: open frame like the excavator undercarriage.
        PartDefinition leftTrack = undercarriage.addOrReplaceChild(
                "left_track",
                crawlerStaticGeometry(),
                PartPose.offset(-15.5F, 18.0F, 0.0F)
        );
        addCrawlerMovingParts(leftTrack);

        // Right track crawler assembly.
        PartDefinition rightTrack = undercarriage.addOrReplaceChild(
                "right_track",
                crawlerStaticGeometry(),
                PartPose.offset(15.5F, 18.0F, 0.0F)
        );
        addCrawlerMovingParts(rightTrack);

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

        // ── 3. Hydraulic Lift Cylinders & Fixed Trunnion Mounts ─────────
        // Fixed chassis-side trunnion blocks. These stay attached to the tractor body
        // while the complete cylinder assemblies rotate around their pivot pins.
        mainBody.addOrReplaceChild(
                "hydraulic_mounts",
                CubeListBuilder.create()
                        .texOffs(184, 144).addBox(-15.0F, 2.0F, 7.0F, 7.0F, 6.0F, 6.0F)
                        .texOffs(184, 144).addBox(8.0F, 2.0F, 7.0F, 7.0F, 6.0F, 6.0F)
                        .texOffs(330, 90).addBox(-12.8F, 3.6F, 5.8F, 2.6F, 2.6F, 8.4F)
                        .texOffs(330, 90).addBox(10.2F, 3.6F, 5.8F, 2.6F, 2.6F, 8.4F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition leftCylinder = mainBody.addOrReplaceChild(
                "left_cylinder",
                CubeListBuilder.create()
                        // Pivot eye / rear clevis
                        .texOffs(184, 144).addBox(-2.3F, -2.3F, -2.0F, 4.6F, 4.6F, 4.0F)
                        // Main hydraulic barrel
                        .texOffs(294, 90).addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 10.0F)
                        // Front gland
                        .texOffs(294, 90).addBox(-2.1F, -2.1F, 8.4F, 4.2F, 4.2F, 2.2F),
                PartPose.offset(-11.5F, CYLINDER_BASE_Y, CYLINDER_BASE_Z)
        );
        leftCylinder.addOrReplaceChild(
                "rod",
                CubeListBuilder.create()
                        .texOffs(330, 90).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, CYLINDER_ROD_MODEL_LENGTH),
                PartPose.offset(0.0F, 0.0F, CYLINDER_ROD_START_Z)
        );

        PartDefinition rightCylinder = mainBody.addOrReplaceChild(
                "right_cylinder",
                CubeListBuilder.create()
                        .texOffs(184, 144).addBox(-2.3F, -2.3F, -2.0F, 4.6F, 4.6F, 4.0F)
                        .texOffs(294, 90).addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 10.0F)
                        .texOffs(294, 90).addBox(-2.1F, -2.1F, 8.4F, 4.2F, 4.2F, 2.2F),
                PartPose.offset(11.5F, CYLINDER_BASE_Y, CYLINDER_BASE_Z)
        );
        rightCylinder.addOrReplaceChild(
                "rod",
                CubeListBuilder.create()
                        .texOffs(330, 90).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, CYLINDER_ROD_MODEL_LENGTH),
                PartPose.offset(0.0F, 0.0F, CYLINDER_ROD_START_Z)
        );

        // ── 4. C-Frame Push Arms (Pivoting on track frame) ───────────────
        PartDefinition pushArms = mainBody.addOrReplaceChild(
                "push_arms",
                CubeListBuilder.create()
                        // Extra-wide C-frame runs completely outside the crawler envelope.
                        // Tracks span roughly X=-20..-11 and X=11..20, so the arms begin
                        // beyond X=22 with a visible air gap instead of intersecting them.
                        .texOffs(184, 144).addBox(-27.0F, -2.0F, -2.0F, 4.0F, 4.0F, 36.0F)
                        .texOffs(184, 144).addBox(23.0F, -2.0F, -2.0F, 4.0F, 4.0F, 36.0F)
                        // Outboard rear pivot housings.
                        .texOffs(184, 144).addBox(-28.5F, -4.0F, -5.0F, 6.0F, 8.0F, 8.0F)
                        .texOffs(184, 144).addBox(22.5F, -4.0F, -5.0F, 6.0F, 8.0F, 8.0F)
                        // Reinforcement gussets near the blade end.
                        .texOffs(184, 144).addBox(-28.0F, -5.0F, 24.0F, 5.5F, 3.0F, 9.0F)
                        .texOffs(184, 144).addBox(22.5F, -5.0F, 24.0F, 5.5F, 3.0F, 9.0F)
                        // Hydraulic rod clevis mounts stay outboard with the C-frame.
                        .texOffs(330, 90).addBox(-27.0F, -4.0F, 19.0F, 4.5F, 8.0F, 6.0F)
                        .texOffs(330, 90).addBox(22.5F, -4.0F, 19.0F, 4.5F, 8.0F, 6.0F)
                        // Cross-ties are pushed ahead of the track nose so they no longer
                        // pass through the front sprocket silhouette.
                        .texOffs(184, 144).addBox(-24.0F, -2.0F, 28.0F, 48.0F, 4.0F, 4.0F)
                        .texOffs(184, 144).addBox(-24.0F, -2.0F, 32.0F, 48.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F)
        );

        // ── 5. Front Bulldozer Blade (The Working Moldboard) ─────────────
        PartDefinition blade = pushArms.addOrReplaceChild(
                "blade",
                CubeListBuilder.create()
                        // Center moldboard
                        .texOffs(0, 144).addBox(-24.0F, -8.0F, 0.0F, 48.0F, 14.0F, 4.0F)
                        // Bottom hardened cutting edge / replaceable wear lip
                        .texOffs(0, 215).addBox(-24.5F, 5.0F, -0.5F, 49.0F, 3.0F, 5.0F)
                        // Top spill shield
                        .texOffs(0, 144).addBox(-24.0F, -11.0F, 1.0F, 48.0F, 3.0F, 2.0F)
                        // Side spill wings
                        .texOffs(0, 144).addBox(-24.0F, -8.0F, 4.0F, 2.0F, 15.0F, 6.0F)
                        .texOffs(0, 144).addBox(22.0F, -8.0F, 4.0F, 2.0F, 15.0F, 6.0F)
                        // Full-width rear backing beam ties the moldboard into the C-frame
                        .texOffs(184, 144).addBox(-21.0F, -1.5F, -6.5F, 42.0F, 3.0F, 3.0F)
                        // Rear reinforcing ribs
                        .texOffs(0, 144).addBox(-16.0F, -7.0F, -4.5F, 3.0F, 13.0F, 4.5F)
                        .texOffs(0, 144).addBox(-1.5F, -7.0F, -4.5F, 3.0F, 13.0F, 4.5F)
                        .texOffs(0, 144).addBox(13.0F, -7.0F, -4.5F, 3.0F, 13.0F, 4.5F)
                        // Left/right blade mounting ears and pivot blocks
                        .texOffs(184, 144).addBox(-19.0F, -4.5F, -8.0F, 7.0F, 9.0F, 4.0F)
                        .texOffs(184, 144).addBox(12.0F, -4.5F, -8.0F, 7.0F, 9.0F, 4.0F)
                        .texOffs(330, 90).addBox(-17.5F, -2.0F, -9.0F, 4.0F, 4.0F, 6.0F)
                        .texOffs(330, 90).addBox(13.5F, -2.0F, -9.0F, 4.0F, 4.0F, 6.0F),
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
        animateCrawler(
                state.leftTrackTravel,
                this.leftDriveSprocket,
                this.leftIdler,
                this.leftRoadWheels,
                this.leftCarrierRollers,
                this.leftTrackLinks
        );
        animateCrawler(
                state.rightTrackTravel,
                this.rightDriveSprocket,
                this.rightIdler,
                this.rightRoadWheels,
                this.rightCarrierRollers,
                this.rightTrackLinks
        );

        // Continuous blade lifting animation: positive bladeHeight lifts the blade up
        float armAngle = state.bladeHeight * 0.45F;
        this.pushArms.xRot = armAngle;

        // Blade pitch rotation on push arm tip
        this.blade.xRot = (float) Math.toRadians(state.bladeAngle) - armAngle;

        // Real cylinder kinematics. The chassis trunnion is fixed and the rod-end target
        // moves with the C-frame. Both cylinder angle and chrome rod extension therefore
        // follow the actual blade lift instead of using an arbitrary rotation multiplier.
        float targetY = PUSH_ARM_PIVOT_Y - (CYLINDER_ARM_ATTACH_Z * (float) Math.sin(armAngle));
        float targetZ = CYLINDER_ARM_ATTACH_Z * (float) Math.cos(armAngle);
        float deltaY = targetY - CYLINDER_BASE_Y;
        float deltaZ = targetZ - CYLINDER_BASE_Z;
        float cylinderLength = (float) Math.sqrt(deltaY * deltaY + deltaZ * deltaZ);
        float cylinderAngle = (float) Math.atan2(-deltaY, deltaZ);

        this.leftCylinder.xRot = cylinderAngle;
        this.rightCylinder.xRot = cylinderAngle;

        float rodLength = Math.max(
                2.0F,
                Math.min(CYLINDER_ROD_MODEL_LENGTH, cylinderLength - CYLINDER_ROD_START_Z)
        );
        float rodScale = rodLength / CYLINDER_ROD_MODEL_LENGTH;
        this.leftCylinderRod.zScale = rodScale;
        this.rightCylinderRod.zScale = rodScale;

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

    private static void animateCrawler(
            float travel,
            ModelPart driveSprocket,
            ModelPart idler,
            ModelPart[] roadWheels,
            ModelPart[] carrierRollers,
            ModelPart[] links
    ) {
        // Wheel angular velocity follows v/r. Both sides are independent, so pivot turns
        // visibly rotate the two crawler systems in opposite directions.
        driveSprocket.xRot = -travel / DRIVE_SPROCKET_RADIUS;
        idler.xRot = -travel / IDLER_RADIUS;

        for (ModelPart roadWheel : roadWheels) {
            roadWheel.xRot = -travel / ROAD_WHEEL_RADIUS;
        }
        for (ModelPart carrierRoller : carrierRollers) {
            carrierRoller.xRot = -travel / CARRIER_ROLLER_RADIUS;
        }

        float spacing = TRACK_LOOP_LENGTH / TRACK_LINK_COUNT;
        for (int i = 0; i < links.length; i++) {
            placeTrackLinkOnLoop(links[i], wrapLoopDistance(travel + (i * spacing)));
        }
    }

    private static float wrapLoopDistance(float distance) {
        distance %= TRACK_LOOP_LENGTH;
        if (distance < 0.0F) {
            distance += TRACK_LOOP_LENGTH;
        }
        return distance;
    }

    private static void placeTrackLinkOnLoop(ModelPart link, float distance) {
        // 1. Upper run: rear idler -> front sprocket.
        if (distance < TRACK_STRAIGHT_LENGTH) {
            link.y = -TRACK_LOOP_RADIUS;
            link.z = TRACK_CENTER_REAR_Z + distance;
            link.xRot = 0.0F;
            return;
        }
        distance -= TRACK_STRAIGHT_LENGTH;

        // 2. Front wrap: top -> nose -> bottom around the drive sprocket.
        if (distance < TRACK_ARC_LENGTH) {
            float t = distance / TRACK_ARC_LENGTH;
            float angle = (float) (-Math.PI * 0.5D + (Math.PI * t));
            link.y = (float) Math.sin(angle) * TRACK_LOOP_RADIUS;
            link.z = TRACK_CENTER_FRONT_Z + ((float) Math.cos(angle) * TRACK_LOOP_RADIUS);
            link.xRot = angle + ((float) Math.PI * 0.5F);
            return;
        }
        distance -= TRACK_ARC_LENGTH;

        // 3. Lower run: front sprocket -> rear idler.
        if (distance < TRACK_STRAIGHT_LENGTH) {
            link.y = TRACK_LOOP_RADIUS;
            link.z = TRACK_CENTER_FRONT_Z - distance;
            link.xRot = (float) Math.PI;
            return;
        }
        distance -= TRACK_STRAIGHT_LENGTH;

        // 4. Rear wrap: bottom -> tail -> top around the tension/idler wheel.
        float t = distance / TRACK_ARC_LENGTH;
        float angle = (float) (Math.PI * 0.5D + (Math.PI * t));
        link.y = (float) Math.sin(angle) * TRACK_LOOP_RADIUS;
        link.z = TRACK_CENTER_REAR_Z + ((float) Math.cos(angle) * TRACK_LOOP_RADIUS);
        link.xRot = angle + ((float) Math.PI * 0.5F);
    }
}
