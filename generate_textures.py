#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw

def create_bulldozer_textures():
    # 1. Entity Texture: 512x512 texture atlas
    img = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Palette ───────────────────────────────────────────────────────
    c_yellow = (245, 184, 0, 255)            # CAT/Komatsu industrial yellow
    c_yellow_light = (255, 210, 50, 255)      # Highlight bevel
    c_yellow_dark = (195, 142, 0, 255)        # Shaded yellow
    c_yellow_groove = (145, 102, 0, 255)      # Panel line

    c_dark_iron = (44, 45, 48, 255)           # Chassis & frame
    c_iron_light = (70, 72, 78, 255)          # Edge highlight
    c_iron_recess = (24, 25, 27, 255)

    c_track_belt = (32, 33, 36, 255)          # Crawler track belt
    c_track_cleat = (58, 60, 64, 255)         # Grouser bars
    c_track_roller = (75, 78, 85, 255)        # Rollers

    c_blade_steel = (65, 68, 74, 255)         # Moldboard steel plate
    c_cutting_edge = (195, 202, 212, 255)     # Hardened cutting edge / wear lip
    c_blade_rib = (48, 50, 54, 255)           # Rear reinforcing ribs

    c_glass_pane = (140, 210, 245, 38)        # Tinted safety glass (alpha ~15%)
    c_glass_frame = (24, 24, 26, 255)         # Frame gasket
    c_glass_streak = (220, 245, 255, 65)

    c_seat_leather = (35, 36, 40, 255)
    c_exhaust_metal = (55, 56, 60, 255)

    c_beacon_amber = (255, 146, 0, 255)
    c_beacon_bright = (255, 225, 40, 255)

    c_dirt = (134, 90, 61, 255)               # Granular soil in blade
    c_sand = (219, 207, 153, 255)
    c_gravel = (128, 126, 124, 255)

    # ── Section 1: Track Belts (u=0..140, v=0..72) ───────────────────
    draw.rectangle([0, 0, 140, 72], fill=c_track_belt)
    for x in range(0, 140, 4):
        draw.line([(x, 0), (x, 72)], fill=c_track_cleat)
        draw.line([(x + 1, 0), (x + 1, 72)], fill=c_iron_recess)

    # ── Section 2: Chassis Frame (u=144..280, v=0..50) ───────────────
    draw.rectangle([144, 0, 280, 50], fill=c_dark_iron)
    for x in range(144, 280, 8):
        draw.line([(x, 0), (x, 50)], fill=c_iron_light)

    # ── Section 3: Engine Hood & Hull (u=0..120, v=76..140) ──────────
    draw.rectangle([0, 76, 120, 140], fill=c_yellow)
    # Louver air vents on side of engine hood
    for y in range(86, 125, 4):
        draw.rectangle([15, y, 75, y + 1], fill=c_yellow_dark)
        draw.line([(15, y + 2), (75, y + 2)], fill=c_iron_recess)

    # ── Section 4: Front Radiator Grille (u=124..180, v=76..130) ─────
    draw.rectangle([124, 76, 180, 130], fill=c_dark_iron)
    for y in range(80, 126, 3):
        draw.line([(126, y), (178, y)], fill=c_iron_light)

    # ── Section 5: ROPS Cab Structure (u=184..310, v=76..140) ────────
    draw.rectangle([184, 76, 310, 140], fill=c_yellow)
    draw.rectangle([188, 80, 306, 136], outline=c_yellow_dark)

    # ── Section 6: Glass Panes (u=314..400, v=0..80) ─────────────────
    draw.rectangle([314, 0, 400, 80], fill=c_glass_pane)
    draw.rectangle([314, 0, 400, 80], outline=c_glass_frame)
    # Reflection highlight streaks
    draw.line([(325, 75), (385, 5)], fill=c_glass_streak, width=2)
    draw.line([(335, 78), (395, 8)], fill=c_glass_streak, width=1)

    # ── Section 7: Bulldozer Blade Moldboard (u=0..180, v=144..230) ──
    draw.rectangle([0, 144, 180, 230], fill=c_blade_steel)
    # Cutting edge lip plate at bottom of blade
    draw.rectangle([0, 215, 180, 230], fill=c_cutting_edge)
    for x in range(10, 175, 15):
        # Bolt rivets along cutting edge
        draw.ellipse([x - 2, 220, x + 2, 224], fill=c_dark_iron)

    # ── Section 8: Blade Push Arms & C-Frame (u=184..290, v=144..190)
    draw.rectangle([184, 144, 290, 190], fill=c_yellow)
    draw.rectangle([184, 144, 290, 190], outline=c_yellow_dark)

    # ── Section 9: Hydraulic Cylinders (u=294..370, v=90..150) ───────
    draw.rectangle([294, 90, 370, 150], fill=c_yellow)
    draw.rectangle([330, 90, 350, 150], fill=c_cutting_edge) # chrome ram

    # ── Section 10: Exhaust Stack & Air Cleaner (u=374..440, v=90..140)
    draw.rectangle([374, 90, 440, 140], fill=c_exhaust_metal)

    # ── Section 11: Safety Beacon (u=444..480, v=0..40) ──────────────
    draw.rectangle([444, 0, 480, 40], fill=c_beacon_amber)
    draw.rectangle([452, 8, 472, 28], fill=c_beacon_bright)

    # ── Section 12: Carried Granular Material (u=0..120, v=234..290) ─
    draw.rectangle([0, 234, 120, 290], fill=c_dirt)
    for x in range(0, 120, 3):
        for y in range(234, 290, 3):
            if (x + y) % 5 == 0:
                draw.point((x, y), fill=(105, 68, 44, 255))
            elif (x + y) % 7 == 0:
                draw.point((x, y), fill=(160, 115, 80, 255))

    # ── Section 13: Cab Interior (Seat & Controls) (u=124..220, v=234..290)
    draw.rectangle([124, 234, 220, 290], fill=c_seat_leather)

    os.makedirs("src/main/resources/assets/pw_groundworks_bulldozer/textures/entity", exist_ok=True)
    entity_path = "src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/bulldozer.png"
    img.save(entity_path, "PNG")
    print(f"Saved entity texture to {entity_path}")

    # 2. Item Icon Texture: 32x32 pixel art bulldozer
    icon = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    idraw = ImageDraw.Draw(icon)

    # Draw miniature isometric bulldozer
    # Tracks (bottom)
    idraw.rectangle([4, 21, 28, 28], fill=(35, 36, 40, 255))
    idraw.rectangle([5, 23, 27, 26], fill=(55, 58, 65, 255))

    # Hull / Engine Hood
    idraw.rectangle([10, 14, 26, 21], fill=(245, 184, 0, 255))
    idraw.rectangle([10, 14, 15, 21], fill=(210, 155, 0, 255)) # front grille

    # ROPS Cab
    idraw.rectangle([16, 8, 25, 14], fill=(245, 184, 0, 255))
    idraw.rectangle([17, 9, 21, 13], fill=(160, 220, 250, 200)) # glass

    # Exhaust Pipe
    idraw.line([(15, 8), (15, 13)], fill=(45, 46, 50, 255), width=1)

    # Push Arm & Front Blade
    idraw.line([(6, 19), (13, 22)], fill=(195, 142, 0, 255), width=2)
    idraw.rectangle([2, 16, 6, 26], fill=(85, 90, 100, 255))
    idraw.line([(2, 26), (6, 26)], fill=(210, 215, 225, 255), width=1) # cutting edge

    # Amber Beacon
    idraw.point((20, 7), fill=(255, 160, 0, 255))

    os.makedirs("src/main/resources/assets/pw_groundworks_bulldozer/textures/item", exist_ok=True)
    item_path = "src/main/resources/assets/pw_groundworks_bulldozer/textures/item/bulldozer.png"
    icon.save(item_path, "PNG")
    print(f"Saved item texture to {item_path}")

if __name__ == "__main__":
    create_bulldozer_textures()
