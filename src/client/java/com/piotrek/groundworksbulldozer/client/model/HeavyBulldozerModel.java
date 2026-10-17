package com.piotrek.groundworksbulldozer.client.model;

import com.piotrek.groundworksbulldozer.client.render.HeavyBulldozerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Geometric hierarchy model for the heavy mining crawler bulldozer (Cat D10T / D8T class).
 *
 * <p>Twice as large as the standard crawler bulldozer:
 * <ul>
 *   <li>Triangular elevated sprocket crawler undercarriage (High-Drive system).</li>
 *   <li>44 animated track links wrapping seamlessly around the triangular geometry.</li>
 *   <li>8 pairs of bottom bogie road wheels, elevated drive sprocket, front and rear idlers, and carrier roller.</li>
 *   <li>Massive 6.0m Semi-U moldboard blade with top rock guard spill mesh and hardened wear plates.</li>
 *   <li>Dual high-mounted hydraulic lift cylinders with animated telescoping chrome rods.</li>
 *   <li>Elevated panoramic ROPS/FOPS operator cab with side catwalks and yellow safety handrails.</li>
 *   <li>Massive diesel engine compartment with twin exhaust stacks, rain flappers, and cyclone air cleaners.</li>
 *   <li>Heavy rear ripper (zrywak) with parallel linkage frame and solid steel ripping shank.</li>
 *   <li>Live carried material surcharge layer dynamically scaling with load.</li>
 *   <li>Flashing amber safety warning beacon on cab roof.</li>
 * </ul>
 */
public class HeavyBulldozerModel extends EntityModel<HeavyBulldozerRenderState> {

    private final ModelPart undercarriage;
    private final ModelPart leftDriveSprocket;
    private final ModelPart rightDriveSprocket;
    private final ModelPart leftFrontIdler;
    private final ModelPart rightFrontIdler;
    private final ModelPart leftRearIdler;
    private final ModelPart rightRearIdler;
    private final ModelPart leftCarrierRoller;
    private final ModelPart rightCarrierRoller;
    private final ModelPart[] leftRoadWheels;
    private final ModelPart[] rightRoadWheels;
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
    private final ModelPart ripper;

    // Kinematic Pivot Offsets for Heavy Bulldozer (Cat D10T)
    private static final float PUSH_ARM_PIVOT_Y = 10.0F;
    private static final float PUSH_ARM_PIVOT_Z = -6.0F;
    private static final float CYLINDER_BASE_Y = -16.0F;
    private static final float CYLINDER_BASE_Z = 38.0F;
    private static final float CYLINDER_ATTACH_ARM_DIST = 52.0F;
    private static final float CYLINDER_ROD_START_Z = 12.0F;
    private static final float CYLINDER_ROD_MODEL_LENGTH = 18.0F;

    // Triangular Track Loop Constants
    private static final float SPROCKET_RADIUS = 8.0F;
    private static final float FRONT_IDLER_RADIUS = 7.0F;
    private static final float REAR_IDLER_RADIUS = 7.0F;
    private static final float ROAD_WHEEL_RADIUS = 4.0F;
    private static final float CARRIER_ROLLER_RADIUS = 3.5F;

    private static final int TRACK_LINK_COUNT = 44;
    private static final float ARC_REAR_LEN = 15.99F;
    private static final float LEN_INCLINE = 25.67F;
    private static final float ARC_SPROCKET_LEN = 9.75F;
    private static final float LEN_SLOPE = 53.88F;
    private static final float ARC_FRONT_LEN = 19.46F;
    private static final float LEN_BOTTOM = 66.00F;
    public static final float TRACK_LOOP_LENGTH = 190.75F;

