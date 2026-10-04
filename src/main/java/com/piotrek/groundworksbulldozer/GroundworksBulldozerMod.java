package com.piotrek.groundworksbulldozer;

import com.piotrek.groundworksbulldozer.command.BulldozerCommand;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.item.BulldozerItem;
import com.piotrek.groundworksbulldozer.network.BulldozerInputPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroundworksBulldozerMod implements ModInitializer {

    public static final String MOD_ID = "pw_groundworks_bulldozer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // ── Entity Registration ──────────────────────────────────────────
    public static final ResourceKey<EntityType<?>> BULLDOZER_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, id("bulldozer"));

    public static final EntityType<GroundworksBulldozerEntity> BULLDOZER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            BULLDOZER_KEY,
            EntityType.Builder.of(GroundworksBulldozerEntity::new, MobCategory.MISC)
                    .sized(2.8F, 2.4F)
                    .clientTrackingRange(10)
                    .build(BULLDOZER_KEY)
    );

    // ── Sound Registration ──────────────────────────────────────────
    public static final SoundEvent ENGINE_LOOP = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            id("engine_loop"),
            SoundEvent.createFixedRangeEvent(id("engine_loop"), 48.0F)
    );

    // ── Item Registration ────────────────────────────────────────────
    public static final ResourceKey<Item> BULLDOZER_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, id("bulldozer"));

    public static final BulldozerItem BULLDOZER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            BULLDOZER_ITEM_KEY,
            new BulldozerItem(new Item.Properties().setId(BULLDOZER_ITEM_KEY).stacksTo(1))
    );

    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("tools_and_utilities")
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Peterwolf's Groundworks Bulldozer for MC 26.3...");

        // 1. Networking registration
        PayloadTypeRegistry.serverboundPlay().register(
                BulldozerInputPayload.TYPE, BulldozerInputPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                BulldozerInputPayload.TYPE, (payload, context) -> {
                    context.server().execute(() -> {
                        ServerPlayer player = context.player();
                        if (player.getVehicle() instanceof GroundworksBulldozerEntity dozer
                                && dozer.isDriver(player)) {
                            dozer.setControlInputs(
                                    payload.throttle(),
                                    payload.steer(),
                                    payload.bladeLift(),
                                    payload.bladeTilt()
                            );
                        }
                    });
                }
        );

        // 2. Command registration
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> BulldozerCommand.register(dispatcher)
        );

        // 3. Creative Tab placement
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_TAB).register(output -> {
            output.accept(BULLDOZER_ITEM);
        });

        LOGGER.info("Peterwolf's Groundworks Bulldozer initialized successfully.");
    }
}
