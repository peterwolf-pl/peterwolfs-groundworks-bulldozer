package com.piotrek.groundworksbulldozer.client.render;

import com.piotrek.groundworksbulldozer.GroundworksBulldozerMod;
import com.piotrek.groundworksbulldozer.entity.GroundworksBulldozerEntity;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * In-cab instrument HUD displayed while operating the bulldozer.
 *
 * <p>Shows real-time blade elevation, carried volume, and control status.
 */
public class BulldozerHudOverlay implements HudElement {

    public static final Identifier ID = GroundworksBulldozerMod.id("hud_overlay");

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
        int x = 10;
        int y = 10;
        int width = 275;
        int height = 74;

        // Semi-transparent panel
        extractor.fill(x - 4, y - 4, x + width, y + height, 0x88000000);
        extractor.outline(x - 4, y - 4, width + 4, height + 4, 0xFFFFAA00);

        // Header
        extractor.text(font, "§6§lPeterwolf's Groundworks Bulldozer", x, y, 0xFFFFFF, true);

        // Blade Height & Mode Indicator
        float h = dozer.getBladeHeight();
        String heightStatus;
        if (h > 0.05F) {
            heightStatus = "§a§lPODNIESIONY (Transport)";
        } else if (h >= -0.15F) {
            heightStatus = "§e§lRÓWNANIE (Grading)";
        } else {
            heightStatus = "§c§lGŁĘBOKIE SKRAWANIE (Cut)";
        }
        extractor.text(font, String.format("Lemiesz [↑/↓]: §f%.2f m §7(%s§7)", h, heightStatus), x, y + 11, 0xFFFFFF, true);

        // Status & Pitch/Roll
        String status = dozer.isPushing() ? "§e§lSPYCHANIE TERENU" : "§7GOTOWA";
        extractor.text(font, "Status: " + status, x + 160, y + 11, 0xCCCCCC, true);

        // Carried Material & Volume
        String matName = dozer.getCarriedMaterialId() > 0 && dozer.getCarriedMaterial() != null
                ? dozer.getCarriedMaterial().name().toUpperCase()
                : "BRAK";
        int units = dozer.getCarriedUnits();
        int cap = 768;
        double m3 = (double) units / 512.0D;
        extractor.text(font, String.format("Urobek w lemieszu: §f%s §7(%d/%d u = %.3f m³)", matName, units, cap, m3), x, y + 22, 0xCCCCCC, true);

        // Volume Progress Bar
        int barWidth = 160;
        int barHeight = 4;
        int barY = y + 33;
        float ratio = (float) units / (float) cap;
        extractor.fill(x, barY, x + barWidth, barY + barHeight, 0xFF333333);
        int fillWidth = Math.round(barWidth * Math.min(1.0F, ratio));
        if (fillWidth > 0) {
            extractor.fill(x, barY, x + fillWidth, barY + barHeight, ratio > 0.8F ? 0xFFFF4400 : 0xFF00AAFF);
        }
        extractor.text(font, String.format("%.0f%%", ratio * 100.0F), x + barWidth + 6, barY - 2, 0xAAAAAA, true);

        // Machine Attitude & Orientation
        extractor.text(font, String.format("Pochylenie wzdłużne: §f%.1f°§7 | Boczne: §f%.1f°§7 | Kąt lemiesza: §f%.1f°",
                dozer.getVehiclePitch(), dozer.getVehicleRoll(), dozer.getBladeAngle()),
                x, y + 43, 0xAAAAAA, true);

        // Controls Hint
        extractor.text(font, "§f[W/S]: §aJazda Przód/Tył §7| §f[A/D]: §eSkręt gąsienicami w miejscu", x, y + 54, 0xDDDDDD, true);
        extractor.text(font, "§8[↑/↓] Podnoszenie/Opuszczanie lemiesza | [←/→] Przechył", x, y + 64, 0x888888, true);
    }
}
