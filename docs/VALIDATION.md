# Validation and acceptance

Twilight uses repeatable machine checks before any visual acceptance pass.

The [additional model matrix](MODEL_MATRIX_2026-09-28.md) records seven real
models, paired client captures, and the defects that prevent visual acceptance.

## Local build

```powershell
$env:JAVA_HOME='<path-to-jdk-25>'
$env:GRADLE_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'
.\gradlew.bat :twilight:build --no-daemon --no-configuration-cache
```

Core tests verify world resolution, archive traversal rejection, content classification, layered legacy items, modern predicate variants, volumetric geometry, model-parent handheld selection, exact authored hand transforms, stable mixed-size glyph pages, named-font collision safety, layered and referenced sounds, deterministic ZIP output, Geyser ownership boundaries, three-snapshot retention, and rollback.

## Source audit

Server roots can be audited read-only with the Gradle verification task:

```powershell
.\gradlew.bat :twilight:auditServerSources `
  -Ptwilight.audit.roots='<server-root>,<second-server-root>' `
  -Ptwilight.audit.output='build/reports/twilight-source-audit.json'
```

The task discovers sources and worlds, parses indexed JSON, expands current item-definition trees, resolves legacy model references, and writes a bounded JSON report. It does not modify the audited servers.

## Server smoke test

Use a disposable local Paper/Folia server with current Geyser:

1. Install `Twilight.jar`.
2. Confirm plugin enable and `vanilla-override=false`.
3. Run `/twilight status`, `/twilight scan`, and `/twilight convert`.
4. Confirm an operation log exists for every command.
5. On strict failure, confirm `build/current` and deployed Geyser files retain their previous hashes.
6. On success, verify `pack.zip`, Geyser mapping v2 JSON, manifest UUIDs, and build report.
7. Deploy four successive known builds and confirm only the newest three snapshots remain; roll back each retained index.

## Visual acceptance

Visual acceptance begins only after structural validation passes. Use a disposable Paper/Geyser server and an automated client run that records commands, perspectives, content logs, and machine-readable results. Required matrices include:

- 2D single/multi-layer icons and animation frames;
- 3D simple and composite models in GUI, first-person hands, third-person hands, ground/frame, and head contexts;
- bow/crossbow pull and charge states, fishing cast/line, shield use, trident/spear, armor, and elytra;
- emoji, glyph, menu, and HUD baselines;
- custom mobs/furniture with idle/move/attack/death animations;
- biome and seasonal transitions in affected worlds.

No private pack, third-party plugin binary, world, credential, or raw server log may enter the public repository or release artifact. Publish screenshots only with explicit operator authorization and after reviewing every image for private data. The [2026-09-27 review](REAL_CONTENT_REVIEW.md) contains authorized test captures, not redistributable source packs.

The unreleased 1.0.0-pre.3 local build passed 65 tests across 16 suites. Regressions cover independent pose bases, left-hand mirroring, Euler singularities, rotated cuboid corners, default/rotated UVs, first animation-frame selection, tall static textures, rejected GUI/spacing providers, pack overlays, display interpolation and visibility, and restart state across repeated deployment, configuration reload, and rollback. Passing these checks does not imply full client visual parity.
