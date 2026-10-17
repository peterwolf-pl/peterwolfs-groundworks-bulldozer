package com.piotrek.groundworksbulldozer.client;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.client.input.BulldozerInputHandler;
import com.piotrek.groundworksbulldozer.client.input.BulldozerKeyBindings;
import com.piotrek.groundworksbulldozer.client.model.BulldozerModel;
import com.piotrek.groundworksbulldozer.client.render.BulldozerHudOverlay;
import com.piotrek.groundworksbulldozer.client.render.BulldozerRenderer;
import com.piotrek.groundworksbulldozer.client.sound.BulldozerEngineSoundController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class GroundworksBulldozerClient implements ClientModInitializer {

    public static final ModelLayerLocation BULLDOZER_LAYER =
            new ModelLayerLocation(GroundworksBulldozerMod.id("bulldozer"), "main");

    public static final ModelLayerLocation HEAVY_BULLDOZER_LAYER =
            new ModelLayerLocation(GroundworksBulldozerMod.id("heavy_bulldozer"), "main");

    @Override
    public void onInitializeClient() {
        // Register entity model layers
        ModelLayerRegistry.registerModelLayer(BULLDOZER_LAYER, BulldozerModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(HEAVY_BULLDOZER_LAYER, com.piotrek.groundworksbulldozer.client.model.HeavyBulldozerModel::createBodyLayer);

        // Register entity renderers
        EntityRendererRegistry.register(GroundworksBulldozerMod.BULLDOZER, BulldozerRenderer::new);
        EntityRendererRegistry.register(GroundworksBulldozerMod.HEAVY_BULLDOZER, com.piotrek.groundworksbulldozer.client.render.HeavyBulldozerRenderer::new);

        // Register keybindings
        BulldozerKeyBindings.register();

        // Register input and positional engine sound tick listeners
        ClientTickEvents.END_CLIENT_TICK.register(BulldozerInputHandler::clientTick);
        ClientTickEvents.END_CLIENT_TICK.register(BulldozerEngineSoundController::clientTick);

        // Register in-cab HUD
        BulldozerHudOverlay.register();
    }
}
