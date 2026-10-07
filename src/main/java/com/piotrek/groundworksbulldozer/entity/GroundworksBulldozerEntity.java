package com.piotrek.groundworksbulldozer.entity;

import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController;
import com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.BladeTickResult;
import com.piotrek.groundworksbulldozer.integration.groundworks.GroundworksBulldozerAdapter;
import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController;
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
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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

    // ── Subsystems ───────────────────────────────────────────────────
    private final BulldozerTrackController trackController = new BulldozerTrackController();

    // ── Server-Authoritative State ────────────────────────────────────
    private float bladeHeight = DEFAULT_BLADE_HEIGHT;
    private float bladeAngle = 0.0F;

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
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this, 3);
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

        // 2. Smooth blade height & angle kinematics
        if (Math.abs(inputBladeLift) > 0.01F) {
            bladeHeight = Mth.clamp(bladeHeight + inputBladeLift * LIFT_SPEED, MIN_BLADE_HEIGHT, MAX_BLADE_HEIGHT);
        }
        // Blade angle naturally pitches forward slightly as it penetrates into the ground
        bladeAngle = bladeHeight * 12.0F + (inputBladeTilt * 8.0F);

        // 3. Track kinematics and differential steering
        boolean heavyLoad = carriedUnits > (BulldozerBladeController.MAX_BLADE_CAPACITY / 2);
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

        // Apply forward translation and gravity
        Vec3 motion = trackState.forwardDelta();
        if (!this.onGround()) {
            motion = motion.add(0.0D, -0.08D, 0.0D);
        } else {
            motion = motion.add(0.0D, -0.02D, 0.0D); // Keep tracks firmly grounded
        }

        // 4. Update Blade Transforms and Execute Terrain Grading BEFORE Vehicle Motion
        BladeTransform currentTransform = BladeTransform.compute(
                this.position(),
                this.getYRot(),
                trackState.pitch(),
                trackState.roll(),
                bladeHeight,
                bladeAngle
        );

        if (previousBladeTransform == null) {
            previousBladeTransform = currentTransform;
        }

        Vec3 targetPos = this.position().add(motion);
        BladeTransform nextTransform = BladeTransform.compute(
                targetPos,
                this.getYRot(),
                trackState.pitch(),
                trackState.roll(),
                bladeHeight,
                bladeAngle
        );

        // 5. Authoritative Blade Terrain Interaction
        GroundworksBulldozerAdapter adapter = GroundworksBulldozerAdapter.of(serverLevel);
        BladeTickResult bladeResult = BulldozerBladeController.tick(
                adapter,
                previousBladeTransform,
                nextTransform,
                carriedUnits,
                carriedMaterial
        );

        this.carriedUnits = bladeResult.carriedUnitsAfter();
        this.carriedMaterial = bladeResult.carriedMaterialAfter();
        this.lastExcavatedUnits = bladeResult.unitsExcavated();
        this.lastDepositedUnits = bladeResult.unitsDeposited();

        // 6. Apply Vehicle Movement (now unobstructed by excavated/pushed loose material)
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);

        previousBladeTransform = BladeTransform.compute(
                this.position(),
                this.getYRot(),
                trackState.pitch(),
                trackState.roll(),
                bladeHeight,
                bladeAngle
        );
        currentBladeTransform = previousBladeTransform;

        // Force position synchronization to passengers and tracking clients while driving
        if (hasDriver || Math.abs(trackState.leftSpeed()) > 0.001F || Math.abs(trackState.rightSpeed()) > 0.001F || Math.abs(trackState.yawDeltaDegrees()) > 0.01F) {
            this.syncPosition = true;
            this.needsSync = true;
            this.syncVelocity = true;
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
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        // Shift + Right-Click with empty hand: retrieve / dismantle bulldozer into inventory
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!this.level().isClientSide() && this.getPassengers().isEmpty()) {
                if (!player.getAbilities().instabuild) {
                    player.getInventory().add(new ItemStack(GroundworksBulldozerMod.BULLDOZER_ITEM));
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
                this.spawnAtLocation(level, GroundworksBulldozerMod.BULLDOZER_ITEM);
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
        this.carriedUnits = Math.max(0, Math.min(units, BulldozerBladeController.MAX_BLADE_CAPACITY));
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
    }
}
