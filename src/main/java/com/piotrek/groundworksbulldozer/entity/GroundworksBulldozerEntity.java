package com.piotrek.groundworksbulldozer.entity;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.api.excavation.ExcavationResult;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import com.piotrek.groundworksbulldozer.integration.groundworks.GroundworksBulldozerAdapter;
import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController;
import com.piotrek.groundworksbulldozer.vehicle.GradeDatum;
import com.piotrek.groundworksbulldozer.vehicle.GradePass;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-authoritative tracked bulldozer vehicle entity.
 *
 * <p>Integrates:
 * <ul>
 *   <li>Differential track crawler drive physics with in-place pivot steering.</li>
 *   <li>Physical, non-cosmetic grading blade with world-space transform and cutting edge.</li>
 *   <li>Continuous smooth blade lifting/lowering.</li>
 *   <li>Volumetric terrain excavation, depression filling, forward berm pushing, and lateral spillage with Peterwolf's Groundworks.</li>
 *   <li>Live carried material retention and volume conservation.</li>
 * </ul>
 */
public class GroundworksBulldozerEntity extends Entity {
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    // ── Blade Limits ─────────────────────────────────────────────────
    public static final float MIN_BLADE_HEIGHT = -0.60F; // Deep trenching / cutting
    public static final float MAX_BLADE_HEIGHT = 0.80F;  // Raised transport position
    public static final float DEFAULT_BLADE_HEIGHT = 0.0F; // Ground-level grading position
    public static final float LIFT_SPEED = 0.035F;       // Smooth hydraulic travel per tick

    // ── Synched Entity Data ───────────────────────────────────────────
    private static final EntityDataAccessor<Float> TRACK_LEFT_SPEED =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TRACK_RIGHT_SPEED =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLADE_HEIGHT =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BLADE_ANGLE =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CARRIED_MATERIAL_ID =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARRIED_UNITS =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VEHICLE_PITCH =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VEHICLE_ROLL =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_PUSHING =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENGINE_RUNNING =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RIPPER_LIFT =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRADE_DATUM =
            SynchedEntityData.defineId(GroundworksBulldozerEntity.class, EntityDataSerializers.FLOAT);

    // ── Subsystems ───────────────────────────────────────────────────
    protected final BulldozerTrackController trackController = createTrackController();

    protected BulldozerTrackController createTrackController() {
        return new BulldozerTrackController();
    }

    public int getMaxBladeCapacity() {
        return BulldozerBladeController.MAX_BLADE_CAPACITY;
    }

    public double getBladeWidth() {
        return BladeTransform.DEFAULT_BLADE_WIDTH;
    }

    public double getBladeHeightDimension() {
        return BladeTransform.DEFAULT_BLADE_HEIGHT;
    }

    public double getArmLength() {
        return BladeTransform.MOUNT_FORWARD_OFFSET;
    }

    /** Distance the whole machine must travel before a blade command returns to zero. */
    public double getVehicleLength() {
        return trackController.trackLength();
    }

    public int getNumEdgeSamples() {
        return BladeTransform.NUM_EDGE_SAMPLES;
    }

    public BladeTransform computeBladeTransform(
            Vec3 pos,
            float yaw,
            float pitch,
            float roll,
            float height,
            float angle
    ) {
        return BladeTransform.compute(
                pos,
                yaw,
                pitch,
                roll,
                height,
                angle,
                getBladeWidth(),
                getBladeHeightDimension(),
                getArmLength(),
                getNumEdgeSamples()
        );
    }

    protected Item getDropItem() {
        return GroundworksBulldozerMod.BULLDOZER_ITEM;
    }

    public String getVehicleName() {
        return "PETERWOLF'S GROUNDWORKS BULLDOZER";
    }

    public net.minecraft.network.chat.Component getVehicleTitle() {
        return net.minecraft.network.chat.Component.translatable("hud.pw_groundworks_bulldozer.title");
    }

    public float getEnginePitchMultiplier() {
        return 1.0F;
    }

    public SoundEvent getEngineLoopSound() {
        return GroundworksBulldozerMod.ENGINE_LOOP;
    }

    public SoundEvent getEngineLoadSound() {
        return GroundworksBulldozerMod.ENGINE_LOAD;
    }

