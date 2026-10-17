package com.piotrek.groundworksbulldozer.client.render;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import com.piotrek.groundworksbulldozer.vehicle.GradeDatum;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * High-contrast in-cab instrument HUD displayed while operating the bulldozer.
 *
 * <p>Shows blade position and true cutting-edge clearance, carried volume,
 * machine attitude and control hints.
 */
public class BulldozerHudOverlay implements HudElement {

    public static final Identifier ID = GroundworksBulldozerMod.id("hud_overlay");

    private static final int PANEL_BG = 0xE6000000;
    private static final int PANEL_BORDER = 0xFFFFB000;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFE6E6E6;
    private static final int TEXT_MUTED = 0xFFB8B8B8;
    private static final int TEXT_AMBER = 0xFFFFC247;
    private static final int TEXT_GREEN = 0xFF72FF72;
    private static final int TEXT_RED = 0xFFFF6868;
    private static final int BAR_BG = 0xFF242424;
    private static final int BAR_FILL = 0xFF25B7FF;
    private static final int BAR_FULL = 0xFFFF6A3D;

    public static void register() {
        HudElementRegistry.addLast(ID, new BulldozerHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (!(client.player.getVehicle() instanceof GroundworksBulldozerEntity dozer)) {
            return;
        }

        Font font = client.font;
        int x = 12;
        int y = 12;
        int width = 340;
        int height = 100;

        // Opaque high-contrast background. The old 0x88 alpha let the world texture
        // bleed through the glyphs and made the small Minecraft font hard to read.
        extractor.fill(x - 6, y - 6, x + width, y + height, PANEL_BG);
        extractor.outline(x - 6, y - 6, width + 6, height + 6, PANEL_BORDER);

        // Header
        extractor.text(font, dozer.getVehicleTitle(), x, y, TEXT_AMBER, true);

        float bladeHeight = dozer.getBladeHeight();
        Component heightStatus;
        int heightColor;
        if (bladeHeight > 0.05F) {
            heightStatus = Component.translatable("hud.pw_groundworks_bulldozer.height_raised");
            heightColor = TEXT_GREEN;
        } else if (bladeHeight >= -0.15F) {
            heightStatus = Component.translatable("hud.pw_groundworks_bulldozer.height_grading");
            heightColor = TEXT_AMBER;
        } else {
            heightStatus = Component.translatable("hud.pw_groundworks_bulldozer.height_deep_cut");
            heightColor = TEXT_RED;
        }

        extractor.text(
                font,
                Component.translatable(
                        "hud.pw_groundworks_bulldozer.blade_height",
                        String.format(Locale.ROOT, "%.2f", bladeHeight)
                ),
                x,
                y + 13,
                TEXT_PRIMARY,
                true
        );
        extractor.text(font, heightStatus, x + 155, y + 13, heightColor, true);

        // After the tracks settle into the slot this reads about 0, not the lever.
        // A latch more than a metre off the chassis is stale: show the lever, not air.
        double edgeY = GradeDatum.edgeY(
                dozer.getGradeDatumY(),
                dozer.getY(),
                bladeHeight
        );
        double clearance = edgeY - dozer.getY();

        Component clearanceText;
        int clearanceColor;
        if (clearance >= 0.0D) {
            clearanceText = Component.translatable(
                    "hud.pw_groundworks_bulldozer.clearance_above",
                    String.format(Locale.ROOT, "%.2f", clearance)
            );
            clearanceColor = clearance > 0.05D ? TEXT_GREEN : TEXT_AMBER;
        } else {
            clearanceText = Component.translatable(
                    "hud.pw_groundworks_bulldozer.clearance_below",
                    String.format(Locale.ROOT, "%.2f", clearance)
            );
            clearanceColor = TEXT_RED;
        }
        extractor.text(font, clearanceText, x, y + 25, clearanceColor, true);

        Component status = dozer.isPushing()
                ? Component.translatable("hud.pw_groundworks_bulldozer.status_pushing")
                : Component.translatable("hud.pw_groundworks_bulldozer.status_ready");
        extractor.text(
                font,
                Component.translatable("hud.pw_groundworks_bulldozer.status", status),
                x + 220,
                y + 25,
                dozer.isPushing() ? TEXT_AMBER : TEXT_GREEN,
                true
        );

        // Carried material & volume
        Object matName = dozer.getCarriedMaterialId() > 0 && dozer.getCarriedMaterial() != null
                ? dozer.getCarriedMaterial().name().toUpperCase()
                : Component.translatable("hud.pw_groundworks_bulldozer.carried_empty");
        int units = dozer.getCarriedUnits();
        int cap = dozer.getMaxBladeCapacity();
        double m3 = (double) units / 512.0D;

        extractor.text(
                font,
                Component.translatable(
                        "hud.pw_groundworks_bulldozer.carried",
                        matName,
                        units,
                        cap,
                        String.format(Locale.ROOT, "%.3f", m3)
                ),
                x,
                y + 38,
                TEXT_SECONDARY,
                true
        );

        // Volume progress bar
        int barWidth = 200;
        int barHeight = 6;
        int barY = y + 51;
        float ratio = (float) units / (float) cap;

        extractor.fill(x, barY, x + barWidth, barY + barHeight, BAR_BG);
        int fillWidth = Math.round(barWidth * Math.min(1.0F, ratio));
        if (fillWidth > 0) {
            extractor.fill(
                    x,
                    barY,
                    x + fillWidth,
                    barY + barHeight,
                    ratio > 0.8F ? BAR_FULL : BAR_FILL
            );
        }
        extractor.text(
                font,
                String.format(Locale.ROOT, "%.0f%%", ratio * 100.0F),
                x + barWidth + 8,
                barY - 2,
                TEXT_PRIMARY,
                true
        );

        // Machine attitude
        extractor.text(
                font,
                Component.translatable(
                        "hud.pw_groundworks_bulldozer.attitude",
                        String.format(Locale.ROOT, "%.1f", dozer.getVehiclePitch()),
                        String.format(Locale.ROOT, "%.1f", dozer.getVehicleRoll()),
                        String.format(Locale.ROOT, "%.1f", dozer.getBladeAngle())
                ),
                x,
                y + 63,
                TEXT_SECONDARY,
                true
        );

        // Controls
        extractor.text(
                font,
                Component.translatable("hud.pw_groundworks_bulldozer.controls_drive"),
                x,
                y + 76,
                TEXT_PRIMARY,
                true
        );
        extractor.text(
                font,
                Component.translatable("hud.pw_groundworks_bulldozer.controls_tilt"),
                x,
                y + 87,
                TEXT_MUTED,
                true
        );

        // Ripper status
        Component ripperState = dozer.getRipperLift() > 0.35F
                ? Component.translatable("hud.pw_groundworks_bulldozer.ripper_active")
                : Component.translatable("hud.pw_groundworks_bulldozer.ripper_raised");
        extractor.text(
                font,
                Component.translatable("hud.pw_groundworks_bulldozer.ripper_status", ripperState),
                x + 160,
                y + 87,
                dozer.getRipperLift() > 0.35F ? TEXT_AMBER : TEXT_GREEN,
                true
        );
    }
}
