package com.piotrek.groundworksbulldozer.item;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Item used to deploy the tracked bulldozer vehicle in the world.
 */
public class BulldozerItem extends Item {

    public BulldozerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos spawnPos = clickedPos.relative(clickedFace);

        Vec3 spawnVec = new Vec3(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D
        );

        AABB bounds = GroundworksBulldozerMod.BULLDOZER.getDimensions().makeBoundingBox(spawnVec);
        if (!level.noCollision(bounds)) {
            bounds = bounds.move(0.0D, 0.5D, 0.0D);
            if (!level.noCollision(bounds)) {
                return InteractionResult.FAIL;
            }
            spawnVec = spawnVec.add(0.0D, 0.5D, 0.0D);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        GroundworksBulldozerEntity bulldozer = GroundworksBulldozerMod.BULLDOZER.create(
                serverLevel,
                EntitySpawnReason.SPAWN_ITEM_USE
        );

        if (bulldozer != null) {
            float playerYaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
            bulldozer.snapTo(spawnVec.x, spawnVec.y, spawnVec.z, playerYaw, 0.0F);
            bulldozer.setYHeadRot(playerYaw);
            bulldozer.setYBodyRot(playerYaw);

            serverLevel.addFreshEntity(bulldozer);

            ItemStack itemStack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}
