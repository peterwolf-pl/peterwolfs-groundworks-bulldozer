# Graph Report - peterwolfs-groundworks-bulldozer  (2026-10-09)

## Corpus Check
- 59 files · ~31,027 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 6 file(s) not represented in the graph (top: .properties 2, (none) 1, .jar 1)

## Summary
- 548 nodes · 1561 edges · 18 communities (10 shown, 8 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 173 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f6bc14c2`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GroundworksBulldozerEntity
- BladeTransform
- GroundworksBulldozerClient.java
- HeavyBulldozerEntity
- net.minecraft.core.BlockPos
- org.junit.jupiter.api.Test
- BulldozerVisualGameTest.java
- BladeSaveLoadTest.java
- BulldozerHudOverlay.java
- GroundworksBulldozerEntity.java
- BulldozerEngineSoundController.java
- Q: Why does BladeTransform connect Blade Kinematics Tests to Bulldozer Entity State, Client Input and HUD, Groundworks Terrain Adapter, Volume Conservation Tests, and Entity Sync and Particles?
- os
- Peterwolf's Groundworks Bulldozer Overview
- Changelog

## God Nodes (most connected - your core abstractions)
1. `GroundworksBulldozerEntity` - 94 edges
2. `TestGranularTerrain` - 43 edges
3. `BladeTransform` - 33 edges
4. `GroundworksBulldozerMod` - 26 edges
5. `HeavyBulldozerEntity` - 26 edges
6. `BulldozerTrackController` - 20 edges
7. `HeavyBulldozerModel` - 18 edges
8. `BulldozerBladeController` - 18 edges
9. `BulldozerModel` - 17 edges
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

## Communities (18 total, 8 thin omitted)

### Community 1 - "BladeTransform"
Cohesion: 0.11
Nodes (9): Physical Grading Blade, BladeTransform, BladeTickResult, BulldozerBladeController, AcceptanceGradingTest, BladeUpNoTerrainModificationTest, ChunkBoundaryPushingTest, ReversingHeapDepositTest (+1 more)

### Community 3 - "GroundworksBulldozerClient.java"
Cohesion: 0.06
Nodes (10): GroundworksBulldozerClient, BulldozerModel, HeavyBulldozerModel, BulldozerRenderer, TrackAnimationState, BulldozerRenderState, HeavyBulldozerRenderer, TrackAnimationState (+2 more)

### Community 4 - "HeavyBulldozerEntity"
Cohesion: 0.09
Nodes (4): BulldozerGroundworksGameTest, HeavyBulldozerEntity, BulldozerTrackController, TrackState

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.16
Nodes (7): BladeDownRemovesAndPushesMaterialTest, BladeTransformKinematicsTest, DifferentialSteeringTest, GradingDepressionFillTest, HeavyBulldozerGradingAndKinematicsTest, TestGranularTerrain, VolumeConservationTest

### Community 8 - "BladeSaveLoadTest.java"
Cohesion: 0.08
Nodes (5): BulldozerInputHandler, BulldozerState, BulldozerInputPayload, BladeSaveLoadTest, MultiplayerSynchronizationTest

### Community 9 - "BulldozerHudOverlay.java"
Cohesion: 0.11
Nodes (3): In-Cab HUD Telemetry, BulldozerKeyBindings, BulldozerHudOverlay

### Community 10 - "GroundworksBulldozerEntity.java"
Cohesion: 0.05
Nodes (5): BulldozerCommand, GroundworksBulldozerMod, BulldozerItem, HeavyBulldozerItem, Bulldozer Item Icon

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
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 95 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GroundworksBulldozerEntity` connect `GroundworksBulldozerEntity` to `BladeTransform`, `GroundworksBulldozerClient.java`, `HeavyBulldozerEntity`, `net.minecraft.core.BlockPos`, `BulldozerVisualGameTest.java`, `BladeSaveLoadTest.java`, `BulldozerHudOverlay.java`, `GroundworksBulldozerEntity.java`, `BulldozerEngineSoundController.java`?**
  _High betweenness centrality (0.350) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GroundworksBulldozerEntity` (e.g. with `Differential Track Steering Kinematics` and `Server Authority Architecture`) actually correct?**
  _`GroundworksBulldozerEntity` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Added`, `Added`, `Answer` to the rest of the system?**
  _10 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `GroundworksBulldozerEntity` be split into smaller, more focused modules?**
  _Cohesion score 0.054244306418219465 - nodes in this community are weakly interconnected._
- **Why does `BladeTransform` connect `BladeTransform` to `GroundworksBulldozerEntity`, `HeavyBulldozerEntity`, `net.minecraft.core.BlockPos`, `org.junit.jupiter.api.Test`, `GroundworksBulldozerEntity.java`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Are the 24 inferred relationships involving `TestGranularTerrain` (e.g. with `.testAcceptanceDirtPileLeveling()` and `.testBladeDoesNotTransmuteForeignMaterial()`) actually correct?**
  _`TestGranularTerrain` has 24 INFERRED edges - model-reasoned connections that need verification._
- **Should `BladeTransform` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._