    protected List<Vec3> getExhaustWorldPositions() {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // Standard bulldozer single stack at X = +0.45m, Z = +0.58m, Y = +2.50m
        Vec3 pos = this.position()
                .add(forward.scale(0.58D))
                .add(right.scale(0.45D))
                .add(up.scale(2.50D));
        return List.of(pos);
    }

    protected void spawnExhaustParticles() {
        float speed = Math.max(Math.abs(getTrackLeftSpeed()), Math.abs(getTrackRightSpeed()));
        boolean pushing = isPushing();
        float fillRatio = (float) getCarriedUnits() / (float) getMaxBladeCapacity();
        float load = Math.max(speed / 0.12F, pushing ? 0.75F + (fillRatio * 0.25F) : 0.08F);

        List<Vec3> exhausts = getExhaustWorldPositions();
        for (Vec3 pos : exhausts) {
            spawnExhaustForPipe(pos, load, pushing);
        }
    }

    private void spawnExhaustForPipe(Vec3 pos, float load, boolean pushing) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));

        if (pushing || load >= 0.85F) {
            // Overload / active earth pushing: dense dark/black diesel smoke billows out and trails back
            this.level().addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 0.05D, pos.z,
                    (Math.random() - 0.5D) * 0.03D - forward.x * 0.02D,
                    0.09D + Math.random() * 0.04D,
                    (Math.random() - 0.5D) * 0.03D - forward.z * 0.02D
            );
            this.level().addParticle(
                    ParticleTypes.SMOKE,
                    pos.x, pos.y + 0.05D, pos.z,
                    (Math.random() - 0.5D) * 0.02D - forward.x * 0.02D,
                    0.07D + Math.random() * 0.03D,
                    (Math.random() - 0.5D) * 0.02D - forward.z * 0.02D
            );
        } else if (load > 0.35F) {
            // Working / driving: medium grey smoke
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(
                        ParticleTypes.SMOKE,
                        pos.x, pos.y + 0.05D, pos.z,
                        (Math.random() - 0.5D) * 0.02D - forward.x * 0.01D,
                        0.06D + Math.random() * 0.02D,
                        (Math.random() - 0.5D) * 0.02D - forward.z * 0.01D
                );
            }
        } else {
            // Idle / low load: soft rhythmic light white smoke puff
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(
                        ParticleTypes.WHITE_SMOKE,
                        pos.x, pos.y + 0.05D, pos.z,
                        (Math.random() - 0.5D) * 0.01D,
                        0.035D + Math.random() * 0.015D,
                        (Math.random() - 0.5D) * 0.01D
                );
            }
        }
    }

    // ── Server-Authoritative State ────────────────────────────────────
    private float bladeHeight = DEFAULT_BLADE_HEIGHT;
    private float bladeAngle = 0.0F;
    private boolean gradePassActive;
    private boolean gradePassLatchOrigin;
    private double gradePassOriginY = Double.NaN;
    private float gradePassCommand;
    private double gradePassTraveled;
    private double gradeCutY;
    private double gradeChassisY;
    private float ripperLift = 0.0F;
    /** Horizontal grade the blade is cutting. Does not follow the chassis down into that cut. */
    private double gradeDatumY = Double.NaN;

    @Nullable
    private BladeTransform previousBladeTransform;
    @Nullable
    private BladeTransform currentBladeTransform;

    private int carriedUnits = 0;
    private GranularMaterial carriedMaterial = GranularMaterial.EMPTY;

    // Operator input buffer
    private float inputThrottle;
    private float inputSteer;
    private float inputBladeLift;
    private float inputBladeTilt;
    private int inputFreshTicks;

    // Telemetry
    private int lastExcavatedUnits;
    private int lastDepositedUnits;

    public GroundworksBulldozerEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRACK_LEFT_SPEED, 0.0F);
        builder.define(TRACK_RIGHT_SPEED, 0.0F);
        builder.define(BLADE_HEIGHT, DEFAULT_BLADE_HEIGHT);
        builder.define(BLADE_ANGLE, 0.0F);
        builder.define(CARRIED_MATERIAL_ID, 0);
        builder.define(CARRIED_UNITS, 0);
        builder.define(VEHICLE_PITCH, 0.0F);
        builder.define(VEHICLE_ROLL, 0.0F);
        builder.define(IS_PUSHING, false);
        builder.define(ENGINE_RUNNING, false);
        builder.define(RIPPER_LIFT, 0.0F);
        builder.define(GRADE_DATUM, 0.0F);
    }

    // ── Input & Control Setters ──────────────────────────────────────

    public void setControlInputs(float throttle, float steer, float bladeLift, float bladeTilt) {
        this.inputThrottle = Mth.clamp(throttle, -1.0F, 1.0F);
        this.inputSteer = Mth.clamp(steer, -1.0F, 1.0F);
        this.inputBladeLift = Mth.clamp(bladeLift, -1.0F, 1.0F);
        this.inputBladeTilt = Mth.clamp(bladeTilt, -1.0F, 1.0F);
        this.inputFreshTicks = 10;
    }

    // ── Ticking & Simulation ─────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            if (this.isEngineRunning()) {
                spawnExhaustParticles();
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        // 1. Process driver input (custom packet + vanilla input fallback)
        Entity driver = this.getControllingPassenger();
        if (driver instanceof ServerPlayer player) {
            var vanillaInput = player.getLastClientInput();
            if (this.inputFreshTicks > 0) {
                this.inputFreshTicks--;
            } else {
                this.inputThrottle = vanillaInput.forward() ? 1.0F : vanillaInput.backward() ? -1.0F : 0.0F;
                this.inputSteer = vanillaInput.left() ? -1.0F : vanillaInput.right() ? 1.0F : 0.0F;
                this.inputBladeLift = 0.0F;
                this.inputBladeTilt = 0.0F;
            }

            // Always allow vanilla forward/backward override if packet throttle is 0
            if (Math.abs(this.inputThrottle) < 0.01F) {
                if (vanillaInput.forward()) this.inputThrottle = 1.0F;
                else if (vanillaInput.backward()) this.inputThrottle = -1.0F;
            }
            if (Math.abs(this.inputSteer) < 0.01F) {
                if (vanillaInput.left()) this.inputSteer = -1.0F;
                else if (vanillaInput.right()) this.inputSteer = 1.0F;
            }
        } else {
            this.inputThrottle = 0.0F;
            this.inputSteer = 0.0F;
            this.inputBladeLift = 0.0F;
            this.inputBladeTilt = 0.0F;
            this.inputFreshTicks = 0;
        }

        boolean hasDriver = driver != null;
        this.entityData.set(ENGINE_RUNNING, hasDriver);

        // 2. Smooth blade & ripper kinematics
        boolean adjustingBlade = Math.abs(inputBladeLift) > 0.01F;
        if (adjustingBlade) {
            bladeHeight = Mth.clamp(bladeHeight + inputBladeLift * LIFT_SPEED, MIN_BLADE_HEIGHT, MAX_BLADE_HEIGHT);
            // A new lever setting starts a new ramp. Arrows do not tilt the moldboard.
            gradePassCommand = bladeHeight;
            gradePassTraveled = 0.0D;
            gradePassActive = Math.abs(bladeHeight) > 0.02F;
            gradePassLatchOrigin = true;
            bladeAngle = 0.0F;
        }

        // Ripper claw controls (Arrow Left = Raise/Transport, Arrow Right = Lower/Penetrate)
        if (inputBladeTilt > 0.01F) {
            ripperLift = Mth.clamp(ripperLift + 0.035F, 0.0F, 1.0F);
        } else if (inputBladeTilt < -0.01F) {
            ripperLift = Mth.clamp(ripperLift - 0.035F, 0.0F, 1.0F);
        }

        // 3. Track kinematics and differential steering
        boolean heavyLoad = carriedUnits > (getMaxBladeCapacity() / 2);
        BulldozerTrackController.TrackState trackState = trackController.tick(
                serverLevel,
                this.position(),
                this.getYRot(),
                inputThrottle,
                inputSteer,
                this.onGround(),
                heavyLoad
        );

        // Apply turning
        if (Math.abs(trackState.yawDeltaDegrees()) > 0.001F) {
            this.setYRot(this.getYRot() + trackState.yawDeltaDegrees());
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
        }

        this.entityData.set(TRACK_LEFT_SPEED, trackState.leftSpeed());
        this.entityData.set(TRACK_RIGHT_SPEED, trackState.rightSpeed());
        this.entityData.set(VEHICLE_PITCH, trackState.pitch());
        this.entityData.set(VEHICLE_ROLL, trackState.roll());

        // Apply forward translation and realistic ground conforming / gravity
        Vec3 motion = trackState.forwardDelta();
        applyGradePass(trackState, adjustingBlade);
        double targetGroundY = gradeChassisY;
        double currentY = this.getY();

        if (currentY > targetGroundY + 0.08D) {
            // Suspended in air: fall down towards actual track support
            double drop = Math.min(0.50D, (currentY - targetGroundY) * 0.50D + 0.08D);
            motion = motion.add(0.0D, -drop, 0.0D);
        } else if (currentY < targetGroundY - 0.04D) {
            // Tracks penetrating ground support: conform upward smoothly without launching into the air
            double rise = Math.min(0.08D, (targetGroundY - currentY) * 0.25D);
            motion = motion.add(0.0D, rise, 0.0D);
        } else {
            motion = motion.add(0.0D, -0.02D, 0.0D); // Keep tracks firmly grounded
        }

        // 4. Update Blade Transforms and Execute Terrain Grading BEFORE Vehicle Motion.
        // Grade position, not the pitched nose: pitch must not deepen the cut.
        Vec3 gradePos = new Vec3(this.getX(), gradeCutY, this.getZ());
        BladeTransform currentTransform = computeBladeTransform(
                gradePos,
                this.getYRot(),
                0.0F,
                trackState.roll(),
                0.0F,
                bladeAngle
        );

        if (previousBladeTransform == null) {
            previousBladeTransform = currentTransform;
        }

        Vec3 targetPos = this.position().add(motion);
        Vec3 gradeTarget = new Vec3(targetPos.x, gradeCutY, targetPos.z);
        BladeTransform nextTransform = computeBladeTransform(
                gradeTarget,
                this.getYRot(),
                0.0F,
                trackState.roll(),
                0.0F,
                bladeAngle
        );

        // 5. Authoritative Blade Terrain Interaction
        GroundworksBulldozerAdapter adapter = GroundworksBulldozerAdapter.of(serverLevel);
        BladeTickResult bladeResult = BulldozerBladeController.tick(
                adapter,
                previousBladeTransform,
                nextTransform,
                carriedUnits,
                carriedMaterial,
                getMaxBladeCapacity()
        );

        this.carriedUnits = bladeResult.carriedUnitsAfter();
        this.carriedMaterial = bladeResult.carriedMaterialAfter();
        this.lastExcavatedUnits = bladeResult.unitsExcavated();
        this.lastDepositedUnits = bladeResult.unitsDeposited();

        // 6. Apply Vehicle Movement (now unobstructed by excavated/pushed loose material)
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        // Compact and level terrain under the heavy steel crawler tracks while driving
        if (Math.abs(trackState.leftSpeed()) > 0.005F || Math.abs(trackState.rightSpeed()) > 0.005F) {
            levelTerrainUnderTracks(serverLevel, adapter, trackState);
        }

        previousBladeTransform = computeBladeTransform(
                new Vec3(this.getX(), gradeCutY, this.getZ()),
                this.getYRot(),
                0.0F,
                trackState.roll(),
                0.0F,
                bladeAngle
        );
        currentBladeTransform = previousBladeTransform;

        // Force position synchronization to passengers and tracking clients while driving
        if (hasDriver || Math.abs(trackState.leftSpeed()) > 0.001F || Math.abs(trackState.rightSpeed()) > 0.001F || Math.abs(trackState.yawDeltaDegrees()) > 0.01F) {
            this.needsSync = true;
            }

        // Spawn visual dust/shaving particles & scraping audio when actively pushing terrain
        if (bladeResult.isPushing() || bladeResult.unitsExcavated() > 0) {
            spawnWorkingParticles(serverLevel, currentBladeTransform, this.carriedMaterial);

            if (this.tickCount % 4 == 0) {
                var sound = (this.carriedMaterial == GranularMaterialRegistry.SAND) ? SoundEvents.SAND_BREAK :
                        (this.carriedMaterial == GranularMaterialRegistry.GRAVEL) ? SoundEvents.GRAVEL_BREAK :
                                SoundEvents.ROOTED_DIRT_BREAK;
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, 0.7F, 0.85F);
            }
        } else if (bladeResult.unitsDeposited() > 0) {
            // Reversing discharge sound: settling of the whole heap
            var placeSound = (this.carriedMaterial == GranularMaterialRegistry.SAND) ? SoundEvents.SAND_PLACE :
                    (this.carriedMaterial == GranularMaterialRegistry.GRAVEL) ? SoundEvents.GRAVEL_PLACE :
                            SoundEvents.ROOTED_DIRT_PLACE;
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), placeSound, SoundSource.BLOCKS, 0.9F, 0.85F);
        }

        // 6. Update Synched Entity Data
        this.entityData.set(TRACK_LEFT_SPEED, trackState.leftSpeed());
        this.entityData.set(TRACK_RIGHT_SPEED, trackState.rightSpeed());
        this.entityData.set(BLADE_HEIGHT, bladeHeight);
        this.entityData.set(BLADE_ANGLE, bladeAngle);
        this.entityData.set(CARRIED_MATERIAL_ID, carriedMaterial != null ? carriedMaterial.id() : 0);
        this.entityData.set(CARRIED_UNITS, carriedUnits);
        this.entityData.set(VEHICLE_PITCH, trackState.pitch());
        this.entityData.set(VEHICLE_ROLL, trackState.roll());
        this.entityData.set(IS_PUSHING, bladeResult.isPushing());
        this.entityData.set(RIPPER_LIFT, ripperLift);

        tickRipper(serverLevel, hasDriver, trackState);
    }

    private void spawnWorkingParticles(ServerLevel level, BladeTransform transform, GranularMaterial mat) {
        var block = (mat != null && mat.sourceBlock() != null) ? mat.sourceBlock() : Blocks.DIRT;
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());

        for (Vec3 pt : transform.cuttingEdgePoints()) {
            level.sendParticles(
                    particle,
                    pt.x, pt.y + 0.12D, pt.z,
                    2,
                    0.15D, 0.08D, 0.15D,
                    0.05D
            );
        }

        Vec3 front = transform.cuttingEdgeCenter().add(transform.forward().scale(0.65D));
        level.sendParticles(
                particle,
                front.x, front.y + 0.25D, front.z,
                4,
                0.5D, 0.15D, 0.5D,
                0.06D
        );
    }

    public float getRipperLift() {
        return this.entityData.get(RIPPER_LIFT);
    }

    public void setRipperLift(float lift) {
        this.ripperLift = Mth.clamp(lift, 0.0F, 1.0F);
        this.entityData.set(RIPPER_LIFT, this.ripperLift);
    }

    protected void levelTerrainUnderTracks(
            ServerLevel level,
            GroundworksBulldozerAdapter adapter,
            BulldozerTrackController.TrackState trackState
    ) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

        double trackGauge = getBladeWidth() * 0.65D;
        double trackLength = getArmLength() * 1.35D;
        double halfGauge = trackGauge * 0.5D;
        double halfLength = trackLength * 0.5D;

        double[] zFractions = {-1.0D, -0.5D, 0.0D, 0.5D, 1.0D};
        double[] xOffsets = {-halfGauge, halfGauge};

        double pitchRad = Math.toRadians(trackState.pitch());
        double rollRad = Math.toRadians(trackState.roll());

        Set<net.minecraft.core.BlockPos> leveledPositions = new HashSet<>();

        for (double xOff : xOffsets) {
            for (double zFrac : zFractions) {
                double zDist = zFrac * halfLength;
                // Positive pitch is nose down (front shoe lower). Positive roll is left side high,
                // so the right shoe (positive xOff) must sit lower, not higher.
                double trackShoeY = this.getY() - (Math.sin(pitchRad) * zDist) - (Math.sin(rollRad) * xOff);
                // Tracks compact bumps. They do not carve a second trench under a pitched nose.
                trackShoeY = Math.max(trackShoeY, this.getY() - 0.20D);

                Vec3 trackPt = this.position()
                        .add(forward.scale(zDist))
                        .add(right.scale(xOff));

                net.minecraft.core.BlockPos trackBlockPos = net.minecraft.core.BlockPos.containing(trackPt.x, trackShoeY + 0.1D, trackPt.z);

                for (net.minecraft.core.BlockPos pos : List.of(trackBlockPos, trackBlockPos.below())) {
                    if (leveledPositions.add(pos) && adapter.isDiggable(pos)) {
                        GranularMaterial mat = adapter.getMaterial(pos);
                        if (mat == GranularMaterial.EMPTY) {
                            continue;
                        }

                        // Compact/shave protruding loose microvoxels above track shoe grade:
                        ExcavationResult cut = adapter.excavateMicrovoxelsAbove(pos, trackShoeY, 128, mat);
                        if (cut.success() && cut.unitsRemoved() > 0) {
                            net.minecraft.core.BlockPos underPos = pos.below();
                            if (adapter.isDiggable(underPos)) {
                                adapter.fillDepressionBelow(underPos, trackShoeY, mat, cut.unitsRemoved());
                            }
                            adapter.markSimulate(pos);
                        }
                    }
                }
            }
        }
    }

    protected void tickRipper(ServerLevel serverLevel, boolean hasDriver, BulldozerTrackController.TrackState trackState) {}

    // ── Passenger Interaction & Seating ──────────────────────────────

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return other != null && !this.hasPassenger(other);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        return false;
    }

    @Override
    public float maxUpStep() {
        // When blade is raised in transport position (> 0.40m), allow climbing steps.
        // When blade is down/grading (<= 0.40m), keep step height at 0.6F so the bulldozer
        // stays firmly on the ground and pushes loose material rather than riding up on it.
        return this.bladeHeight > 0.40F ? 1.0F : 0.6F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        // Shift + Right-Click with empty hand: retrieve / dismantle bulldozer into inventory
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(new ItemStack(getDropItem()));
                }
                this.level().playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F
                );
                this.discard();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide()) {
            if (this.getPassengers().isEmpty()) {
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && this.getPassengers().isEmpty();
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    public boolean isDriver(Entity entity) {
        return entity != null && entity == this.getControllingPassenger();
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), 1.0F));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // 0.25m behind vehicle center, 1.10m elevation inside cab
        return forward.scale(-0.25D).add(up.scale(1.10D));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        return this.position().add(left.scale(2.2D)).add(0.0D, 0.25D, 0.0D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        }

        level.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.8F, 1.1F
        );

        if (source.getEntity() instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(level, getDropItem());
            }
            this.ejectPassengers();
            this.discard();
            return true;
        }

        // Generic damage, explosions, /kill command
        this.ejectPassengers();
        this.discard();
        return true;
    }

    // ── Getters for Physical Blade State ─────────────────────────────

    public float getBladeHeight() {
        return this.entityData.get(BLADE_HEIGHT);
    }

    public void setBladeHeight(float height) {
        this.bladeHeight = Mth.clamp(height, MIN_BLADE_HEIGHT, MAX_BLADE_HEIGHT);
        this.entityData.set(BLADE_HEIGHT, this.bladeHeight);
    }

    public float getBladeAngle() {
        return this.entityData.get(BLADE_ANGLE);
    }

    public void setBladeAngle(float angle) {
        this.bladeAngle = angle;
        this.entityData.set(BLADE_ANGLE, angle);
    }

    @Nullable
    public BladeTransform getPreviousBladeTransform() {
        return previousBladeTransform;
    }

    @Nullable
    public BladeTransform getCurrentBladeTransform() {
        return currentBladeTransform;
    }

    public void setCurrentBladeTransform(BladeTransform transform) {
        this.currentBladeTransform = transform;
    }

    public void setPreviousBladeTransform(BladeTransform transform) {
        this.previousBladeTransform = transform;
    }

    public AABB getBladeCollisionBox() {
        if (currentBladeTransform != null) {
            return currentBladeTransform.boundingBox();
        }
        return this.getBoundingBox();
    }

    public int getCarriedUnits() {
        return this.entityData.get(CARRIED_UNITS);
    }

    public void setCarriedUnits(int units) {
        this.carriedUnits = Math.max(0, Math.min(units, getMaxBladeCapacity()));
        this.entityData.set(CARRIED_UNITS, this.carriedUnits);
    }

    public int getCarriedMaterialId() {
        return this.entityData.get(CARRIED_MATERIAL_ID);
    }

    public GranularMaterial getCarriedMaterial() {
        return this.carriedMaterial;
    }

    public void setCarriedMaterial(GranularMaterial material) {
        this.carriedMaterial = material != null ? material : GranularMaterial.EMPTY;
        this.entityData.set(CARRIED_MATERIAL_ID, this.carriedMaterial.id());
    }

    public float getTrackLeftSpeed() {
        return this.entityData.get(TRACK_LEFT_SPEED);
    }

    public float getTrackRightSpeed() {
        return this.entityData.get(TRACK_RIGHT_SPEED);
    }

    public float getVehiclePitch() {
        return this.entityData.get(VEHICLE_PITCH);
    }

    public float getVehicleRoll() {
        return this.entityData.get(VEHICLE_ROLL);
    }

    public double getGradeDatumY() {
        return this.entityData.get(GRADE_DATUM);
    }

    /**
     * Blade height the model and HUD should show. Once the tracks have settled
     * onto the slot floor, this returns 0 instead of the lever setting, so the
     * blade is not drawn another 60 cm under the floor.
     */
    public float getVisualBladeHeight() {
        double edge = GradeDatum.edgeY(
                getGradeDatumY(),
                this.getY(),
                bladeHeight
        );
        return (float) Mth.clamp(edge - this.getY(), MIN_BLADE_HEIGHT, MAX_BLADE_HEIGHT);
    }

    /**
     * A lever command becomes a ramp over {@link #getVehicleLength()}.
     * Arrows only set the depth. The bite angle appears while driving and returns to zero with the lever.
     */
    private void applyGradePass(BulldozerTrackController.TrackState trackState, boolean adjustingBlade) {
        double support = trackState.groundSupportY();
        double saneSupport = support;
        if (!Double.isFinite(saneSupport)) {
            saneSupport = this.getY();
        } else if (saneSupport > this.getY() + 2.5D) {
            // Limit upward support snapping (cannot climb ledges higher than max step)
            saneSupport = this.getY() + 2.5D;
        } else if (saneSupport < this.getY() - 16.0D) {
            // Bound extreme bottom fall to search window
            saneSupport = this.getY() - 16.0D;
        }

        // Reversing or backing away: cancel any active forward grade cut pass so the machine
        // does not lock itself to a forward ramp elevation and levitate above slopes.
        boolean isReversing = inputThrottle < -0.01F
                || trackState.leftSpeed() < -0.01F
                || trackState.rightSpeed() < -0.01F;
        if (isReversing) {
            gradePassActive = false;
            gradePassOriginY = Double.NaN;
            gradePassTraveled = 0.0D;
            gradeDatumY = Double.NaN;
            bladeAngle = 0.0F;
        }

        // If the machine drives off the grade (drops significantly below the cut floor),
        // end the pass so it follows the actual slope down rather than planing the sky.
        if (gradePassActive && support < gradePassOriginY + gradePassCommand - 1.0D) {
            gradePassActive = false;
            gradePassOriginY = Double.NaN;
        }

        if (gradePassLatchOrigin) {
            gradePassOriginY = saneSupport;
            gradePassLatchOrigin = false;
        }
        if (adjustingBlade || !gradePassActive || Double.isNaN(gradePassOriginY)) {
            if (!adjustingBlade) {
                bladeAngle = 0.0F;
            }
            double gradeY = holdGradeDatum(saneSupport);
            gradeChassisY = saneSupport;
            gradeCutY = gradeY + bladeHeight;
            return;
        }
        if (inputThrottle > 0.05F) {
            Vec3 step = trackState.forwardDelta();
            gradePassTraveled += Math.sqrt(step.x * step.x + step.z * step.z);
        }
        double length = Math.max(1.0D, getVehicleLength());
        double tChassis = GradePass.chassisProgress(gradePassTraveled, length);
        double tBlade = GradePass.bladeProgress(gradePassTraveled, getArmLength(), length);
        if (tChassis >= 1.0D - 1.0E-6D) {
            gradePassActive = false;
            bladeHeight = 0.0F;
            bladeAngle = 0.0F;
            gradeDatumY = gradePassOriginY + gradePassCommand;
            this.entityData.set(GRADE_DATUM, (float) gradeDatumY);
            gradeChassisY = gradeDatumY;
            gradeCutY = gradeDatumY;
            return;
        }
        bladeHeight = GradePass.remainingHeight(gradePassCommand, tChassis);
        bladeAngle = GradePass.biteDegrees(gradePassCommand, length, tChassis);
        gradeChassisY = GradePass.elevation(gradePassOriginY, gradePassCommand, tChassis);
        gradeCutY = GradePass.elevation(gradePassOriginY, gradePassCommand, tBlade);
        this.entityData.set(GRADE_DATUM, (float) gradeChassisY);
    }

    /**
     * Latch the grade when the blade is raised or the machine climbs.
     * A lowered blade does not follow the chassis down into the slot it just cut.
     */
    private double holdGradeDatum(double groundSupportY) {
        gradeDatumY = GradeDatum.hold(
                gradeDatumY,
                this.getY(),
                groundSupportY,
                bladeHeight
        );
        this.entityData.set(GRADE_DATUM, (float) gradeDatumY);
        return gradeDatumY;
    }

    public boolean isPushing() {
        return this.entityData.get(IS_PUSHING);
    }

    public boolean isEngineRunning() {
        return this.entityData.get(ENGINE_RUNNING);
    }

    public int getLastExcavatedUnits() {
        return lastExcavatedUnits;
    }

    public int getLastDepositedUnits() {
        return lastDepositedUnits;
    }

    // ── NBT Persistence (Save / Load) ────────────────────────────────

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        BulldozerState state = BulldozerState.load(input);
        this.bladeHeight = state.bladeHeight();
        this.bladeAngle = state.bladeAngle();
        this.carriedUnits = state.carriedUnits();
        this.carriedMaterial = GranularMaterialRegistry.byId(state.carriedMaterialId());

        trackController.setTrackSpeeds(state.leftTrackSpeed(), state.rightTrackSpeed());
        trackController.setOrientation(state.vehiclePitch(), state.vehicleRoll());

        this.entityData.set(BLADE_HEIGHT, this.bladeHeight);
        this.entityData.set(BLADE_ANGLE, this.bladeAngle);
        this.entityData.set(CARRIED_UNITS, this.carriedUnits);
        this.entityData.set(CARRIED_MATERIAL_ID, state.carriedMaterialId());
        this.entityData.set(TRACK_LEFT_SPEED, state.leftTrackSpeed());
        this.entityData.set(TRACK_RIGHT_SPEED, state.rightTrackSpeed());
        this.entityData.set(VEHICLE_PITCH, state.vehiclePitch());
        this.entityData.set(VEHICLE_ROLL, state.vehicleRoll());
        this.gradePassActive = input.getBooleanOr("GradePassActive", false);
        this.gradePassCommand = input.getFloatOr("GradePassCommand", 0.0F);
        this.gradePassTraveled = input.getDoubleOr("GradePassTraveled", 0.0D);
        this.gradePassOriginY = input.getDoubleOr("GradePassOriginY", Double.NaN);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        BulldozerState state = new BulldozerState(
                this.bladeHeight,
                this.bladeAngle,
                this.carriedUnits,
                this.carriedMaterial != null ? this.carriedMaterial.id() : 0,
                this.trackController.leftTrackSpeed(),
                this.trackController.rightTrackSpeed(),
                this.trackController.vehiclePitch(),
                this.trackController.vehicleRoll()
        );
        state.save(output);
        output.putBoolean("GradePassActive", this.gradePassActive);
        output.putFloat("GradePassCommand", this.gradePassCommand);
        output.putDouble("GradePassTraveled", this.gradePassTraveled);
        if (!Double.isNaN(this.gradePassOriginY)) {
            output.putDouble("GradePassOriginY", this.gradePassOriginY);
        }
    }
}
