#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw

def create_heavy_bulldozer_textures():
    # ── 1. Entity Texture Atlas: 512x512 ──────────────────────────────
    img = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Palette
    c_cat_yellow = (245, 184, 0, 255)         # Authentic CAT equipment yellow
    c_yellow_light = (255, 215, 60, 255)      # Bevel highlight
    c_yellow_dark = (190, 138, 0, 255)        # Shaded yellow
    c_yellow_groove = (140, 98, 0, 255)       # Panel seam line

    c_dark_iron = (40, 42, 45, 255)           # Undercarriage & chassis
    c_iron_light = (68, 70, 76, 255)          # Steel edge highlight
    c_iron_recess = (22, 23, 25, 255)         # Deep shadow / gap

    c_track_belt = (30, 32, 35, 255)          # Track chain link
    c_track_cleat = (56, 58, 62, 255)         # Extreme-service grouser shoe
    c_track_roller = (72, 75, 82, 255)        # Track rollers & idlers

    c_blade_steel = (60, 64, 70, 255)         # Moldboard heavy steel plate
    c_cutting_edge = (195, 202, 212, 255)     # Hardened cutting edge / wear plate
    c_blade_rib = (45, 48, 52, 255)           # Reinforcement ribbing
    c_rock_guard = (35, 36, 40, 255)          # Top spill plate grid / mesh

    c_ripper_steel = (180, 188, 198, 255)     # Ripper shank & point
    c_chrome_ram = (235, 240, 248, 255)       # Hydraulic cylinder piston rod

    c_glass_pane = (135, 205, 245, 45)        # Tinted safety glass
    c_glass_frame = (20, 20, 22, 255)         # Window gasket
    c_glass_streak = (220, 245, 255, 80)      # Reflection streak

    c_exhaust_metal = (50, 52, 56, 255)       # Heat-treated exhaust stack
    c_beacon_amber = (255, 146, 0, 255)
    c_beacon_bright = (255, 225, 40, 255)
    c_worklight = (255, 255, 220, 255)

    c_hazard_yellow = (255, 210, 0, 255)
    c_hazard_black = (20, 20, 20, 255)

    c_dirt = (134, 90, 61, 255)
    c_seat_leather = (32, 33, 36, 255)

    # ── Section 1: Track Links & Grousers (u=0..160, v=0..72) ────────
    draw.rectangle([0, 0, 160, 72], fill=c_track_belt)
    for x in range(0, 160, 4):
        draw.line([(x, 0), (x, 72)], fill=c_track_cleat)
        draw.line([(x + 1, 0), (x + 1, 72)], fill=c_iron_recess)

    # ── Section 2: Undercarriage Frame & Belly Plate (u=164..340, v=0..72) ──
    draw.rectangle([164, 0, 340, 72], fill=c_dark_iron)
    for x in range(164, 340, 8):
        draw.line([(x, 0), (x, 72)], fill=c_iron_light)
    # Final drive hub ring
    draw.ellipse([180, 10, 220, 50], outline=c_cat_yellow, width=3)
    draw.ellipse([190, 20, 210, 40], fill=c_iron_light)

    # ── Section 3: Engine Hood & Grille (u=0..180, v=76..170) ────────
    draw.rectangle([0, 76, 180, 170], fill=c_cat_yellow)
    draw.rectangle([2, 78, 178, 168], outline=c_yellow_dark)
    # Side engine louver vents
    for y in range(90, 155, 5):
        draw.rectangle([15, y, 95, y + 2], fill=c_yellow_dark)
        draw.line([(15, y + 2), (95, y + 2)], fill=c_iron_recess)
    # Front heavy radiator grille (u=100..176, v=85..165)
    draw.rectangle([105, 85, 175, 165], fill=c_dark_iron)
    for y in range(88, 162, 3):
        draw.line([(107, y), (173, y)], fill=c_iron_light)

    # ── Section 4: ROPS Cab Structure & Roof (u=184..340, v=76..170) ──
    draw.rectangle([184, 76, 340, 170], fill=c_cat_yellow)
    draw.rectangle([188, 80, 336, 166], outline=c_yellow_dark)
    # Black ROPS posts & trim
    draw.rectangle([184, 76, 192, 170], fill=c_dark_iron)
    draw.rectangle([332, 76, 340, 170], fill=c_dark_iron)

    # ── Section 5: Cab Tinted Glass Panes (u=344..440, v=0..80) ──────
    draw.rectangle([344, 0, 440, 80], fill=c_glass_pane)
    draw.rectangle([344, 0, 440, 80], outline=c_glass_frame, width=2)
    # Angled reflection streaks
    draw.line([(355, 75), (425, 5)], fill=c_glass_streak, width=2)
    draw.line([(365, 78), (435, 8)], fill=c_glass_streak, width=1)

    # ── Section 6: Semi-U Blade Moldboard & Rock Guard (u=0..260, v=174..280) ──
    draw.rectangle([0, 174, 260, 280], fill=c_blade_steel)
    # Top rock guard grid (mesh)
    draw.rectangle([0, 174, 260, 204], fill=c_rock_guard)
    for x in range(0, 260, 6):
        draw.line([(x, 174), (x, 204)], fill=c_iron_light)
    for y in range(174, 204, 6):
        draw.line([(0, y), (260, y)], fill=c_iron_light)
    # Vertical reinforcement ribs
    for x in range(20, 250, 40):
        draw.rectangle([x, 205, x + 4, 280], fill=c_blade_rib)

    # ── Section 7: Hardened Cutting Edge & Teeth (u=0..260, v=284..320) ─
    draw.rectangle([0, 284, 260, 320], fill=c_cutting_edge)
    for x in range(8, 255, 12):
        draw.ellipse([x - 2, 298, x + 2, 302], fill=c_dark_iron)
        draw.line([(x, 304), (x, 320)], fill=c_iron_light)

    # ── Section 8: Push Arms, Tag-Links, C-Frame (u=264..400, v=174..250) ─
    draw.rectangle([264, 174, 400, 250], fill=c_cat_yellow)
    draw.rectangle([266, 176, 398, 248], outline=c_yellow_dark)
    # Heavy pivot trunnion bearings
    draw.ellipse([270, 180, 300, 210], fill=c_dark_iron)
    draw.ellipse([275, 185, 295, 205], fill=c_iron_light)

    # ── Section 9: Rear Ripper (Shank & Frame) (u=264..420, v=254..340) ──
    draw.rectangle([264, 254, 350, 340], fill=c_cat_yellow)
    draw.rectangle([266, 256, 348, 338], outline=c_yellow_dark)
    # Solid steel ripper shank & chisel tip
    draw.rectangle([354, 254, 420, 340], fill=c_ripper_steel)
    draw.polygon([(354, 340), (420, 340), (387, 310)], fill=c_cutting_edge)
    for y in range(260, 330, 10):
        draw.ellipse([385, y, 389, y + 4], fill=c_dark_iron) # pin holes

    # ── Section 10: Lift Cylinders & Chrome Rods (u=404..480, v=90..180) ─
    draw.rectangle([404, 90, 480, 180], fill=c_cat_yellow)
    draw.rectangle([440, 90, 465, 180], fill=c_chrome_ram) # chrome piston rod
    draw.rectangle([404, 165, 480, 180], fill=c_dark_iron)  # cylinder gland / seals

    # ── Section 11: Twin Exhaust Stacks & Air Cleaners (u=424..500, v=184..260) ──
    draw.rectangle([424, 184, 500, 260], fill=c_exhaust_metal)
    for y in range(188, 256, 8):
        draw.line([(426, y), (498, y)], fill=c_dark_iron)

    # ── Section 12: Hazard Chevron Stripes & Catwalks (u=424..512, v=264..340) ──
    draw.rectangle([424, 264, 512, 340], fill=c_hazard_yellow)
    for offset in range(-80, 120, 14):
        draw.polygon([
            (424 + offset, 264),
            (424 + offset + 7, 264),
            (424 + offset + 7 + 76, 340),
            (424 + offset + 76, 340)
        ], fill=c_hazard_black)

    # ── Section 13: Beacon & Work Lights (u=444..500, v=0..50) ─────────
    draw.rectangle([444, 0, 480, 50], fill=c_beacon_amber)
    draw.rectangle([452, 8, 472, 42], fill=c_beacon_bright)
    # Work lights
    draw.rectangle([482, 0, 506, 24], fill=c_dark_iron)
    draw.rectangle([485, 3, 503, 21], fill=c_worklight)

    # ── Section 14: Cab Interior (u=0..140, v=324..380) ────────────────
    draw.rectangle([0, 324, 140, 380], fill=c_seat_leather)
    draw.rectangle([70, 324, 140, 350], fill=c_iron_light) # dashboard console

    # ── Section 15: Carried Granular Material (u=144..260, v=324..380) ──
    draw.rectangle([144, 324, 260, 380], fill=c_dirt)
    for x in range(144, 260, 3):
        for y in range(324, 380, 3):
            if (x + y) % 5 == 0:
                draw.point((x, y), fill=(105, 68, 44, 255))
            elif (x + y) % 7 == 0:
                draw.point((x, y), fill=(160, 115, 80, 255))

    os.makedirs("src/main/resources/assets/pw_groundworks_bulldozer/textures/entity", exist_ok=True)
    entity_path = "src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png"
    img.save(entity_path, "PNG")
    print(f"Saved heavy bulldozer entity texture to {entity_path}")

    # ── 2. Item Icon Texture: 32x32 Pixel Art (matching bulldozer.png style) ────
    icon = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    idraw = ImageDraw.Draw(icon)

    c_yellow = (245, 184, 0, 255)
    c_yellow_dark = (205, 150, 0, 255)
    c_yellow_light = (255, 215, 60, 255)
    c_iron_dark = (35, 36, 40, 255)
    c_iron_mid = (55, 58, 65, 255)
    c_iron_light = (75, 78, 85, 255)
    c_glass = (160, 220, 250, 210)
    c_glass_frame = (28, 30, 34, 255)
    c_exhaust = (45, 46, 50, 255)
    c_blade_steel = (85, 90, 100, 255)
    c_cutting_edge = (215, 222, 232, 255)
    c_rock_guard = (40, 42, 46, 255)
    c_ripper_shank = (185, 192, 202, 255)
    c_beacon = (255, 160, 0, 255)
    c_beacon_bright = (255, 225, 40, 255)

    # 1. Rear Ripper (X=26..30, Y=16..26)
    idraw.rectangle([26, 17, 28, 22], fill=c_yellow_dark)
    idraw.line([(28, 18), (30, 23)], fill=c_ripper_shank, width=2)
    idraw.point((30, 24), fill=c_cutting_edge)
    idraw.point((30, 25), fill=c_cutting_edge)

    # 2. Triangular Track (High-Drive undercarriage)
    idraw.rectangle([5, 23, 26, 28], fill=c_iron_dark)
    idraw.rectangle([6, 25, 25, 27], fill=c_iron_mid)
    for x in range(6, 26, 3):
        idraw.point((x, 28), fill=c_iron_light)

    # Elevated Sprocket (triangle apex at upper rear: X=20..25, Y=15..20)
    idraw.ellipse([20, 15, 25, 20], fill=c_iron_dark)
    idraw.ellipse([21, 16, 24, 19], fill=c_iron_mid)
    idraw.point((22, 17), fill=c_yellow)
    idraw.point((23, 17), fill=c_yellow)

    # Front and Rear Idlers
    idraw.ellipse([5, 22, 9, 26], fill=c_iron_mid)
    idraw.ellipse([22, 22, 26, 26], fill=c_iron_mid)

    # Incline track runs
    idraw.line([(25, 23), (24, 18)], fill=c_iron_dark, width=2)
    idraw.point((25, 20), fill=c_iron_light)
    idraw.line([(20, 16), (9, 23)], fill=c_iron_dark, width=2)
    idraw.line([(19, 17), (10, 23)], fill=c_iron_mid, width=1)
    idraw.point((15, 20), fill=c_iron_light)

    # 3. Main Body & Engine Hood (X=8..22, Y=13..21)
    idraw.rectangle([8, 13, 22, 21], fill=c_yellow)
    idraw.rectangle([8, 13, 12, 21], fill=c_yellow_dark)
    idraw.line([(9, 15), (9, 19)], fill=c_iron_dark)
    idraw.line([(11, 15), (11, 19)], fill=c_iron_dark)

    # Twin Exhaust Stacks
    idraw.line([(12, 5), (12, 12)], fill=c_exhaust, width=1)
    idraw.line([(14, 5), (14, 12)], fill=c_exhaust, width=1)
    idraw.point((12, 4), fill=c_iron_dark)
    idraw.point((14, 4), fill=c_iron_dark)

    # Cyclone Air Cleaner
    idraw.rectangle([10, 8, 11, 12], fill=c_exhaust)
    idraw.point((10, 7), fill=c_iron_light)

    # 4. Elevated ROPS Cab (X=15..25, Y=6..14)
    idraw.rectangle([16, 7, 24, 14], fill=c_yellow)
    idraw.rectangle([15, 6, 25, 7], fill=c_yellow_light)
    idraw.rectangle([17, 8, 21, 12], fill=c_glass)
    idraw.line([(16, 8), (16, 12)], fill=c_glass_frame)
    idraw.line([(22, 8), (22, 12)], fill=c_glass_frame)

    # Amber Beacon on Cab Roof
    idraw.point((20, 5), fill=c_beacon)
    idraw.point((20, 4), fill=c_beacon_bright)

    # 5. Push Arm & Front Semi-U Blade
    idraw.line([(6, 19), (14, 21)], fill=c_yellow_dark, width=2)
    idraw.line([(9, 20), (13, 18)], fill=c_yellow, width=1)
    idraw.line([(10, 15), (7, 18)], fill=c_cutting_edge, width=1)

    # Top Rock Guard
    idraw.rectangle([2, 13, 6, 15], fill=c_rock_guard)
    idraw.point((3, 14), fill=c_iron_light)
    idraw.point((5, 14), fill=c_iron_light)

    # Main curved moldboard
    idraw.rectangle([2, 16, 6, 25], fill=c_blade_steel)
    idraw.line([(2, 17), (2, 24)], fill=c_iron_dark, width=1)

    # Hardened Cutting Edge & wear teeth
    idraw.rectangle([1, 26, 6, 27], fill=c_cutting_edge)
    idraw.point((2, 27), fill=c_iron_dark)
    idraw.point((4, 27), fill=c_iron_dark)

    item_path = "src/main/resources/assets/pw_groundworks_bulldozer/textures/item/heavy_bulldozer.png"
    icon.save(item_path, "PNG")
    print(f"Saved matching 32x32 pixel art icon to {item_path}")

if __name__ == "__main__":
    create_heavy_bulldozer_textures()
