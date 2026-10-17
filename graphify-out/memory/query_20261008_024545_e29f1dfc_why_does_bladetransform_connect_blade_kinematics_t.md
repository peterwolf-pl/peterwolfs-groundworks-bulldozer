---
type: "query"
date: "2026-10-08T02:45:45.484677+00:00"
question: "Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?"
contributor: "graphify"
outcome: "useful"
source_nodes: ["BladeTransform", "GroundworksBulldozerEntity", "BulldozerHudOverlay", "BulldozerBladeController", "VolumeConservationTest"]
---

# Q: Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?

## Answer

Expanded from original query via vocab: [blade, transform, kinematics, entity, state, client, input, hud, terrain, volume, conservation, adapter]. BladeTransform is the immutable 3D world-space representation of the bulldozer's blade pose, geometry, and swept volume. It acts as the central kinematic contract across 5 distinct subsystems: 1) Bulldozer Entity State stores current and previous blade transforms for server tick updates and particle emission; 2) Client Input and HUD consumes it in BulldozerHudOverlay to render in-cab blade telemetry and cutting depths; 3) Groundworks Terrain Adapter references it in BulldozerBladeController for microvoxel cutting and reversing heap deposit; 4) Volume Conservation Tests assert cutting bounds, wing spillage, and elevation predicates like isRaised(); and 5) Entity Sync coordinates state tracking across network ticks.

## Outcome

- Signal: useful

## Source Nodes

- BladeTransform
- GroundworksBulldozerEntity
- BulldozerHudOverlay
- BulldozerBladeController
- VolumeConservationTest