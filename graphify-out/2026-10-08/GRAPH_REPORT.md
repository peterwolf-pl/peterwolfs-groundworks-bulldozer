# Graph Report - peterwolfs-groundworks-bulldozer  (2026-10-08)

## Corpus Check
- 59 files · ~28,021 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 6 file(s) not represented in the graph (top: .properties 2, (none) 1, .jar 1)

## Summary
- 528 nodes · 1451 edges · 22 communities (9 shown, 13 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 143 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f6bc14c2`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GroundworksBulldozerEntity
- GroundworksBulldozerEntity.java
- net.minecraft.world.phys.Vec3
- BulldozerRenderer.java
- HeavyBulldozerEntity
- net.minecraft.core.BlockPos
- org.junit.jupiter.api.Test
- net.minecraft.server.level.ServerLevel
- BulldozerInputPayload
- BulldozerHudOverlay.java
- GroundworksBulldozerMod.java
- BulldozerEngineSoundController.java
- BulldozerCommand.java
- Q: Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?
- os
- Peterwolf's Groundworks Bulldozer Overview
- GroundworksBulldozerClient.java
- Changelog

## God Nodes (most connected - your core abstractions)
1. `GroundworksBulldozerEntity` - 87 edges
2. `TestGranularTerrain` - 39 edges
3. `BladeTransform` - 32 edges
4. `GroundworksBulldozerMod` - 26 edges
5. `HeavyBulldozerEntity` - 25 edges
6. `BulldozerTrackController` - 20 edges
7. `BulldozerModel` - 17 edges
8. `HeavyBulldozerModel` - 17 edges
9. `BulldozerBladeController` - 17 edges
10. `BladeTickResult` - 15 edges

## Surprising Connections (you probably didn't know these)
- `In-Cab HUD Telemetry` --implements--> `BulldozerHudOverlay`  [INFERRED]
  README.md → src/client/java/com/piotrek/groundworksbulldozer/client/render/BulldozerHudOverlay.java
- `Physical Grading Blade` --references--> `BladeTransform`  [INFERRED]
  README.md → src/main/java/com/piotrek/groundworksbulldozer/blade/BladeTransform.java
- `Physical Grading Blade` --implements--> `BulldozerBladeController`  [INFERRED]
  README.md → src/main/java/com/piotrek/groundworksbulldozer/blade/BulldozerBladeController.java
- `Differential Track Steering Kinematics` --implements--> `GroundworksBulldozerEntity`  [INFERRED]
  README.md → src/main/java/com/piotrek/groundworksbulldozer/entity/GroundworksBulldozerEntity.java
- `Server Authority Architecture` --rationale_for--> `GroundworksBulldozerEntity`  [INFERRED]
  README.md → src/main/java/com/piotrek/groundworksbulldozer/entity/GroundworksBulldozerEntity.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Volumetric Blade Grading and Volume Conservation Cycle** — readme_physical_grading_blade, readme_volume_conservation, src_main_java_com_piotrek_groundworksbulldozer_blade_bulldozerbladecontroller_bulldozerbladecontroller [INFERRED 0.85]

## Communities (22 total, 13 thin omitted)

### Community 1 - "GroundworksBulldozerEntity.java"
Cohesion: 0.07
Nodes (9): Physical Grading Blade, BladeTransform, BladeTickResult, BulldozerBladeController, HeavyBulldozerItem, AcceptanceGradingTest, BladeUpNoTerrainModificationTest, ChunkBoundaryPushingTest (+1 more)

### Community 2 - "net.minecraft.world.phys.Vec3"
Cohesion: 0.09
Nodes (4): BulldozerState, BulldozerTrackController, TrackState, BladeSaveLoadTest

### Community 3 - "BulldozerRenderer.java"
Cohesion: 0.07
Nodes (9): BulldozerModel, HeavyBulldozerModel, BulldozerRenderer, TrackAnimationState, BulldozerRenderState, HeavyBulldozerRenderer, TrackAnimationState, HeavyBulldozerRenderState (+1 more)

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.16
Nodes (8): BladeDownRemovesAndPushesMaterialTest, BladeTransformKinematicsTest, DifferentialSteeringTest, GradingDepressionFillTest, HeavyBulldozerGradingAndKinematicsTest, TestGranularTerrain, TwoRowsLooseMaterialPushingTest, VolumeConservationTest

### Community 8 - "BulldozerInputPayload"
Cohesion: 0.16
Nodes (3): BulldozerInputHandler, BulldozerInputPayload, MultiplayerSynchronizationTest

### Community 10 - "GroundworksBulldozerMod.java"
Cohesion: 0.14
Nodes (3): GroundworksBulldozerMod, BulldozerItem, Bulldozer Item Icon

### Community 11 - "BulldozerEngineSoundController.java"
Cohesion: 0.09
Nodes (6): BulldozerEngineSoundController, EngineLoop, EnginePair, EngineSoundProfile, Mix, EngineSoundProfileTest

### Community 14 - "Q: Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?"
Cohesion: 0.40
Nodes (4): Answer, Outcome, Q: Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?, Source Nodes

### Community 23 - "Changelog"
Cohesion: 0.33
Nodes (5): [0.1.0+26.3] - 2026-10-04, [0.2.0+26.3] - 2026-10-08, Added, Added, Changelog

## Knowledge Gaps
- **10 isolated node(s):** `Added`, `Added`, `Answer`, `Outcome`, `Source Nodes` (+5 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 98 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GroundworksBulldozerEntity` connect `GroundworksBulldozerEntity` to `GroundworksBulldozerEntity.java`, `net.minecraft.world.phys.Vec3`, `BulldozerRenderer.java`, `HeavyBulldozerEntity`, `net.minecraft.core.BlockPos`, `net.minecraft.server.level.ServerLevel`, `BulldozerInputPayload`, `BulldozerHudOverlay.java`, `GroundworksBulldozerMod.java`, `BulldozerEngineSoundController.java`, `BulldozerCommand.java`, `HeavyBulldozerEntity.java`?**
  _High betweenness centrality (0.332) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GroundworksBulldozerEntity` (e.g. with `Differential Track Steering Kinematics` and `Server Authority Architecture`) actually correct?**
  _`GroundworksBulldozerEntity` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Added`, `Added`, `Answer` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `GroundworksBulldozerEntity` be split into smaller, more focused modules?**
  _Cohesion score 0.05674044265593561 - nodes in this community are weakly interconnected._
- **Why does `BladeTransform` connect `GroundworksBulldozerEntity.java` to `GroundworksBulldozerEntity`, `net.minecraft.world.phys.Vec3`, `net.minecraft.core.BlockPos`, `org.junit.jupiter.api.Test`, `net.minecraft.server.level.ServerLevel`, `BulldozerHudOverlay.java`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Are the 20 inferred relationships involving `TestGranularTerrain` (e.g. with `.testAcceptanceDirtPileLeveling()` and `.testBladeDoesNotTransmuteForeignMaterial()`) actually correct?**
  _`TestGranularTerrain` has 20 INFERRED edges - model-reasoned connections that need verification._
- **Should `GroundworksBulldozerEntity.java` be split into smaller, more focused modules?**
  _Cohesion score 0.0691358024691358 - nodes in this community are weakly interconnected._