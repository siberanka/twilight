# Changelog

All notable changes in Twilight are documented here.

## 1.0.0-pre.3 - unreleased

- Render custom inventory icons from Java model geometry and inherited GUI
  transforms instead of the first texture. Preserve face UVs, UV quarter turns,
  element rotation/rescale, depth and transparency in static previews.

- Accept Java-compatible dotted and uppercase pack overlay directory names;
  retain traversal protection. Extended real-source testing exposed versioned
  directories that previously stopped Survival content discovery.
- Match Java's full-height passenger attachment for cloud-mounted item displays.
  This corrects their vertical position and the potion basket's dark lighting
  above solid blocks, without changing textures, shaders or brightness overrides.
- Automatically represent invisible, zero-radius Java cloud anchors with inert
  Bedrock actors. Preserve cloud metadata and mounted passengers across changes;
  ordinary clouds retain Geyser translation. This removes the unwanted particles
  observed around ModelEngine models without editing provider assets.
- Keep item-display head yaw aligned with body yaw and apply entity yaw once,
  correcting mounted BetterModel models that faced away from their Java reference.
- Add a seven-model Java/Bedrock comparison with captured failures and follow-up
  evidence; lighting and full pose parity remain acceptance gaps.
- Match Java display visibility: ignore base entity invisibility and hide
  zero-view-range displays while retaining their current item and transform.
- Added a development Geyser item-display bridge with separate quaternion
  interpolation, Java item-frame rotation, and correct start/duration semantics.
- Added live BetterModel and ModelEngine bone-item registry discovery. Strict
  conversion rejects incomplete provider discovery.
- Select pack overlays by the target Minecraft format and declaration order;
  inactive overlays are no longer imported as independent packs.
- Resolve explicit vanilla model textures and their frame metadata from the
  verified client cache while preserving authored texture overrides.
- Require a restart for display-index changes, and validate pack ZIP data before
  replacing deployed files.
- Corrected custom third-person item rotation axes, signs, and model-frame
  conversion to preserve Java display orientation, including compound angles.
- Applied Java's left-hand mirroring to explicitly authored left-hand poses.
- Fixed quaternion-to-Euler conversion at the positive 90-degree singularity,
  which could reverse an item's orientation.
- Added regressions for yaw, all three orientation axes, explicit left-hand
  transforms, and both Euler singularities. Full in-game visual parity remains
  unverified.
- Corrected rotated cuboid X/Y signs, preserved face UV rotations, and derived
  omitted face UV rectangles from Java element bounds.
- Sampled the first authored animation frame for static texture export instead
  of stretching a whole animation sheet; tall static PNGs are no longer cropped.
- Rejected oversized GUI bitmap glyphs and custom spacing in strict mode instead
  of silently shrinking menus into emoji-sized cells. Diagnostic builds report
  these omissions; full Bedrock GUI adaptation remains unsupported.
- Reported a required server restart when custom item mappings differ from the
  running Geyser registry. Texture-only reloads remain available; configuration
  reloads no longer clear the pending restart requirement.
- Added a JVM-locale diagnostic for Geyser's Turkish/Azeri enum-parsing issue.
- Skipped optional event registration for disabled providers, avoiding closed
  classloader errors when an incompatible ModelEngine version fails to enable.
- Disabled automatic GitHub and GitLab pipeline triggers. Builds are run and
  tested locally under siberanka; pushes do not run hosted builds.

## 1.0.0-pre.2 - 2026-09-22

### Java-to-Bedrock presentation

- Fixed custom tool and weapon pose selection by deriving Geyser's handheld
  presentation from the resolved Java model parent chain instead of the base
  Minecraft item identifier.
- Preserved authored Java first- and third-person translation, rotation, and
  scale without automatic geometry fitting that could shrink or reposition a
  model into an incorrect held pose.
- Fixed chat emoji height by composing every Bedrock Unicode page on a stable
  16-pixel grid and bottom-aligning each glyph independently, including pages
  that also contain oversized GUI glyphs.

### Validation

- Added regressions for model-parent pose selection, exact hand transforms,
  and mixed-size bitmap glyphs sharing one Unicode page.
- Removed pre-fix pose and emoji captures from the public acceptance gallery.

## 1.0.0-pre.1 - 2026-09-21

### Server platform

- Added one server plugin for Paper, Folia, and Spigot with a Bukkit service
  API, operation events, commands, structured logs, and controlled Geyser
  reloads.
- Added deterministic provider discovery, lifecycle hooks, command
  synchronization, source fingerprints, and delayed settle checks for
  ItemsAdder, CraftEngine, Nexo, Oraxen, ModelEngine, and BetterModel.
- Added authored `contents` and `resources`, provider `data` and `cache`,
  datapack, configured source, and generated-pack fallback layers.

### Java-to-Bedrock conversion

- Added current and legacy item-definition handling, Geyser custom mappings,
  layered textures, Java cuboids, display transforms, equipment attachables,
  block states, creative metadata, and animated textures.
- Preserved Bedrock's native pose, pull, charge, cast, and line behavior for
  exact texture-only bow, crossbow, and fishing-rod recolours.
- Added runtime-selected Java state geometry for layered, transformed,
  animated, and volumetric weapons, including distinct crossbow arrow and
  rocket states.
- Added bitmap-font conversion with Unicode collision protection and layered
  custom-sound conversion with recursive references and OGG validation.
- Added hash-verified, version-matched Mojang model, texture, font, and sound
  fallback for explicitly referenced vanilla assets.

### Reliability

- Added strict publication validation, bounded resource names, safe path and
  archive handling, deterministic output, transactional deployment,
  last-known-good restoration, and bounded backups.
- Added focused source, font, and sound audits plus 28 automated tests for the
  current server compiler and deployment path.
- Added GitHub Actions and GitLab CI pipelines that build and test the same
  tagged source before publishing `Twilight.jar` and its SHA-256 checksum.
