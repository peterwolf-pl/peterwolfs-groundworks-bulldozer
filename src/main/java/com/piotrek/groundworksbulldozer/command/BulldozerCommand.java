package com.piotrek.groundworksbulldozer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Debug and operator command for Peterwolf's Groundworks Bulldozer.
 */
public final class BulldozerCommand {

    private BulldozerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bulldozer")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    GroundworksBulldozerEntity dozer = GroundworksBulldozerMod.BULLDOZER.create(
                                            source.getLevel(), EntitySpawnReason.COMMAND
                                    );
                                    if (dozer != null) {
                                        dozer.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
                                        source.getLevel().addFreshEntity(dozer);
                                        player.startRiding(dozer);
                                        source.sendSuccess(() -> Component.literal("§a[Bulldozer] Spawned and boarded bulldozer."), true);
                                        return 1;
                                    }
                                    return 0;
                                })
                        )
                        .then(Commands.literal("blade")
                                .then(Commands.argument("height", FloatArgumentType.floatArg(-0.60F, 0.80F))
                                        .executes(ctx -> {
                                            float height = FloatArgumentType.getFloat(ctx, "height");
                                            CommandSourceStack source = ctx.getSource();
                                            ServerPlayer player = source.getPlayerOrException();
                                            if (player.getVehicle() instanceof GroundworksBulldozerEntity dozer) {
                                                dozer.setBladeHeight(height);
                                                source.sendSuccess(() -> Component.literal(
                                                        String.format("§a[Bulldozer] Blade height set to %.2f m", height)
                                                ), false);
                                                return 1;
                                            }
                                            source.sendFailure(Component.literal("§cMust be driving a bulldozer."));
                                            return 0;
                                        })
                                )
                        )
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    if (player.getVehicle() instanceof GroundworksBulldozerEntity dozer) {
                                        source.sendSuccess(() -> Component.literal(String.format(
                                                "§e[Bulldozer Info]§r\n" +
                                                        "- Blade Height: §b%.2f m§r\n" +
                                                        "- Blade Angle: §b%.1f°§r\n" +
                                                        "- Carried Material: §a%s§r (%d units / %.3f m³)\n" +
                                                        "- Track Speeds: L=%.3f, R=%.3f\n" +
                                                        "- Last Excavated: %d units, Deposited: %d units\n" +
                                                        "- Ground Pitch: %.1f°, Roll: %.1f°",
                                                dozer.getBladeHeight(),
                                                dozer.getBladeAngle(),
                                                dozer.getCarriedMaterial() != null ? dozer.getCarriedMaterial().name() : "none",
                                                dozer.getCarriedUnits(),
                                                dozer.getCarriedUnits() / 512.0D,
                                                dozer.getTrackLeftSpeed(),
                                                dozer.getTrackRightSpeed(),
                                                dozer.getLastExcavatedUnits(),
                                                dozer.getLastDepositedUnits(),
                                                dozer.getVehiclePitch(),
                                                dozer.getVehicleRoll()
                                        )), false);
                                        return 1;
                                    }
                                    source.sendFailure(Component.literal("§cMust be driving a bulldozer."));
                                    return 0;
                                })
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    if (player.getVehicle() instanceof GroundworksBulldozerEntity dozer) {
                                        dozer.setCarriedUnits(0);
                                        source.sendSuccess(() -> Component.literal("§a[Bulldozer] Carried material cleared."), false);
                                        return 1;
                                    }
                                    source.sendFailure(Component.literal("§cMust be driving a bulldozer."));
                                    return 0;
                                })
                        )
        );
    }
}
