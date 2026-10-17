# Graph Report - peterwolfs-groundworks-bulldozer  (2026-10-09)

## Corpus Check
- Corpus is ~32,483 words - fits in a single context window. You may not need a graph.

## Summary
- 616 nodes · 1724 edges · 26 communities (9 shown, 17 thin omitted)
- Extraction: 89% EXTRACTED · 10% INFERRED · 1% AMBIGUOUS · INFERRED: 179 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 10
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 21
- Community 23

## God Nodes (most connected - your core abstractions)
1. `GroundworksBulldozerEntity` - 94 edges
2. `TestGranularTerrain` - 43 edges
3. `BladeTransform` - 32 edges
4. `GroundworksBulldozerMod` - 26 edges
5. `HeavyBulldozerEntity` - 26 edges
6. `BulldozerTrackController` - 21 edges
7. `HeavyBulldozerModel` - 18 edges
8. `BulldozerModel` - 17 edges
9. `BulldozerBladeController` - 17 edges
10. `Heavy Bulldozer Entity Texture Atlas` - 17 edges

## Surprising Connections (you probably didn't know these)
- `Bulldozer Entity Texture` --conceptually_related_to--> `BulldozerRenderer`  [INFERRED]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/bulldozer.png → src/client/java/com/piotrek/groundworksbulldozer/client/render/BulldozerRenderer.java
- `Bulldozer Item Icon` --conceptually_related_to--> `BulldozerItem`  [INFERRED]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/item/bulldozer.png → src/main/java/com/piotrek/groundworksbulldozer/item/BulldozerItem.java
- `Heavy Mining Crawler Bulldozer` --semantically_similar_to--> `Heavy Bulldozer 0.2.0`  [INFERRED] [semantically similar]
  README.md → CHANGELOG.md
- `BulldozerRenderer` --references--> `GroundworksBulldozerEntity`  [EXTRACTED]
  src/client/java/com/piotrek/groundworksbulldozer/client/render/BulldozerRenderer.java → src/main/java/com/piotrek/groundworksbulldozer/entity/GroundworksBulldozerEntity.java
- `HeavyBulldozerRenderer` --references--> `HeavyBulldozerEntity`  [EXTRACTED]
  src/client/java/com/piotrek/groundworksbulldozer/client/render/HeavyBulldozerRenderer.java → src/main/java/com/piotrek/groundworksbulldozer/entity/HeavyBulldozerEntity.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Blade, cab, and tracks co-depicted on the icon** — bulldozer_blade, operator_cab, continuous_tracks [EXTRACTED 1.00]
- **Cab with two side windows and a roof pipe** — operator_cab, cab_side_windows, cab_roof_pipe [EXTRACTED 1.00]
- **Heavy Dozer 0.2.0 Release** — changelog_heavy_bulldozer_020, changelog_high_drive, changelog_semi_u_blade, changelog_ripper [EXTRACTED 1.00]
- **Yellow panels, hazard stripes, and orange marker share one high-visibility paint scheme** — src_main_resources_assets_pw_groundworks_bulldozer_textures_entity_heavy_bulldozer_safety_yellow_panels, src_main_resources_assets_pw_groundworks_bulldozer_textures_entity_heavy_bulldozer_hazard_chevrons, src_main_resources_assets_pw_groundworks_bulldozer_textures_entity_heavy_bulldozer_orange_marker_square [INFERRED 0.85]

## Communities (26 total, 17 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (11): BladeTransform, BladeTickResult, BulldozerBladeController, BulldozerItem, HeavyBulldozerItem, Bulldozer Item Icon, AcceptanceGradingTest, BladeUpNoTerrainModificationTest (+3 more)

### Community 1 - "Community 1"
Cohesion: 0.06
Nodes (11): GroundworksBulldozerClient, BulldozerModel, HeavyBulldozerModel, SpoilHeapModel, BulldozerRenderer, TrackAnimationState, BulldozerRenderState, HeavyBulldozerRenderer (+3 more)

### Community 2 - "Community 2"
Cohesion: 0.16
Nodes (7): BladeDownRemovesAndPushesMaterialTest, BladeTransformKinematicsTest, GradeDatumTest, GradingDepressionFillTest, HeavyBulldozerGradingAndKinematicsTest, TestGranularTerrain, VolumeConservationTest

### Community 3 - "Community 3"
Cohesion: 0.12
Nodes (3): BulldozerGroundworksGameTest, GroundworksBulldozerAdapter, IGranularTerrainAccess

