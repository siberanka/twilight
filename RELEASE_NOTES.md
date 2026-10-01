# Twilight 1.0.0-pre.3 ? release candidate (unpublished)

This development build corrects custom item rotation and texture conversion and makes
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

Publication is pending final Bedrock glyph and menu-title visual checks.
Automated tests and native-cell calibration pass; the current desktop focus
blocker is documented in the font metrics review.
