package com.piotrek.groundworksbulldozer.entity;

import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.vehicle.BulldozerTrackController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Heavy mining crawler bulldozer (Cat D10T / D8T class) with triangular elevated sprocket tracks,
 * massive 6.0m Semi-U moldboard blade with top rock guard, dual lift cylinders, and heavy rear ripper.
 *
 * <p>Twice as large as the standard crawler bulldozer:
 * <ul>
 *   <li>6.0m blade width with 17 cutting edge contact points.</li>
 *   <li>6144 units (12 full Minecraft blocks / 12.0 m³) moldboard surcharge capacity.</li>
 *   <li>Triangular high-drive undercarriage with elevated rear drive sprocket.</li>
 *   <li>Deep Cat V12 diesel engine acoustics.</li>
 * </ul>
 */
public class HeavyBulldozerEntity extends GroundworksBulldozerEntity {

    public static final int HEAVY_BLADE_CAPACITY = 24576; // 48.0 full blocks of granular material (48.0 m³ - massive rolling surcharge!)
    public static final double HEAVY_BLADE_WIDTH = 6.0D;
    public static final double HEAVY_BLADE_HEIGHT_DIM = 1.8D;
    public static final double HEAVY_ARM_LENGTH = 4.2D;
    public static final int HEAVY_NUM_EDGE_SAMPLES = 17;