### Community 4 - "Community 4"
Cohesion: 0.08
Nodes (5): BulldozerInputHandler, BulldozerState, BulldozerInputPayload, BladeSaveLoadTest, MultiplayerSynchronizationTest

### Community 5 - "Community 5"
Cohesion: 0.09
Nodes (6): BulldozerEngineSoundController, EngineLoop, EnginePair, EngineSoundProfile, Mix, EngineSoundProfileTest

### Community 8 - "Community 8"
Cohesion: 0.12
Nodes (25): Heavy Bulldozer Entity Texture Atlas, Black Disc on Yellow Panel, Light-Blue Glass Panel with White Diagonal Highlight, Brown Diagonally Hatched Rectangle, Cab Glazing, Cooling Intake, Tiny Cream Square, Diagonally Split Gray Plate with Black Hatch (+17 more)

### Community 15 - "Community 15"
Cohesion: 0.18
Nodes (12): Front Blade, Short Yellow Cab-Roof Pipe, Two Dark Cab Side Windows, Dark Continuous Tracks, Exhaust Stack, Left-Facing Three-Quarter View, Operator Cab, Pixel-Art Item Icon (+4 more)

### Community 18 - "Community 18"
Cohesion: 0.20
Nodes (7): Heavy Bulldozer 0.2.0, Triangular High-Drive Tracks, Rear Ripper, 6.0m Semi-U Blade, Heavy Mining Crawler Bulldozer, Medium Tracked Bulldozer, Peterwolf's Groundworks Bulldozer

## Ambiguous Edges - Review These
- `Charcoal Vertically Ribbed Panel` → `Cooling Intake`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Charcoal Vertically Ribbed Panel` → `Track Running Gear`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Yellow Ring on Black Ribbed Field` → `Lamp or Gauge`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Orange Square with Yellow Border` → `Lamp or Gauge`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Tiny Cream Square` → `Lamp or Gauge`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Black Disc on Yellow Panel` → `Lamp or Gauge`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Small Horizontally Lined Gray Square` → `Cooling Intake`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Silver Band with Even Black Rivet Dots` → `Moldboard`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Silver Band with Even Black Rivet Dots` → `Track Running Gear`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Diagonally Split Gray Plate with Black Hatch` → `Moldboard`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Yellow and Black Diagonal Hazard Stripes` → `Moldboard`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Brown Diagonally Hatched Rectangle` → `Weathered or Soiled Surface`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Lower Dark Gray Blocks` → `Track Running Gear`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/entity/heavy_bulldozer.png · relation: may_depict
- `Short Yellow Cab-Roof Pipe` → `Exhaust Stack`  [AMBIGUOUS]
  src/main/resources/assets/pw_groundworks_bulldozer/textures/item/heavy_bulldozer.png · relation: conceptually_related_to

## Knowledge Gaps
- **15 isolated node(s):** `Bulldozer Entity Texture`, `Bulldozer Item Icon`, `Medium Tracked Bulldozer`, `Triangular High-Drive Tracks`, `Rear Ripper` (+10 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 104 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Charcoal Vertically Ribbed Panel` and `Cooling Intake`?**
  _Edge tagged AMBIGUOUS (relation: may_depict) - confidence is low._
- **Why does `GroundworksBulldozerEntity` connect `Community 6` to `Community 0`, `Community 1`, `Community 3`, `Community 4`, `Community 5`, `Community 7`, `Community 9`, `Community 10`, `Community 11`, `Community 12`, `Community 13`, `Community 14`, `Community 16`, `Community 20`?**
  _High betweenness centrality (0.298) - this node is a cross-community bridge._
- **Are the 24 inferred relationships involving `TestGranularTerrain` (e.g. with `.testAcceptanceDirtPileLeveling()` and `.testBladeDoesNotTransmuteForeignMaterial()`) actually correct?**
  _`TestGranularTerrain` has 24 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Bulldozer Entity Texture`, `Bulldozer Item Icon`, `Medium Tracked Bulldozer` to the rest of the system?**
  _15 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05711086226203808 - nodes in this community are weakly interconnected._
- **What is the exact relationship between `Charcoal Vertically Ribbed Panel` and `Track Running Gear`?**
  _Edge tagged AMBIGUOUS (relation: may_depict) - confidence is low._
- **Why does `BladeTransform` connect `Community 0` to `Community 2`, `Community 3`, `Community 6`, `Community 9`, `Community 20`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._