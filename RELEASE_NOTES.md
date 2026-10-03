# Twilight 1.0.0-pre.4-SNAPSHOT - development checkpoint

Adaptive bitmap-font cells now preserve wide labels and larger UI/HUD images at
their Java-authored display size. Atlas growth also preserves neighboring glyph
heights and baselines. The [wide glyph review](docs/WIDE_GLYPHS_2026-10-02.md)
records six further real images, 36 existing real glyphs, source integrity,
whole-source audits and paired client captures. All 89 tests in 19 suites pass.

The [current JAR and checksum](artifacts/) are a locally built snapshot. Native
inventory-title clipping, bitmap tint, transparent left padding and fractional
sampling remain visible failures. Full menu, HUD, item and animation parity is
not established. Large native atlases have substantial memory costs.

The published `v1.0.0-pre.3` assets remain unchanged on
[GitLab](https://gitlab.com/siberanka/twilight/-/releases/v1.0.0-pre.3) and
[GitHub](https://github.com/siberanka/twilight/releases/tag/v1.0.0-pre.3).
The notes below describe that historical release, not the current snapshot.

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
