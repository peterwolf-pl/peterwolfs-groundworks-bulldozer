package com.piotrek.groundworksbulldozer.client;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.client.input.BulldozerInputHandler;
import com.piotrek.groundworksbulldozer.client.input.BulldozerKeyBindings;
import com.piotrek.groundworksbulldozer.client.model.BulldozerModel;
import com.piotrek.groundworksbulldozer.client.render.BulldozerHudOverlay;
import com.piotrek.groundworksbulldozer.client.render.BulldozerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class GroundworksBulldozerClient implements ClientModInitializer {

    public static final ModelLayerLocation BULLDOZER_LAYER =
            new ModelLayerLocation(GroundworksBulldozerMod.id("bulldozer"), "main");

    @Override
    public void onInitializeClient() {
        // Register entity model layer
        ModelLayerRegistry.registerModelLayer(BULLDOZER_LAYER, BulldozerModel::createBodyLayer);

        // Register entity renderer
        EntityRendererRegistry.register(GroundworksBulldozerMod.BULLDOZER, BulldozerRenderer::new);

        // Register keybindings
        BulldozerKeyBindings.register();

        // Register input tick listener
        ClientTickEvents.END_CLIENT_TICK.register(BulldozerInputHandler::clientTick);

        // Register in-cab HUD
        BulldozerHudOverlay.register();
    }
}