    public HeavyBulldozerEntity(EntityType<? extends HeavyBulldozerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected BulldozerTrackController createTrackController() {
        // High drawbar tractive effort: 4.2m gauge, 6.2m length, 0.20D max speed, high torque acceleration (0.080D), maintains 90% power under heavy push
        return new BulldozerTrackController(4.2D, 6.2D, 0.20D, 0.080D, 0.100D, 0.38F, 0.90D);
    }

    @Override
    public int getMaxBladeCapacity() {
        return HEAVY_BLADE_CAPACITY;
    }

    @Override
    public double getBladeWidth() {
        return HEAVY_BLADE_WIDTH;
    }

    @Override
    public double getBladeHeightDimension() {
        return HEAVY_BLADE_HEIGHT_DIM;
    }

    @Override
    public double getArmLength() {
        return HEAVY_ARM_LENGTH;
    }

    @Override
    public int getNumEdgeSamples() {
        return HEAVY_NUM_EDGE_SAMPLES;
    }

    @Override
    public float getMinBladeHeight() {
        return -0.80F; // Controlled, clean deep plunge for heavy mining bulldozer (1.8m moldboard)
    }

    @Override
    public float getMaxBladeHeight() {
        return 1.20F;
    }

    @Override
    public float maxUpStep() {
        // Allow rolling over 1-block granular material heaps without getting stuck,
        // and 2.5m obstacle clearance when raised in transport position.
        return this.getBladeHeight() > 0.40F ? 2.5F : 1.25F;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // Elevated panoramic operator cab: 0.50m rear of center, 2.35m elevation
        return forward.scale(-0.50D).add(up.scale(2.35D));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 left = new Vec3(-Math.cos(yawRad), 0.0D, -Math.sin(yawRad));
        // Steps down to the side catwalk / ladder
        return this.position().add(left.scale(3.4D)).add(0.0D, 0.3D, 0.0D);
    }

    @Override
    protected Item getDropItem() {
        return GroundworksBulldozerMod.HEAVY_BULLDOZER_ITEM;
    }

    @Override
    public String getVehicleName() {
        return "PETERWOLF'S HEAVY BULLDOZER (D10)";
    }

    @Override
    public net.minecraft.network.chat.Component getVehicleTitle() {
        return net.minecraft.network.chat.Component.translatable("hud.pw_groundworks_bulldozer.title_heavy");
    }

    @Override
    public SoundEvent getEngineLoopSound() {
        return GroundworksBulldozerMod.HEAVY_ENGINE_LOOP;
    }

    @Override
    public SoundEvent getEngineLoadSound() {
        return GroundworksBulldozerMod.HEAVY_ENGINE_LOAD;
    }

    @Override
    protected List<Vec3> getExhaustWorldPositions() {
        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // Twin exhaust stacks at X = ±0.39m, Z = +1.11m, Y = +4.12m
        Vec3 center = this.position()
                .add(forward.scale(1.11D))
                .add(up.scale(4.12D));
        return List.of(
                center.subtract(right.scale(0.39D)),
                center.add(right.scale(0.39D))
        );
    }

    @Override
    public float getEnginePitchMultiplier() {
        return 0.85F; // Deeper rumble for Cat C27 V12 diesel engine
    }

    @Override
    protected void tickRipper(ServerLevel serverLevel, boolean hasDriver, BulldozerTrackController.TrackState trackState) {
        float lift = getRipperLift();
        if (lift < 0.35F) {
            return;
        }

        // Only rips when driving (has movement or forward throttle)
        double speed = trackState.forwardDelta().length();
        if (speed < 0.005D) {
            return;
        }

        double yawRad = Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        // Ripper shank tip position: 3.8m behind vehicle center, reaching down into ground
        double ripElevation = 0.25D - (lift * 0.95D);
        Vec3 clawPos = this.position()
                .subtract(forward.scale(3.8D))
                .add(up.scale(ripElevation));

        BlockPos targetPos = BlockPos.containing(clawPos.x, clawPos.y, clawPos.z);
        BlockPos belowPos = targetPos.below();

        for (BlockPos pos : List.of(targetPos, belowPos)) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            // Check if stone/rock
            boolean isStone = state.is(BlockTags.BASE_STONE_OVERWORLD)
                    || state.is(Blocks.STONE)
                    || state.is(Blocks.DEEPSLATE)
                    || state.is(Blocks.ANDESITE)
                    || state.is(Blocks.DIORITE)
                    || state.is(Blocks.GRANITE)
                    || state.is(Blocks.TUFF)
                    || state.is(Blocks.COBBLESTONE)
                    || state.is(Blocks.SANDSTONE)
                    || state.is(Blocks.RED_SANDSTONE)
                    || state.is(Blocks.TERRACOTTA)
                    || state.is(Blocks.BLACKSTONE)
                    || state.is(Blocks.BASALT);

            // Check if permafrost/frozen ground
            boolean isPermafrost = state.is(BlockTags.ICE)
                    || state.is(Blocks.PACKED_ICE)
                    || state.is(Blocks.BLUE_ICE)
                    || state.is(Blocks.ICE)
                    || state.is(Blocks.FROSTED_ICE)
                    || state.is(Blocks.SNOW_BLOCK)
                    || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.PACKED_MUD)
                    || state.is(Blocks.MUD);

            if (isStone) {
                // Shatter rock into loose granular gravel
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                GroundworksApi.depositWithOverflow(serverLevel, pos, GranularMaterialRegistry.GRAVEL, 512);
                GroundworksApi.markForSimulation(serverLevel, pos);

                serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        clawPos.x, clawPos.y + 0.1D, clawPos.z,
                        4, 0.2D, 0.15D, 0.2D, 0.15D
                );
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        clawPos.x, clawPos.y + 0.1D, clawPos.z,
                        5, 0.25D, 0.2D, 0.25D, 0.1D
                );

                if (this.tickCount % 3 == 0) {
                    serverLevel.playSound(null, clawPos.x, clawPos.y, clawPos.z,
                            SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.0F, 0.75F);
                }
                break;
            } else if (isPermafrost) {
                // Tear through permafrost into loose granular dirt
                serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                GroundworksApi.depositWithOverflow(serverLevel, pos, GranularMaterialRegistry.DIRT, 512);
                GroundworksApi.markForSimulation(serverLevel, pos);

                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        clawPos.x, clawPos.y + 0.1D, clawPos.z,
                        6, 0.3D, 0.2D, 0.3D, 0.1D
                );

                if (this.tickCount % 3 == 0) {
                    serverLevel.playSound(null, clawPos.x, clawPos.y, clawPos.z,
                            SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.9F, 0.8F);
                }
                break;
            }
        }
    }
}