    public HeavyBulldozerModel(ModelPart root) {
        super(root);
        this.undercarriage = root.getChild("undercarriage");

        ModelPart leftTrack = this.undercarriage.getChild("left_track");
        ModelPart rightTrack = this.undercarriage.getChild("right_track");

        this.leftDriveSprocket = leftTrack.getChild("drive_sprocket");
        this.rightDriveSprocket = rightTrack.getChild("drive_sprocket");
        this.leftFrontIdler = leftTrack.getChild("front_idler");
        this.rightFrontIdler = rightTrack.getChild("front_idler");
        this.leftRearIdler = leftTrack.getChild("rear_idler");
        this.rightRearIdler = rightTrack.getChild("rear_idler");
        this.leftCarrierRoller = leftTrack.getChild("carrier_roller");
        this.rightCarrierRoller = rightTrack.getChild("carrier_roller");

        this.leftRoadWheels = children(leftTrack, "road_wheel_", 8);
        this.rightRoadWheels = children(rightTrack, "road_wheel_", 8);
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
        this.ripper = this.mainBody.getChild("ripper");
    }

    private static ModelPart[] children(ModelPart parent, String prefix, int count) {
        ModelPart[] parts = new ModelPart[count];
        for (int i = 0; i < count; i++) {
            parts[i] = parent.getChild(prefix + i);
        }
        return parts;
    }

    private static CubeListBuilder crawlerStaticTriangularFrame() {
        return CubeListBuilder.create()
                // Bottom heavy roller guide rail
                .texOffs(164, 0).addBox(-2.5F, 11.0F, -32.0F, 5.0F, 6.0F, 70.0F)
                // Diagonal box member climbing up to elevated drive sprocket
                .texOffs(164, 0).addBox(-3.0F, -6.0F, -24.0F, 6.0F, 18.0F, 18.0F)
                // Front track tensioner recoil spring tube
                .texOffs(164, 0).addBox(-4.0F, 10.0F, 18.0F, 8.0F, 6.0F, 16.0F)
                .texOffs(164, 0).addBox(-4.5F, 9.5F, 22.0F, 9.0F, 7.0F, 2.0F)
                .texOffs(164, 0).addBox(-4.5F, 9.5F, 27.0F, 9.0F, 7.0F, 2.0F);
    }

