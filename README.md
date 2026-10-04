# Peterwolf's Groundworks Bulldozer

**Peterwolf's Groundworks Bulldozer** is a Minecraft Java 26.3 Fabric mod that adds an industrial tracked crawler bulldozer designed specifically to push, grade, level, and shape granular terrain powered by **Peterwolf's Groundworks**.

---

## Features

### 🚜 Vehicle
- **Medium Tracked Bulldozer**: Heavy industrial crawler chassis with realistic caterpillar track frames, sprockets, idlers, and bottom track rollers.
- **Roll-Over Protective Cab (ROPS)**: Hollow safety cabin with panoramic tinted safety glass windows, driver seat, and instrument dashboard giving full forward visibility of the work area.
- **Flashing Amber Beacon**: Working rotary warning strobe on the cab roof that spins and pulses while operating.
- **Differential Track Steering**: Allows true in-place pivot turns when throttle is neutral, as well as smooth curvature turns during driving.

### 🛡️ Physical Grading Blade
- **Real World-Space Geometry**: The blade is not cosmetic. It maintains exact 3D world-space collision and cutting edge sampling points across its full 3.0 m width.
- **Continuous Blade Elevation**: Smooth hydraulic raising and lowering from `-0.60 m` (deep ditching and cutting) to `+0.80 m` (raised transport position).
- **Physical States Tracked**:
  - `bladeHeight`: Continuous vertical cutting offset relative to track ground level.
  - `bladeAngle`: Kinematic pitch/tilt angle of the moldboard.
  - `previousBladeTransform` & `currentBladeTransform`: Full 3D pose records representing the swept cutting area.
- **Dynamic Carried Surcharge**: Live granular material layer visible directly inside the blade's moldboard during active pushing.

### ⛏️ Groundworks Volumetric Integration
- **Strict Volume Conservation**: Every microvoxel excavated is stored in the blade's live carry buffer, deposited forward into a berm, used to fill depressions, or spilled sideways past the blade wings. **Pushed volume strictly equals removed volume — zero duplication, zero loss.**
- **Automatic Terrain Leveling**:
  - Shaves material peaks higher than the blade's cutting line.
  - Automatically deposits carried material to fill ruts, holes, and small depressions flush with the grade.
  - Leaves a flat, graded surface behind the machine.
- **Lateral Spill (Windrows)**: When the blade is overloaded or carrying significant material, excess material escapes around the left and right blade wings.
- **Granular Materials Supported**: Dirt, coarse dirt, sand, and gravel.
- **Granular Relaxation**: Modified cells trigger Groundworks' relaxation engine to naturally settle loose material according to its natural angle of repose.

### 🌐 Server Authority
- **Server**: Controls vehicle position, differential track kinematics, blade elevation, terrain modifications, and material conservation.
- **Client**: Handles user input gathering, smooth visual animation, model rendering, and the in-cab telemetry HUD.

---

## Controls

All controls are configurable in the Minecraft Key Binds settings menu:

| Action | Default Key | Description |
|---|---|---|
| **Forward** | `W` | Drives both tracks forward |
| **Reverse** | `S` | Drives both tracks backward |
| **Turn Left** | `A` | Differential left steer (pivots in place if stopped) |
| **Turn Right** | `D` | Differential right steer (pivots in place if stopped) |
| **Raise Blade** | `Arrow Up` | Smoothly lifts the blade upward |
| **Lower Blade** | `Arrow Down` | Smoothly lowers the blade into the ground |
| **Tilt Blade Left** | `Arrow Left` | Fine tilt adjustment to the left |
| **Tilt Blade Right** | `Arrow Right` | Fine tilt adjustment to the right |

---

## In-Cab HUD Telemetry

When seated in the bulldozer cab, an instrument HUD displays:
- Blade height in meters and active grading mode (`PODNIESIONY / Transport`, `RÓWNANIE / Grading`, `GŁĘBOKIE SKRAWANIE / Deep Cut`).
- Operation status (`SPYCHANIE TERENU / Pushing` or `GOTOWA / Ready`).
- Carried material type and volume progress bar (`units / 768 u = m³`).
- Pitch and roll angles conforming to the ground terrain.
- Context-sensitive control hints.

---

## Commands

- `/bulldozer spawn` — Spawns and boards a bulldozer at player position.
- `/bulldozer blade <height>` — Sets the blade height directly (`-0.60` to `0.80`).
- `/bulldozer info` — Displays real-time telemetry (blade height, blade angle, carried material, track speeds, pitch, roll).
- `/bulldozer clear` — Clears the blade's carried material buffer.

---

## Testing & Verification

The mod includes comprehensive unit and integration tests:

1. **`BladeUpNoTerrainModificationTest`**: Proves that a raised blade driving over flat terrain never modifies the ground.
2. **`BladeDownRemovesAndPushesMaterialTest`**: Validates that lowering the blade into granular terrain shaves material, accumulates it in front of the blade, and continuous height variation affects cutting volume.
3. **`VolumeConservationTest`**: Strictly asserts across multi-tick grading cycles that `worldUnits + carriedUnits == initialUnits`.
4. **`ChunkBoundaryPushingTest`**: Verifies that cutting, pushing, and depositing seamlessly span across chunk borders.
5. **`BladeSaveLoadTest`**: Validates roundtrip serialization and deserialization of blade elevation, angle, and carried material.
6. **`MultiplayerSynchronizationTest`**: Tests client input encoding/decoding over `RegistryFriendlyByteBuf`.
7. **`DifferentialSteeringTest`**: Proves differential track steering physics, including in-place pivot turns and straight driving.
8. **`BladeTransformKinematicsTest`**: Tests blade geometry, sample point spacing, and elevation kinematics.
9. **`AcceptanceGradingTest`**: Full acceptance test creating a dirt pile, lowering the blade with `Arrow Down`, driving forward with `W`, and verifying visible pushing, accumulation, lateral spillage, flattening behind the blade, and exact volume conservation.

Run the test suite with:
```bash
./gradlew test
```
