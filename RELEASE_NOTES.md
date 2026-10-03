# Twilight 1.0.0-pre.5 - prerelease

This prerelease lays out Java font-image menu titles on Bedrock with Java font
metrics, so real menu art lands on Java's GUI pixels.

- Laid out chest titles for Bedrock players with Java font metrics. Space
  providers, negative-height bitmap shifts and ItemsAdder offsets become exact
  invisible spacers; bitmap glyph bearings are corrected (removing the former
  one-unit offset); characters Java remaps in the default or named fonts receive
  private-use aliases instead of replacing Bedrock's glyphs. Controlled by
  `ui.java-text-layout`. See the [text layout review](docs/TEXT_LAYOUT_2026-10-03.md).
- Combined font definitions from every resource pack like Java and ignored
  TrueType fonts that Java cannot load.
- Kept the touch layout's native centred title with glyph substitution only.

Six real Survival menus (negative-height shifts, an ItemsAdder offset and a
512-pixel image) matched Java in the title area and on all measured slot rows;
hopper, furnace and dispenser screens stayed identical to vanilla Bedrock.
Remaining differences: a glyph directly after text without a space is one unit
right, overlapping title layers are not reproduced, bitmap tint differs, and chat,
lore, scoreboards and boss bars are not laid out yet.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
109 tests across 22 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The display adapter and title layout target Geyser 2.11.3 build
1247; other Geyser core versions require validation.

The notes below describe previous prereleases.

## Twilight 1.0.0-pre.4 - prerelease

This prerelease makes Java font-image menus usable on Bedrock desktop chest
screens and preserves wide bitmap glyphs at their authored size.

- Generated a Java container layout for Bedrock desktop chest screens. Wide
  title images are no longer wrapped, hyphenated or clipped; title and
  inventory labels use Java's positions, colour and drawing order; chest,
  player inventory and hotbar rows keep Java's spacing for 1 to 6 chest rows.
  The partial UI merges into Bedrock's vanilla UI and can be disabled with
  `ui.java-container-layout`. See the [container layout review](docs/CONTAINER_LAYOUT_2026-10-03.md).
- Enlarged bitmap-font atlas cells automatically while keeping each glyph's
  Java display size and ascent, so wide rank labels and large UI/HUD images are
  no longer shrunk or omitted. See the [wide glyph review](docs/WIDE_GLYPHS_2026-10-02.md).
- Added regressions for title placement and layering, slot spacing, vanilla
  defaults for other screens, glyph namespaces, multi-row glyph sheets, wide
  glyph pixels and neighboring cells.
- Expanded the acceptance contract to 40 areas and 289 required scenarios.

Paired Java/Bedrock captures measured every chest size from 9 to 54 slots and
confirmed that hopper, dispenser and furnace screens remain identical to the
vanilla Bedrock UI. Known differences remain: Bedrock does not tint bitmap
glyphs with the text colour, draws each bitmap glyph one GUI unit to the right,
trims transparent left padding and samples fractional sizes differently. Java
space advances, other container types, touch layouts, tooltips, live HUDs,
complete animation and first-person parity are not yet supported.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
95 tests across 20 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The development display adapter specifically targets Geyser
2.11.3 build 1247; other Geyser core versions require validation.


## Twilight 1.0.0-pre.3 - prerelease

This prerelease corrects custom item rotation and texture conversion and makes
unsupported UI conversion and Geyser restart requirements explicit.

- Corrected bitmap glyph baselines using Java height/ascent and measured Bedrock cell coordinates; added negative-ascent, overflow and page-independence regressions. See the [font metrics review](docs/FONT_METRICS_2026-10-01.md).
- Rendered inventory icons from model faces and the inherited Java GUI pose instead of exporting the first raw texture.
- Preserved individual static composite child poses, fixing separated BetterModel
  head pieces and unintended tilt in the [new-sample regression](docs/COMPOSITE_MODELS_2026-09-30.md).
- Corrected third-person model-frame conversion, left-hand mirroring, and an Euler singularity.
- Corrected rotated cuboid axes, default face UVs, and face UV rotation.
- Exported the first authored animation frame instead of stretching sprite sheets; full animation playback remains unsupported.
- Rejected oversized GUI glyphs, visible baseline overflow and custom spacing in strict mode. Diagnostic exports report omissions.
- Preserved mapping restart state across configuration reload, repeated deploys, and rollback. Geyser reload cannot activate changed item mappings.
- Added a warning for JVM locales that break Geyser mapping enum parsing.
- Added a live item-display bridge and verified sampled BetterModel and ModelEngine poses, including stationary MythicMobs models.
- Corrected display invisibility and zero-view-range handling; inactive ModelEngine fire layers no longer appear as stray planes.
- Corrected mounted display facing by aligning head and body yaw and removing duplicate mesh yaw.
- Automatically adapted invisible zero-radius cloud anchors, removing unwanted ModelEngine particles while preserving ordinary clouds and mounted passengers.
- Corrected cloud-mounted display height to match Java, fixing the tested basket's dark appearance on solid ground while retaining day/night lighting.
- Selected resource-pack overlays for the target Minecraft version and resolved explicit vanilla texture dependencies.
- Accepted dotted and uppercase overlay directory names, fixing discovery of versioned Survival content.
- Skipped event hooks for disabled providers.

The [real-content review](docs/REAL_CONTENT_REVIEW.md) includes Java references,
Bedrock observations, and unaccepted chat/UI checks. Java source assets were
unchanged. This checkpoint does not claim full visual parity or production GUI support.
The [additional seven-model matrix](docs/MODEL_MATRIX_2026-09-28.md) records
orientation, lighting and particle defects, with follow-up evidence for corrections.
The [cloud-anchor regression](docs/CLOUD_ANCHORS_2026-09-29.md) covers four ModelEngine
models, live radius/visibility changes, passenger retention and client reconnection.
The [mount-height regression](docs/DISPLAY_SEATS_2026-09-29.md) measures Java's
attachment point and verifies the basket on unchanged stone in daylight and at night.
The [extended content checks](docs/BROAD_CONTENT_2026-09-29.md) cover whole-source
compilation, 36 inventory examples, six additional held items, 28 emoji and Survival menu images; they
confirm that full first-person, animated-texture and menu parity is still absent.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
84 tests across 19 suites passed. No hosted CI was run. Automatic GitHub/GitLab pipeline
triggers are disabled.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The development display adapter specifically targets Geyser
2.11.3 build 1247; other Geyser core versions require validation. Full animated
textures, tint, billboard behavior, and UI adaptation remain open acceptance work.

Final font captures verify six exact normalized height/baseline combinations,
35 supported real glyphs and the tested inventory title. Java title color
modulation, wide rank tags and full custom UI backgrounds remain unsupported
or visually different. Oversized content is reported rather than downscaled.
See the [measured comparison](docs/FONT_METRICS_2026-10-01.md) for the precise scope.