    private static void addCrawlerMovingParts(PartDefinition track, boolean isLeft) {
        // 1. High-mounted Elevated Drive Sprocket (top apex at Y=-5.0, Z=-14.0)
        PartDefinition driveSprocket = track.addOrReplaceChild(
                "drive_sprocket",
                CubeListBuilder.create()
                        .texOffs(164, 0).addBox(-6.0F, -8.0F, -8.0F, 12.0F, 16.0F, 16.0F)
                        // Outer sprocket tooth spokes
                        .texOffs(0, 0).addBox(-6.4F, -2.5F, -9.0F, 12.8F, 5.0F, 18.0F)
                        .texOffs(0, 0).addBox(-6.4F, -9.0F, -2.5F, 12.8F, 18.0F, 5.0F)
                        // Planetary final drive center hub
                        .texOffs(404, 90).addBox(isLeft ? -7.2F : -4.0F, -4.0F, -4.0F, 11.2F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -5.0F, -14.0F)
        );

        // 2. Front Idler Wheel (large tension wheel at bottom front: Y=15.0, Z=36.0)
        track.addOrReplaceChild(
                "front_idler",
                CubeListBuilder.create()
                        .texOffs(164, 0).addBox(-5.5F, -7.0F, -7.0F, 11.0F, 14.0F, 14.0F)
                        .texOffs(0, 0).addBox(-5.8F, -2.0F, -7.8F, 11.6F, 4.0F, 15.6F)
                        .texOffs(0, 0).addBox(-5.8F, -7.8F, -2.0F, 11.6F, 15.6F, 4.0F)
                        .texOffs(404, 90).addBox(-6.2F, -3.0F, -3.0F, 12.4F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, 36.0F)
        );

        // 3. Rear Idler Wheel (bottom rear: Y=15.0, Z=-30.0)
        track.addOrReplaceChild(
                "rear_idler",
                CubeListBuilder.create()
                        .texOffs(164, 0).addBox(-5.5F, -7.0F, -7.0F, 11.0F, 14.0F, 14.0F)
                        .texOffs(0, 0).addBox(-5.8F, -2.0F, -7.8F, 11.6F, 4.0F, 15.6F)
                        .texOffs(0, 0).addBox(-5.8F, -7.8F, -2.0F, 11.6F, 15.6F, 4.0F)
                        .texOffs(404, 90).addBox(-6.2F, -3.0F, -3.0F, 12.4F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, -30.0F)
        );

        // 4. Upper Carrier Roller (supports diagonal track run at Y=-2.5, Z=11.0)
        track.addOrReplaceChild(
                "carrier_roller",
                CubeListBuilder.create()
                        .texOffs(164, 0).addBox(-5.0F, -3.5F, -3.5F, 10.0F, 7.0F, 7.0F)
                        .texOffs(404, 90).addBox(-5.4F, -1.5F, -1.5F, 10.8F, 3.0F, 3.0F),
                PartPose.offset(0.0F, -2.5F, 11.0F)
        );

        // 5. 8 pairs of heavy bottom bogie road wheels along the ground contact run
        float[] roadZ = {-22.0F, -15.0F, -8.0F, -1.0F, 6.0F, 13.0F, 20.0F, 27.0F};
        for (int i = 0; i < roadZ.length; i++) {
            track.addOrReplaceChild(
                    "road_wheel_" + i,
                    CubeListBuilder.create()
                            .texOffs(164, 0).addBox(-5.0F, -4.0F, -4.0F, 10.0F, 8.0F, 8.0F)
                            .texOffs(404, 90).addBox(-5.5F, -1.8F, -1.8F, 11.0F, 3.6F, 3.6F),
                    PartPose.offset(0.0F, 18.0F, roadZ[i])
            );
        }

        // 6. 44 animated track links wrapping around the complete triangular circuit
        for (int i = 0; i < TRACK_LINK_COUNT; i++) {
            track.addOrReplaceChild(
                    "track_link_" + i,
                    CubeListBuilder.create()
                            // Main heavy track shoe
                            .texOffs(0, 0).addBox(-6.5F, -0.7F, -2.0F, 13.0F, 1.4F, 4.0F)
                            // Raised extreme-service single grouser bar
                            .texOffs(0, 0).addBox(-6.8F, -1.9F, -0.6F, 13.6F, 1.2F, 1.2F),
                    PartPose.offset(0.0F, 22.0F, 36.0F)
            );
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 1. Undercarriage & Triangular Tracks ─────────────────────────
        PartDefinition undercarriage = root.addOrReplaceChild(
                "undercarriage",
                CubeListBuilder.create()
                        // Massive armored belly plate & main cross members: UV [164, 0]
                        .texOffs(164, 0).addBox(-18.0F, 8.0F, -32.0F, 36.0F, 12.0F, 72.0F)
                        // Front heavy towing cross-member
                        .texOffs(164, 0).addBox(-12.0F, 9.0F, 40.0F, 24.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Left track crawler assembly: X = -27.0
        PartDefinition leftTrack = undercarriage.addOrReplaceChild(
                "left_track",
                crawlerStaticTriangularFrame(),
                PartPose.offset(-27.0F, 0.0F, 0.0F)
        );
        addCrawlerMovingParts(leftTrack, true);

        // Right track crawler assembly: X = +27.0
        PartDefinition rightTrack = undercarriage.addOrReplaceChild(
                "right_track",
                crawlerStaticTriangularFrame(),
                PartPose.offset(27.0F, 0.0F, 0.0F)
        );
        addCrawlerMovingParts(rightTrack, false);

        // ── 2. Main Body (Engine Compartment, Cab & Ripper) ──────────────
        PartDefinition mainBody = root.addOrReplaceChild(
                "main_body",
                CubeListBuilder.create()
                        // Massive engine hood (front): UV [0, 76]
                        .texOffs(0, 76).addBox(-17.0F, -18.0F, 0.0F, 34.0F, 26.0F, 46.0F)
                        // Heavy radiator grille guard & doors (front nose): UV [105, 85]
                        .texOffs(105, 85).addBox(-16.0F, -17.0F, 46.0F, 32.0F, 24.0F, 4.0F)
                        // Elevated cab platform / subframe
                        .texOffs(164, 0).addBox(-18.0F, -6.0F, -26.0F, 36.0F, 14.0F, 28.0F)
                        // Rear fuel / hydraulic tank: UV [0, 76]
                        .texOffs(0, 76).addBox(-18.0F, -12.0F, -32.0F, 36.0F, 18.0F, 8.0F)

                        // ── Twin Exhaust Stacks (Cat V12 twin-turbo) ──
                        .texOffs(424, 184).addBox(-8.0F, -40.0F, 16.0F, 3.5F, 23.0F, 3.5F)
                        .texOffs(424, 184).addBox(4.5F, -40.0F, 16.0F, 3.5F, 23.0F, 3.5F)
                        // Rain flappers on top
                        .texOffs(424, 184).addBox(-8.5F, -42.0F, 15.5F, 4.5F, 2.0F, 4.5F)
                        .texOffs(424, 184).addBox(4.0F, -42.0F, 15.5F, 4.5F, 2.0F, 4.5F)

                        // ── Twin Cyclone Air Cleaner Pre-Cleaners ──
                        .texOffs(424, 184).addBox(-15.0F, -26.0F, 24.0F, 5.0F, 9.0F, 5.0F)
                        .texOffs(424, 184).addBox(10.0F, -26.0F, 24.0F, 5.0F, 9.0F, 5.0F)
                        .texOffs(424, 184).addBox(-16.0F, -28.5F, 23.0F, 7.0F, 2.5F, 7.0F)
                        .texOffs(424, 184).addBox(9.0F, -28.5F, 23.0F, 7.0F, 2.5F, 7.0F)

                        // ── Catwalks and Safety Handrails ──
                        // Left & right deck platforms: UV [424, 264]
                        .texOffs(424, 264).addBox(-22.0F, -6.0F, -26.0F, 4.0F, 2.0F, 32.0F)
                        .texOffs(424, 264).addBox(18.0F, -6.0F, -26.0F, 4.0F, 2.0F, 32.0F)
                        // Tubular safety handrails
                        .texOffs(184, 76).addBox(-22.5F, -22.0F, -26.0F, 1.0F, 16.0F, 1.0F)
                        .texOffs(184, 76).addBox(-22.5F, -22.0F, 4.0F, 1.0F, 16.0F, 1.0F)
                        .texOffs(184, 76).addBox(-22.5F, -22.0F, -26.0F, 1.0F, 1.5F, 30.0F)
                        .texOffs(184, 76).addBox(21.5F, -22.0F, -26.0F, 1.0F, 16.0F, 1.0F)
                        .texOffs(184, 76).addBox(21.5F, -22.0F, 4.0F, 1.0F, 16.0F, 1.0F)
                        .texOffs(184, 76).addBox(21.5F, -22.0F, -26.0F, 1.0F, 1.5F, 30.0F)

                        // ── Elevated ROPS / FOPS Safety Cab ──
                        // Cab roof with overhang visor: UV [184, 76]
                        .texOffs(184, 76).addBox(-16.0F, -42.0F, -24.0F, 32.0F, 4.0F, 28.0F)
                        // A-pillars and hexagonal front windshield posts
                        .texOffs(184, 76).addBox(-15.0F, -38.0F, 1.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(184, 76).addBox(12.0F, -38.0F, 1.0F, 3.0F, 32.0F, 3.0F)
                        // Rear B-pillars
                        .texOffs(184, 76).addBox(-15.0F, -38.0F, -23.0F, 3.0F, 32.0F, 3.0F)
                        .texOffs(184, 76).addBox(12.0F, -38.0F, -23.0F, 3.0F, 32.0F, 3.0F)
                        // Rear cab bulkhead
                        .texOffs(184, 76).addBox(-13.0F, -22.0F, -23.0F, 26.0F, 16.0F, 2.0F)

                        // ── Tinted Panoramic Glass Panes: UV [344, 0] ──
                        .texOffs(344, 0).addBox(-12.0F, -37.0F, 2.0F, 24.0F, 20.0F, 1.0F)   // front
                        .texOffs(344, 0).addBox(-15.5F, -37.0F, -20.0F, 1.0F, 18.0F, 20.0F) // left
                        .texOffs(344, 0).addBox(14.5F, -37.0F, -20.0F, 1.0F, 18.0F, 20.0F)  // right
                        .texOffs(344, 0).addBox(-12.0F, -37.0F, -22.5F, 24.0F, 15.0F, 1.0F) // rear

                        // ── Cab Interior Details: UV [0, 324] ──
                        // High-back air suspension seat
                        .texOffs(0, 324).addBox(-5.0F, -12.0F, -14.0F, 10.0F, 6.0F, 10.0F)
                        .texOffs(0, 324).addBox(-5.0F, -26.0F, -14.0F, 10.0F, 14.0F, 3.0F)
                        // Dual electro-hydraulic control consoles & joysticks
                        .texOffs(70, 324).addBox(-10.0F, -16.0F, -12.0F, 3.5F, 8.0F, 12.0F)
                        .texOffs(70, 324).addBox(6.5F, -16.0F, -12.0F, 3.5F, 8.0F, 12.0F)
                        // Dashboard instrumentation
                        .texOffs(70, 324).addBox(-8.0F, -14.0F, -1.0F, 16.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ── Heavy Rear Ripper Assembly (Zrywak): Pivoting parallel linkage ──
        mainBody.addOrReplaceChild(
                "ripper",
                CubeListBuilder.create()
                        // Parallel linkage mounting bracket
                        .texOffs(264, 254).addBox(-16.0F, -4.0F, -6.0F, 32.0F, 14.0F, 6.0F)
                        // Upper & lower ripper arms
                        .texOffs(264, 254).addBox(-14.0F, -2.0F, -14.0F, 5.0F, 4.0F, 10.0F)
                        .texOffs(264, 254).addBox(9.0F, -2.0F, -14.0F, 5.0F, 4.0F, 10.0F)
                        .texOffs(264, 254).addBox(-14.0F, 6.0F, -14.0F, 5.0F, 4.0F, 10.0F)
                        .texOffs(264, 254).addBox(9.0F, 6.0F, -14.0F, 5.0F, 4.0F, 10.0F)
                        // Heavy ripper beam across rear
                        .texOffs(424, 264).addBox(-18.0F, -3.0F, -16.0F, 36.0F, 12.0F, 5.0F)
                        // Solid steel ripper shank (curved claw)
                        .texOffs(354, 254).addBox(-3.0F, -8.0F, -20.0F, 6.0F, 26.0F, 7.0F)
                        .texOffs(354, 254).addBox(-3.0F, 14.0F, -24.0F, 6.0F, 6.0F, 8.0F)
                        // Hardened chisel tooth point at the tip
                        .texOffs(0, 284).addBox(-3.5F, 16.0F, -28.0F, 7.0F, 4.5F, 6.0F),
                PartPose.offset(0.0F, 4.0F, -32.0F)
        );

        // Warning Beacon on Cab Roof: UV [444, 0]
        PartDefinition beaconBase = mainBody.addOrReplaceChild(
                "beacon_base",
                CubeListBuilder.create()
                        .texOffs(444, 0).addBox(-3.0F, -47.0F, -10.0F, 6.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        beaconBase.addOrReplaceChild(
                "beacon_reflector",
                CubeListBuilder.create()
                        .texOffs(444, 0).addBox(-2.0F, -46.5F, -9.5F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ── 3. Dual High-Mounted Hydraulic Lift Cylinders ────────────────
        // Trunnions mounted high up on radiator shell (X=±15, Y=-16, Z=38)
        PartDefinition leftCylinder = mainBody.addOrReplaceChild(
                "left_cylinder",
                CubeListBuilder.create()
                        .texOffs(264, 174).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                        .texOffs(404, 90).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 16.0F)
                        .texOffs(404, 90).addBox(-3.0F, -3.0F, 14.0F, 6.0F, 6.0F, 3.0F),
                PartPose.offset(-15.0F, CYLINDER_BASE_Y, CYLINDER_BASE_Z)
        );
        leftCylinder.addOrReplaceChild(
                "rod",
                CubeListBuilder.create()
                        .texOffs(440, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, CYLINDER_ROD_MODEL_LENGTH),
                PartPose.offset(0.0F, 0.0F, CYLINDER_ROD_START_Z)
        );

        PartDefinition rightCylinder = mainBody.addOrReplaceChild(
                "right_cylinder",
                CubeListBuilder.create()
                        .texOffs(264, 174).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                        .texOffs(404, 90).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 16.0F)
                        .texOffs(404, 90).addBox(-3.0F, -3.0F, 14.0F, 6.0F, 6.0F, 3.0F),
                PartPose.offset(15.0F, CYLINDER_BASE_Y, CYLINDER_BASE_Z)
        );
        rightCylinder.addOrReplaceChild(
                "rod",
                CubeListBuilder.create()
                        .texOffs(440, 90).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, CYLINDER_ROD_MODEL_LENGTH),
                PartPose.offset(0.0F, 0.0F, CYLINDER_ROD_START_Z)
        );

        // ── 4. Heavy Push Arms (Pivoting outside the triangular tracks) ───
        PartDefinition pushArms = mainBody.addOrReplaceChild(
                "push_arms",
                CubeListBuilder.create()
                        // Outboard rear pivot housings (outside the 54F track envelope)
                        .texOffs(264, 174).addBox(-38.0F, -5.0F, -6.0F, 6.0F, 10.0F, 10.0F)
                        .texOffs(264, 174).addBox(32.0F, -5.0F, -6.0F, 6.0F, 10.0F, 10.0F)
                        // Extra-heavy box-section push arm beams (length ~62F)
                        .texOffs(264, 174).addBox(-37.0F, -3.0F, -2.0F, 5.0F, 6.0F, 58.0F)
                        .texOffs(264, 174).addBox(32.0F, -3.0F, -2.0F, 5.0F, 6.0F, 58.0F)
                        // Diagonal tag-link / stabilizer braces
                        .texOffs(264, 174).addBox(-34.0F, -2.0F, 32.0F, 16.0F, 4.0F, 5.0F)
                        .texOffs(264, 174).addBox(18.0F, -2.0F, 32.0F, 16.0F, 4.0F, 5.0F)
                        // Hydraulic cylinder clevis attachment points on push arms
                        .texOffs(404, 90).addBox(-36.0F, -7.0F, 42.0F, 4.0F, 6.0F, 8.0F)
                        .texOffs(404, 90).addBox(32.0F, -7.0F, 42.0F, 4.0F, 6.0F, 8.0F)
                        // Heavy forward cross tie
                        .texOffs(264, 174).addBox(-33.0F, -2.0F, 48.0F, 66.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, PUSH_ARM_PIVOT_Y, PUSH_ARM_PIVOT_Z)
        );

        // ── 5. Front Semi-U Bulldozer Blade (6.0m Wide Heavy Moldboard) ──
        PartDefinition blade = pushArms.addOrReplaceChild(
                "blade",
                CubeListBuilder.create()
                        // Center moldboard, 60 units. Wings open to ±48 so the graphic matches the 6.0 m cut.
                        .texOffs(0, 174).addBox(-30.0F, -12.0F, 0.0F, 60.0F, 28.0F, 6.0F)
                        .texOffs(0, 284).addBox(-30.0F, 14.0F, -1.0F, 60.0F, 4.0F, 8.0F)
                        .texOffs(0, 174).addBox(-30.0F, -18.0F, 1.0F, 60.0F, 6.0F, 3.0F)
                        // Rear reinforcement ribs
                        .texOffs(0, 174).addBox(-20.0F, -10.0F, -6.0F, 4.0F, 24.0F, 6.0F)
                        .texOffs(0, 174).addBox(-2.0F, -10.0F, -6.0F, 4.0F, 24.0F, 6.0F)
                        .texOffs(0, 174).addBox(16.0F, -10.0F, -6.0F, 4.0F, 24.0F, 6.0F)
                        // Rear structural backing frame tying blade to push arms
                        .texOffs(264, 174).addBox(-38.0F, -6.0F, -8.0F, 76.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 56.0F)
        );
        // Renderer mirrors X, so the yaw that looks forward is the opposite of model-space forward.
        float wingOpen = 0.451F;
        blade.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 174).addBox(-20.0F, -12.0F, 0.0F, 20.0F, 28.0F, 6.0F)
                        .texOffs(0, 284).addBox(-20.0F, 14.0F, -1.0F, 20.0F, 4.0F, 8.0F)
                        .texOffs(0, 174).addBox(-20.0F, -18.0F, 1.0F, 20.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(-30.0F, 0.0F, 0.0F, 0.0F, wingOpen, 0.0F)
        );
        blade.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 174).addBox(0.0F, -12.0F, 0.0F, 20.0F, 28.0F, 6.0F)
                        .texOffs(0, 284).addBox(0.0F, 14.0F, -1.0F, 20.0F, 4.0F, 8.0F)
                        .texOffs(0, 174).addBox(0.0F, -18.0F, 1.0F, 20.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(30.0F, 0.0F, 0.0F, 0.0F, -wingOpen, 0.0F)
        );

        // ── 6. Dynamic Carried Granular Material Surcharge Layer ──────────
        blade.addOrReplaceChild(
                "carried_material",
                CubeListBuilder.create()
                        .texOffs(144, 324).addBox(-44.0F, 2.0F, 2.0F, 88.0F, 14.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void setupAnim(HeavyBulldozerRenderState state) {
        animateCrawler(
                state.leftTrackTravel,
                this.leftDriveSprocket,
                this.leftFrontIdler,
                this.leftRearIdler,
                this.leftCarrierRoller,
                this.leftRoadWheels,
                this.leftTrackLinks
        );
        animateCrawler(
                state.rightTrackTravel,
                this.rightDriveSprocket,
                this.rightFrontIdler,
                this.rightRearIdler,
                this.rightCarrierRoller,
                this.rightRoadWheels,
                this.rightTrackLinks
        );

        // Continuous blade lifting animation
        float armAngle = state.bladeHeight * 0.38F;
        this.pushArms.xRot = armAngle;

        // Arms lift the blade. The bite angle is only the ramp, and it returns to zero with the lever.
        this.blade.xRot = -armAngle + (float) Math.toRadians(state.bladeAngle);

        // Ripper claw kinematics: 0.0 = raised (-18° up), 1.0 = deep ripping (+22° down)
        float ripperAngle = (float) Math.toRadians(-18.0F + (state.ripperLift * 40.0F));
        this.ripper.xRot = ripperAngle;

        // Dynamic hydraulic lift cylinder kinematics
        float attachArmZ = CYLINDER_ATTACH_ARM_DIST;
        float targetY = PUSH_ARM_PIVOT_Y - (attachArmZ * (float) Math.sin(armAngle));
        float targetZ = PUSH_ARM_PIVOT_Z + (attachArmZ * (float) Math.cos(armAngle));
        float deltaY = targetY - CYLINDER_BASE_Y;
        float deltaZ = targetZ - CYLINDER_BASE_Z;
        float cylinderLength = (float) Math.sqrt(deltaY * deltaY + deltaZ * deltaZ);
        float cylinderAngle = (float) Math.atan2(-deltaY, deltaZ);

        this.leftCylinder.xRot = cylinderAngle;
        this.rightCylinder.xRot = cylinderAngle;

        float rodLength = Math.max(
                3.0F,
                Math.min(CYLINDER_ROD_MODEL_LENGTH, cylinderLength - CYLINDER_ROD_START_Z)
        );
        float rodScale = rodLength / CYLINDER_ROD_MODEL_LENGTH;
        this.leftCylinderRod.zScale = rodScale;
        this.rightCylinderRod.zScale = rodScale;

        // Live carried material visibility & dynamic scaling
        // Spoil is a separate mesh so it can wear the cargo block texture.
        this.carriedMaterial.visible = false;

        // Amber rotary safety warning beacon
        this.beaconReflector.yRot = state.beaconSpin;
        this.beaconReflector.visible = state.beaconFlash;
    }

    private static void animateCrawler(
            float travel,
            ModelPart driveSprocket,
            ModelPart frontIdler,
            ModelPart rearIdler,
            ModelPart carrierRoller,
            ModelPart[] roadWheels,
            ModelPart[] links
    ) {
        driveSprocket.xRot = -travel / SPROCKET_RADIUS;
        frontIdler.xRot = -travel / FRONT_IDLER_RADIUS;
        rearIdler.xRot = -travel / REAR_IDLER_RADIUS;
        carrierRoller.xRot = -travel / CARRIER_ROLLER_RADIUS;

        for (ModelPart roadWheel : roadWheels) {
            roadWheel.xRot = -travel / ROAD_WHEEL_RADIUS;
        }

        float spacing = TRACK_LOOP_LENGTH / TRACK_LINK_COUNT;
        for (int i = 0; i < links.length; i++) {
            placeTrackLinkOnTriangularLoop(links[i], wrapLoopDistance(travel + (i * spacing)));
        }
    }

    private static float wrapLoopDistance(float distance) {
        distance %= TRACK_LOOP_LENGTH;
        if (distance < 0.0F) {
            distance += TRACK_LOOP_LENGTH;
        }
        return distance;
    }

    private static void placeTrackLinkOnTriangularLoop(ModelPart link, float distance) {
        // 1. Bottom flat run: front idler -> rear idler at ground level (z=36 to z=-30, y=22)
        if (distance < LEN_BOTTOM) {
            link.z = 36.0F - distance;
            link.y = 22.0F;
            link.xRot = (float) Math.PI;
            return;
        }
        distance -= LEN_BOTTOM;

        // 2. Rear wrap around Rear Idler (z=-30, y=15), r=7. From 90° to 220.897°
        if (distance < ARC_REAR_LEN) {
            float t = distance / ARC_REAR_LEN;
            float phi = (float) Math.toRadians(90.0D + t * (220.897D - 90.0D));
            link.z = -30.0F + 7.0F * (float) Math.cos(phi);
            link.y = 15.0F + 7.0F * (float) Math.sin(phi);
            link.xRot = phi + (float) (Math.PI * 0.5D);
            return;
        }
        distance -= ARC_REAR_LEN;

        // 3. Rear incline straight: from (z=-35.291, y=10.417) to (z=-20.047, y=-10.238)
        if (distance < LEN_INCLINE) {
            float t = distance / LEN_INCLINE;
            link.z = -35.291F + t * (-20.047F - (-35.291F));
            link.y = 10.417F + t * (-10.238F - 10.417F);
            link.xRot = (float) Math.toRadians(220.897D + 90.0D);
            return;
        }
        distance -= LEN_INCLINE;

        // 4. Elevated drive sprocket top wrap (z=-14, y=-5), r=8. From 220.897° to 290.737°
        if (distance < ARC_SPROCKET_LEN) {
            float t = distance / ARC_SPROCKET_LEN;
            float phi = (float) Math.toRadians(220.897D + t * (290.737D - 220.897D));
            link.z = -14.0F + 8.0F * (float) Math.cos(phi);
            link.y = -5.0F + 8.0F * (float) Math.sin(phi);
            link.xRot = phi + (float) (Math.PI * 0.5D);
            return;
        }
        distance -= ARC_SPROCKET_LEN;

        // 5. Front diagonal slope: from (z=-11.167, y=-12.482) to (z=38.479, y=8.454)
        if (distance < LEN_SLOPE) {
            float t = distance / LEN_SLOPE;
            link.z = -11.167F + t * (38.479F - (-11.167F));
            link.y = -12.482F + t * (8.454F - (-12.482F));
            link.xRot = (float) Math.toRadians(290.737D + 90.0D);
            return;
        }
        distance -= LEN_SLOPE;

        // 6. Front wrap around Front Idler (z=36, y=15), r=7. From 290.737° to 450°
        float t = distance / ARC_FRONT_LEN;
        float phi = (float) Math.toRadians(290.737D + t * (450.0D - 290.737D));
        link.z = 36.0F + 7.0F * (float) Math.cos(phi);
        link.y = 15.0F + 7.0F * (float) Math.sin(phi);
        link.xRot = phi + (float) (Math.PI * 0.5D);
    }
}
