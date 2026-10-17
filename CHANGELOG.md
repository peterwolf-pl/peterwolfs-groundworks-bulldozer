# Changelog

All notable changes to **Peterwolf's Groundworks Bulldozer** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.2.1+26.3] - 2026-10-09

### Fixed
- **Slope & Embankment Reversing Anti-Levitation Fix (*Cofanie po skarpie bez lewitacji*)**:
  - Fixed an issue where the heavy bulldozer (and standard crawler bulldozer) would levitate horizontally in mid-air at a fixed height instead of descending when reversing down a slope, scarp, or embankment.
  - Automatically aborts active forward grade cut pass (`gradePassActive`) and resets grade latch datum upon shifting into reverse, allowing the crawler tracks to follow real terrain support rather than a frozen forward cut elevation.
  - Removed artificial `2.0m` ground support cut-off (`saneSupport`) that locked vertical chassis movement in mid-air whenever terrain dropped by more than 2 blocks below current position.
  - Scaled crawler ground contact drop floor (`supportFloor`) to vehicle track length (`6.2m` for heavy bulldozer) so contact points can conform to $\pm 45^\circ$ slopes and steep embankment ledges without premature clamping.
  - Enhanced vertical terrain conforming drop rate (up to `0.50m/tick`) to maintain tight, grounded track contact while reversing down steep grades.

---

## [0.2.0+26.3] - 2026-10-08

### Added
- **Heavy Mining Crawler Bulldozer (Cat D10T / D8T Class)** (`pw_groundworks_bulldozer:heavy_bulldozer`):
  - Twice as large as the standard crawler bulldozer: `5.4F` width $\times$ `4.4F` height hitbox.
  - **Triangular Elevated Sprocket Undercarriage (*High-Drive system*)**:
    - High-mounted drive sprocket at the top rear (protects final drive from shock loads and mud).
    - Front and rear idlers mounted low at ground level.
    - 8 pairs of heavy bottom bogie road wheels along the ground contact run.
    - Upper carrier roller supporting the long descending forward track run.
    - 44 animated track links with extreme-service single grousers moving along the continuous $190.75$-unit triangular loop.
  - **Massive 6.0m Semi-U Moldboard Blade**:
    - Deep curved moldboard with $25^\circ$ forward-angled side wings to contain massive earth volumes.
    - Protective top rock guard (*spill plate / kratownica*) across the entire upper edge.
    - Segmented hardened cutting edge wear plates with bolt detailing.
    - 17 physical cutting-edge sample points spanning the full 6.0m blade width.
    - Heavy box-section push arms with diagonal tag-link stabilizer braces.
    - Dual high-mounted hydraulic lift cylinders on the radiator shell with animated telescoping chrome piston rods.
  - **High Tractive Effort & Massive Earthmoving Capacity**:
    - **10,240 units ($20.0\text{ m}^3$ = 20 full blocks)** moldboard capacity — easily pushes material from **at least 16 blocks** at once without stalling.
    - High-throughput cutting intake of **512 units/tick per contact point** (slices through entire full blocks in 1 tick).
    - Multi-block vertical reach ($above.above()$) allowing the 1.8m tall blade to shave down 2-block high terrain walls.
    - Full capacity utilization (`liveSurchargeTarget = maxCapacity`) without premature dumping.
    - Increased tractive torque (acceleration `0.055`, max speed `0.14D`, `maxUpStep` $1.25\text{ m}$ grading / $2.0\text{ m}$ transport).
    - Reversing heap discharge: deposits up to 10,240 units across 7 terrain columns spanning the 6m width with 100% volume conservation.
  - **Interactive Rear Ripper (*Zrywak*) with Rock & Permafrost Shattering**:
    - Animated parallel-linkage ripper frame: pivots smoothly from raised transport position ($-18^\circ$) to deep penetration ($+22^\circ$) driven by **`Arrow Left` (Raise)** and **`Arrow Right` (Lower)**.
    - Active ripping physics: tearing through stone, deepslate, granite, and tuff shatters the rock into loose **Groundworks Gravel**; ripping through packed ice, blue ice, and permafrost fractures the frozen ground into loose **Groundworks Dirt**.
    - Heavy stone/ice cracking audio and block fracture spark particles.
  - **Dynamic Layer Spreading Under Lifted Blade (No Pyramids/Pillars)**:
    - Pushed material forms an authentic wide rolling berm across the entire blade.
    - Lifting the blade up while driving smoothly discharges the carried heap underneath the cutting edge, laying down an even, uniform layer equal to the space between the ground and the blade.
    - Forward berm height is capped at the top of the moldboard to eliminate 1x1 spires, pillars, and pyramids.
  - **Realistic Terrain Pitching & Anti-Levitation Physics**:
    - Continuous 6-point ground elevation sampling under front, middle, and rear track rollers.
    - Full $\pm 45^\circ$ forward/backward pitch conforming to hills, ledges, and crests without floating or hovering in mid-air.
    - Downward gravity tracking keeps the tracks firmly resting on real terrain support points.
    - Refined collision hitbox (`4.0F` x `3.4F`) matching the physical track gauge.
  - **Exhaust Smoke Plumes (Excavator System)**:
    - Client-side multi-tier diesel smoke: soft white puffs at idle, grey working smoke while driving, and dense black overload plumes when pushing heavy dirt banks.
    - Dual symmetrical plumes billowing from both exhaust stacks on the heavy bulldozer.
  - **Cat V12 Heavy Diesel Audio**:
    - Dedicated heavy diesel sound files (`heavy_engine_loop.ogg`, `heavy_engine_load.ogg`) with 30% lower piston stroke rate, +12 dB sub-bass rumble, and zero-click seamless crossfading.
  - **Elevated ROPS/FOPS Operator Cab**:
    - Elevated platform with panoramic tinted safety glass.
    - Side catwalks with yellow safety handrails and access boarding steps.
    - Rotary amber warning beacon and 4 roof LED work light clusters.
    - Deep Cat V12 diesel engine acoustics (`0.82x` pitch multiplier).
- **Matching Inventory Item Icon**:
  - 32x32 RGBA pixel art item icon (`textures/item/heavy_bulldozer.png`) matching the exact perspective, palette, and shading of the standard bulldozer icon while featuring the triangular high-drive tracks, 6.0m Semi-U blade with rock guard, twin exhausts, and rear ripper.
- **Full Multi-Language Localization (i18n)**:
  - All in-cab HUD overlay elements (blade height, ground clearance, attitude, carried volume, status, and control hints) now use `Component.translatable` for automatic English (`en_us`) and Polish (`pl_pl`) support.
- **Commands**:
  - Added `/bulldozer spawn heavy` command to spawn and board the heavy bulldozer directly.

---

## [0.1.0+26.3] - 2026-10-04

### Added
- Initial release for Minecraft Java 26.3 Fabric.
- Medium crawler bulldozer entity with differential track steering and in-place pivot turns.
- Physical 3.0m grading moldboard blade with 3072-unit ($6.0\text{ m}^3$) capacity.
- Integration with Peterwolf's Groundworks volumetric microvoxel granular terrain engine.
- Reversing heap discharge, continuous depression filling, and lateral windrow spillage.
- Positional diesel engine audio with load crossfading.
- In-cab instrument HUD overlay displaying real-time cutting edge ground clearance and carried volume.
