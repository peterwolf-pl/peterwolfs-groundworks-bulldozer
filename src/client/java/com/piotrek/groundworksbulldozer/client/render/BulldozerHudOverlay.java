package com.piotrek.groundworksbulldozer.client.render;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.blade.BladeTransform;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

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
        int height = 96;

        // Opaque high-contrast background. The old 0x88 alpha let the world texture
        // bleed through the glyphs and made the small Minecraft font hard to read.
        extractor.fill(x - 6, y - 6, x + width, y + height, PANEL_BG);
        extractor.outline(x - 6, y - 6, width + 6, height + 6, PANEL_BORDER);

        // Header
        extractor.text(font, "PETERWOLF'S GROUNDWORKS BULLDOZER", x, y, TEXT_AMBER, true);

        float bladeHeight = dozer.getBladeHeight();
        String heightStatus;
        int heightColor;
        if (bladeHeight > 0.05F) {
            heightStatus = "PODNIESIONY / TRANSPORT";
            heightColor = TEXT_GREEN;
        } else if (bladeHeight >= -0.15F) {
            heightStatus = "RÓWNANIE";
            heightColor = TEXT_AMBER;
        } else {
            heightStatus = "GŁĘBOKIE SKRAWANIE";
            heightColor = TEXT_RED;
        }

        extractor.text(
                font,
                String.format("Lemiesz [↑/↓]: %.2f m", bladeHeight),
                x,
                y + 13,
                TEXT_PRIMARY,
                true
        );
        extractor.text(font, heightStatus, x + 155, y + 13, heightColor, true);

        // Compute the actual lowest point of the cutting edge from the synchronized
        // vehicle pitch/roll and blade pose. BladeTransform treats entity Y as the
        // local ground plane under the machine, so this value remains useful on slopes.
        BladeTransform blade = BladeTransform.compute(
                dozer.position(),
                dozer.getYRot(),
                dozer.getVehiclePitch(),
                dozer.getVehicleRoll(),
                dozer.getBladeHeight(),
                dozer.getBladeAngle()
        );

        double lowestEdgeY = blade.cuttingEdgePoints().stream()
                .mapToDouble(point -> point.y)
                .min()
                .orElse(blade.cuttingEdgeCenter().y);
        double clearance = lowestEdgeY - dozer.getY();

        String clearanceText;
        int clearanceColor;
        if (clearance >= 0.0D) {
            clearanceText = String.format("Dolna krawędź nad gruntem: +%.2f m", clearance);
            clearanceColor = clearance > 0.05D ? TEXT_GREEN : TEXT_AMBER;
        } else {
            clearanceText = String.format("Dolna krawędź poniżej gruntu: %.2f m", clearance);
            clearanceColor = TEXT_RED;
        }
        extractor.text(font, clearanceText, x, y + 25, clearanceColor, true);

        String status = dozer.isPushing() ? "SPYCHANIE TERENU" : "GOTOWA";
        extractor.text(
                font,
                "Status: " + status,
                x + 220,
                y + 25,
                dozer.isPushing() ? TEXT_AMBER : TEXT_GREEN,
                true
        );

        // Carried material & volume
        String matName = dozer.getCarriedMaterialId() > 0 && dozer.getCarriedMaterial() != null
                ? dozer.getCarriedMaterial().name().toUpperCase()
                : "BRAK";
        int units = dozer.getCarriedUnits();
        int cap = com.piotrek.groundworksbulldozer.blade.BulldozerBladeController.MAX_BLADE_CAPACITY;
        double m3 = (double) units / 512.0D;

        extractor.text(
                font,
                String.format("Urobek: %s   %d/%d u   %.3f m³", matName, units, cap, m3),
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
                String.format("%.0f%%", ratio * 100.0F),
                x + barWidth + 8,
                barY - 2,
                TEXT_PRIMARY,
                true
        );

        // Machine attitude
        extractor.text(
                font,
                String.format(
                        "Pochylenie: %.1f°   Boczne: %.1f°   Kąt lemiesza: %.1f°",
                        dozer.getVehiclePitch(),
                        dozer.getVehicleRoll(),
                        dozer.getBladeAngle()
                ),
                x,
                y + 63,
                TEXT_SECONDARY,
                true
        );

        // Controls
        extractor.text(
                font,
                "[W/S] Jazda   [A/D] Skręt   [↑/↓] Lemiesz góra/dół",
                x,
                y + 76,
                TEXT_PRIMARY,
                true
        );
        extractor.text(
                font,
                "[←/→] Przechył lemiesza",
                x,
                y + 87,
                TEXT_MUTED,
                true
        );
    }
}
