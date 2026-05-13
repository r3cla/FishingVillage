# FishingVillage — Project Notes for AI

## Stack
- Java 21, LibGDX 1.12.1, LWJGL3 backend
- Gradle 9.4.1, multi-module: `core/` (game logic + rendering) + `desktop/` (launcher)
- Run: `./gradlew :desktop:run`
- Compile check: `./gradlew :core:compileJava`

## Versioning
Format: `0.x.y` (no alpha/beta suffix needed for a solo hobby project)

- `0.x.0` — new feature or milestone
- `0.x.y` — bugfix or small tweak
- `1.0.0` — when the game feels feature-complete enough to show someone

Current version: `0.1.0` (pre-release, active development)

Tracked in `gradle.properties` as `version=0.x.y`.

## IDE Diagnostics
The Eclipse-based Java language server in VS Code frequently produces **stale false-positive errors** on enum constructors (e.g. "final field may already have been assigned"). Always verify real errors with `./gradlew :core:compileJava` — if Gradle says BUILD SUCCESSFUL, the code is correct.

## Save File
`desktop/assets/save.json` — persists coins, rod, bait inventory, clock time, fish records (per-species catch count / personal best / shiny), and unlocked achievements. Managed by `SaveManager.java` + `SaveData.java`.

## Package Dependency Constraint
`game` package **must not import** `fish` package. `FishRegistry` (fish package) already imports `game.JunkType`, so a reverse import creates a cycle. Consequence: achievement-checking logic lives in `FishingScreen` (which can see both packages), not in `GameState`. Methods like `recordFishCatch()` take `String fishId` rather than a `Fish` object for the same reason.

## Key Systems

### Fishing Minigame (`FishingScreen`)
- Player moves the hook with **←→ arrow keys** (`isKeyPressed` for smooth hold-down movement).
- The fish zone bounces automatically; speed and zone size scale with rarity.
- Hook turns **green** when overlapping the zone.
- **Progress bar** mechanic: keeping hook on zone fills a green catch bar; off zone drains it. Bar reaching 100% = catch. Time limit per rarity (10–20s); exceeding it = fish escapes.
- Zone never starts within 0.10 of center (forced to left or right half).
- Fill/drain rates and zone half-widths per rarity: Common 0.14, Uncommon 0.09, Rare 0.06, Epic 0.04, Legendary 0.03.
- Storm debuff (rare+): player `mgHookSpeed *= 0.80f`, zone `mgZoneSpeed *= 1.35f`, drain rate `*= 1.25f`.

### Day/Night Sky Transition (`FishingScreen.drawBackground`)
- When `TimeOfDay` changes, a 5-second cross-fade runs between the old and new background texture sets.
- Fields: `bgCurLayers`, `bgFromLayers`, `skyTransition` (0→1), `SKY_FADE_DURATION = 5f`.
- The old layers are drawn opaque underneath; new layers fade in on top via batch alpha.

### Journal Window (`FishingScreen`)
- Toggled with **[J]** key from the IDLE state; closes the bag if open.
- Two tabs: **Encyclopedia** (all 20 fish, discovery/catch/PB/shiny status) and **Achievements** (12 milestones).
- Data stored in `GameState` (`fishRecords` map, `unlockedAchievements` set) and persisted via `SaveManager`.

### Text Wrapping
- Long fish descriptions use LibGDX's built-in word-wrap: `font.draw(batch, text, x, y, wrapWidth, Align.left, true)`.

## Scene Geometry (`FishingScreen`)

**Screen**: 1280 × 720 px. Constants: `W = 1280f`, `H = 720f`, `hz = 320f` (horizon y — water meets land).

**Coordinate system**: LibGDX y=0 is at the **bottom**. The horizon at y=320 is roughly mid-screen. Above hz = sky/land; below hz = water.

**Render order** (shapes block, each frame):
1. `drawBackground` (SpriteBatch, textures)
2. `drawWaves`, `drawWaterShimmer`, `drawSplash` (water surface effects)
3. `drawDock` (dock structure)
4. `drawWharfStall` (market stall sitting on dock left end, drawn over dock)
5. `drawFishingLine`, `drawWeatherEffects` (overlays)
6. `drawSprites` (SpriteBatch: rod, fisherman, bobber, fish/junk catch)
7. `drawLightningFlash`, `drawHUD`, panels, minigame (UI layers)

**Dock geometry** (`drawDock`):
- Deck surface: x=0–710, y=(hz−22) to hz (top face highlight: top 6px)
- Posts: x={100, 270, 460, 640}, each 16px wide, from y=(hz−240) to hz
- Cross-beam: y=(hz−112), ties all posts
- Right end cap: x=706–712, y=(hz−26) to hz
- Left end (x=0–99) is free — the harbor building sits here

**Fisher geometry**:
- `FISHER_X = 630f` — fisherman's foot/center x
- `ARM_X_OFFSET = 65f`, arm at x ≈ 695f
- Rod tip: approx x=940, y varies with state
- `ROD_SCALE = 1.7f`

**Wharf stall** (`drawWharfStall`, drawn after dock so it sits on top):
- Counter: x=35–275, y=hz to hz+50 (body 40px + top highlight 10px), right side face x=275–285
- Awning posts: x=43–51 and x=259–267, y=hz+50 to hz+146
- Striped canopy: x=7–303 (28px overhang each side), y=hz+146 to hz+174, 6 alternating stripes
- Canopy valance drop: y=hz+130 to hz+146 (darker front edge)
- Barrel (left counter): x=59–103, y=hz+50 to hz+102, with 3 horizontal hoop lines
- Crate (right counter): x=203–259, y=hz+50 to hz+90, with plank lines
- Day: dock wood tones (PLANK/POST colors) + red-and-cream canopy stripes
- Night: same shapes, all colors darkened, canopy stripes muted
