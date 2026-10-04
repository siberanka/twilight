# New samples and composite transforms — 30 September–1 October 2026

> The screenshots of this report were retired on 4 October 2026 in favour of current captures; the measurements below remain. Current Java/Bedrock captures are in the [README](../README.md#visual-acceptance-tests).

New real-source samples exposed a conversion defect: static composite children
lost their individual Java display transforms. BetterModel's `demon_knight`
head contains 27 child models with different `fixed` rotations. Merging their
cubes and applying the first child's pose separated the horns and tilted other
parts. `blue_wizard` also showed an incorrect head angle.

The compiler now retains each child mesh and its authored context transforms.
Display entities and attachables get separate child bones; GUI rendering applies
each child's GUI pose and lighting with a shared depth buffer. Generated sprite
children are retained beside cuboid children. This is automatic conversion, with
no model-name exceptions or edits to the source assets.

## Scope and build checks

- 36 item models and 24 emoji not included in the two earlier item selections.
  Selection used a fixed seed and preferred different namespaces and model kinds.
- Four new provider models: BetterModel `blue_wizard` (211 elements) and
  `demon_knight` (323), ModelEngine `bl_earth_small_spider` (49) and
  `angel_gm_lancer_one` (24). The lancer was also bound through MythicMobs.
- Provider discovery used the live item collector. The BetterModel fixture
  compiled 167/168 mappings, including 36 new item definitions and six previously
  held items. The ModelEngine fixture compiled 278/278 mappings: legacy and
  modern mappings for 139 descriptors, **not 278 distinct mobs**.
- Both repeated builds produced identical packs. Strict mode accepted the
  ModelEngine fixture. It rejected the BetterModel fixture's missing source
  texture and preserved the diagnostic pack byte for byte.
- 80 tests in 18 suites passed locally. New regressions exercise independent
  composite GUI transforms and shared depth, mixed sprite/cuboid children,
  separate held-item poses, fixed rotations and left-hand fallbacks.
- 163 source-manifest entries covering 135 distinct copied files and four
  original model files were hash-verified unchanged. Third-party source packs,
  models, server configurations and raw logs are excluded from the repository.

The development [artifact](../artifacts/README.md) has SHA-256
`152fd07575e4a2c26b910f5ba9974ebf08c6e501eeb04e43f9c4c8f1010f24c5`.
Baseline captures use the preceding `8cb2ceff…` build; each published pair records
its full build hash in [results.json](images/acceptance/2026-09-30-composites/results.json).

## Client observations

| Java reference | Bedrock before | Bedrock after |
| --- | --- | --- |
| Java demon knight | Detached head parts before correction | Assembled head after correction |
| Java blue wizard | Incorrect head tilt before correction | Corrected child pose |

| Sample | Checks | Observed result |
| --- | --- | --- |
| blue_wizard | idle, walk | Incorrect head tilt corrected; mesh remains assembled. |
| demon_knight | idle, walk, guard, hammer_attack_1 | Detached head/horn pieces corrected; guard and attack poses reach Bedrock. |
| bl_earth_small_spider | idle, walk, fangs_attack | Geometry visible in both clients; pose samples retained for comparison. |
| angel_gm_lancer_one | idle, walk, attack2 | Geometry and sampled pose changes visible in both clients. |
| MythicMobs lancer | stationary invulnerable carrier and ModelEngine binding | Full model visible in both clients. |
| 36-item inventory | weapons, furniture, cosmetics, model parts, icons | 35 Bedrock previews rendered; 34 have usable Java references. See exceptions below. |
| Six held items | lance, crossbow, dagger, staff, pickaxe, shield | Models visible; first-person angle, position and scale still differ. |
| 24 emoji | four lines with adjacent `Agjp` text | Glyphs transfer; baseline, size and lower-line clipping remain differences. |

The final MythicMobs pair was retaken on 1 October. The saved carrier had lost
its ModelEngine binding after the isolated server restart, before conversion;
it was not counted as a working sample. A fresh stationary MythicMobs spawn
restored the provider binding, verified through ModelEngine's API before capture.
Provider binding persistence across restart remains unverified.

Inventory slot 26 (`spectra_aurelium_skills:skill_ship`) refers to `#missing`;
Java shows the missing-texture pattern and diagnostic Bedrock output leaves
unmapped paper. Slot 19 (`iasurvival:item/shields/ruby_shield`) has a missing-texture
Java reference in this fixture while Bedrock renders a red shield. Its Java
reference needs investigation; it is **not** counted as a visual parity pass.
Neither source was modified to make the test pass.

| Java inventory | Bedrock inventory |
| --- | --- |
| 36 Java references including two missing-texture cells | 35 rendered previews and one unmapped paper item |

World cameras used the same commanded positions and directions in the two
clients. Client aspect ratios, UI scales and renderers differ. After-fix
BetterModel animations ran normally and were captured sequentially; their
screenshots do not establish synchronized frame timing. Earlier zero-speed
BetterModel samples are retained only to show the composite assembly defect.
Spectator cameras can reveal a translucent invisible carrier on Java.

The item tests use paper with the selected item-model component. They validate
appearance, not weapon damage, crossbow loading or production plugin gameplay.
The inventory is a test container, not evidence that custom menu backgrounds or
skills behave identically. Previous [menu failures](BROAD_CONTENT_2026-09-29.md)
remain open. Full animated-texture playback, tint/material equivalence,
first-person pose parity and glyph/UI layout are not accepted by this checkpoint.

## Evidence and test safety

Client connection and dialog handling used reusable scripts. Capture automation
now checks that both test players are still connected before taking a pair;
disconnected-menu captures and a paused held-item capture were excluded and
retaken. Fixtures use stationary, invulnerable carriers. Test players use
creative/spectator mode, return to a safe platform, and have zero deaths in all
ten saved player-stat records.

Published images are unchanged captures, with per-file hashes in
[sha256.json](images/acceptance/2026-09-30-composites/sha256.json), individual
results in [results.json](images/acceptance/2026-09-30-composites/results.json), and
sanitized counts and source-integrity checks in
[measurements.json](images/acceptance/2026-09-30-composites/measurements.json).
This is a tested development correction, not a claim of perfect conversion for
every model or animation.
