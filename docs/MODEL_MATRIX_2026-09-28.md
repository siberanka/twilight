# Additional real-model review — 2026-09-28

**Overall visual acceptance remains incomplete.** All seven additional models produced visible
Bedrock geometry, but the comparison exposed lighting, particle and orientation
differences. A successful conversion count is not a successful visual test.
The BetterModel facing defect was subsequently corrected and retested below.
This review does not approve a release.

The operator supplied seven unchanged models from two local server installations.
Source and copied model SHA-256 values matched for every selected asset. Java used
the provider-generated original resource packs, with their bytes verified after
copying. The source servers were read-only; all spawning, conversion and diagnostic
changes took place on isolated local test servers.

## Baseline coverage and results

| Provider | Model | Sampled states | Observation |
|---|---|---|---|
| ModelEngine R4.1.1 | `bear_brown` | idle, attack | Assembled body, fish and attack pose visible; unwanted black particles in Bedrock. |
| ModelEngine R4.1.1 | `crab_hermit` | idle, hide | Body and shell visible; black particles obscure the small model. |
| ModelEngine R4.1.1 | `angel_gm_archer_one` | idle, attack1 | Body, wings and bow visible; particles and a pose/orientation discrepancy prevent acceptance. |
| ModelEngine R4.1.1 | `basket_with_health_potions` | static | Recognizable geometry, but almost black in Bedrock while brightly textured in Java; unwanted particles also present. |
| BetterModel 3.4.2-SNAPSHOT-516 | `pet_griffon_phoenix` | idle, interact | Textured body, wings and staff visible; orientation differs from Java. |
| BetterModel 3.4.2-SNAPSHOT-516 | `blacksmith_hm5_npc` | idle, wave | NPC and anvil visible; orientation differs from Java. |
| BetterModel 3.4.2-SNAPSHOT-516 | `owl_crate` | idle, open | Chest geometry and opening movement visible; orientation differs from Java. |

The matrix contains 13 Java/Bedrock pairs (26 original captures). Each pair uses
the same requested player location, yaw and pitch. Client FOV, resolution and
rendering differ; these are visual comparisons, not pixel equality tests.
ModelEngine animation clocks were frozen after a short advance. BetterModel
captures are sequential live samples and **do not establish matching animation
frames or timing**. No complete animation cycle or arbitrary-model guarantee is
inferred from these samples.

The two complete test packs converted 172/172 and 70/70 volumetric candidates,
respectively, with zero compiler problems. These counts include existing fixtures
and individual bone meshes; they are not counts of accepted mobs. The baseline
development JAR SHA-256 is
`ba1f08919c262375c866ae21f6f221221da513686b14d2f2e7b7192413932817`.

## Facing correction and follow-up

The runtime probe found display body yaw at -180 degrees while head yaw remained
zero. Java item displays have no independent head, but Bedrock rotated the mounted
mesh toward that head heading. The bridge now aligns head yaw with body yaw at
spawn and in relative/absolute movement. It ignores independent head-look updates.
The generated rig also stops applying actor yaw a second time.

The rebuilt JAR passed all 65 local tests across 16 suites. Its SHA-256 is
`ffa682351d2893e8b2bda5cb3c8d4acc52cd651267ce584cbd8768daf5e6fb5f`.
A full BetterModel conversion produced 70/70 meshes without compiler problems,
was deployed, and both clients rejoined through the connection script. The manual
probe pack was replaced by the compiler-generated pack before this follow-up.

All three BetterModel models now face the Java reference in the sampled scenes.
The owl also matched the reference heading after carrier rotations to 0, 90 and
-45 degrees, followed by restoration to 180 degrees. These checks exercise fresh
spawn and subsequent rotation updates. They do not establish frame-perfect
animation playback, pitch behavior, mount offsets or lighting parity.

The first four follow-up pairs were captured in rain. Weather was cleared during
the owl idle pair; the three dedicated heading comparisons used clear weather.
World weather differences are not treated as model conversion results. The NPC
and pet poses are still sequential live animation samples, not synchronized frames.
See the [follow-up results](images/acceptance/2026-09-28-matrix/yaw-fix/results.json)
and [hash manifest](images/acceptance/2026-09-28-matrix/yaw-fix/sha256.json).

| Java heading reference | Bedrock after correction |
|---|---|
| ![Owl yaw 0 Java](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw-0-java.png) | ![Owl yaw 0 Bedrock](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw-0-bedrock.png) |
| ![Owl yaw 90 Java](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw-90-java.png) | ![Owl yaw 90 Bedrock](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw-90-bedrock.png) |
| ![Owl yaw -45 Java](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw--45-java.png) | ![Owl yaw -45 Bedrock](images/acceptance/2026-09-28-matrix/yaw-fix/owl-yaw--45-bedrock.png) |

The same new JAR was also rebuilt into the ModelEngine pack (172/172 meshes,
zero compiler problems), deployed, and tested after a full restart and fresh
client join. All four models remained visible across seven paired samples.
Unwanted black particles and the basket's severe darkening persisted; full pose
acceptance remains open. These follow-up captures are included in the same
machine-readable results and image manifest.

## Reproduction and limits

Use licensed local copies of the selected models with their original provider.
Attach each model to a stationary invulnerable carrier with AI and gravity
disabled, enable creative mode for both test players, and download the converted
pack through Geyser. Reconnect through the local client-join automation, then
capture the same camera coordinates in each client. Restore idle after sampling.
The private `ai/MODEL_MATRIX.md` describes the automated local runner and its
guards; neither it nor the private asset packs are published.

Both players had 20/20 health after the capture sequences. All saved player death
counters in both test worlds remained zero. Production combat skills were not
imported. This is a direct-provider model test; the earlier MythicMobs integration
probe remains documented in [the real-content review](REAL_CONTENT_REVIEW.md).

The basket also triggered provider warnings about its authored eye height and
duplicate bone names. Its source was left unchanged. A controlled lighting probe replaced only the 25
stone platform blocks beneath it with glass. Bedrock then displayed normal basket
and bottle colors without changing the model, texture or Java pack. All 25 blocks
were restored to stone afterward. This implicates light sampling/occlusion around
the display anchor; it does not identify a complete generic correction. Zero-radius ModelEngine pivot clouds and Geyser's
minimum cloud radius are a candidate explanation for the unwanted particles,
not a confirmed fix. No blanket suppression of gameplay particles was introduced.

The screenshots are compatibility evidence for operator-provided artwork. They
do not license redistribution of the original models, textures or generated
packs. BetterModel is by toxicity188 and contributors, ModelEngine by Ticxo,
and MythicMobs by Lumine; artwork remains attributable to its respective creators.

## Paired captures

Java is the left column; Bedrock is the right column. See the [machine-readable results](images/acceptance/2026-09-28-matrix/results.json) and [image hashes](images/acceptance/2026-09-28-matrix/sha256.json).

| Java | Bedrock |
|---|---|
| ![bear_brown idle Java](images/acceptance/2026-09-28-matrix/modelengine-bear_brown-idle-java.png) | ![bear_brown idle Bedrock](images/acceptance/2026-09-28-matrix/modelengine-bear_brown-idle-bedrock.png) |
| ![bear_brown attack Java](images/acceptance/2026-09-28-matrix/modelengine-bear_brown-attack-java.png) | ![bear_brown attack Bedrock](images/acceptance/2026-09-28-matrix/modelengine-bear_brown-attack-bedrock.png) |
| ![crab_hermit idle Java](images/acceptance/2026-09-28-matrix/modelengine-crab_hermit-idle-java.png) | ![crab_hermit idle Bedrock](images/acceptance/2026-09-28-matrix/modelengine-crab_hermit-idle-bedrock.png) |
| ![crab_hermit hide Java](images/acceptance/2026-09-28-matrix/modelengine-crab_hermit-hide-java.png) | ![crab_hermit hide Bedrock](images/acceptance/2026-09-28-matrix/modelengine-crab_hermit-hide-bedrock.png) |
| ![angel_gm_archer_one idle Java](images/acceptance/2026-09-28-matrix/modelengine-angel_gm_archer_one-idle-java.png) | ![angel_gm_archer_one idle Bedrock](images/acceptance/2026-09-28-matrix/modelengine-angel_gm_archer_one-idle-bedrock.png) |
| ![angel_gm_archer_one attack1 Java](images/acceptance/2026-09-28-matrix/modelengine-angel_gm_archer_one-attack1-java.png) | ![angel_gm_archer_one attack1 Bedrock](images/acceptance/2026-09-28-matrix/modelengine-angel_gm_archer_one-attack1-bedrock.png) |
| ![basket_with_health_potions static Java](images/acceptance/2026-09-28-matrix/modelengine-basket_with_health_potions-static-java.png) | ![basket_with_health_potions static Bedrock](images/acceptance/2026-09-28-matrix/modelengine-basket_with_health_potions-static-bedrock.png) |
| ![pet_griffon_phoenix idle Java](images/acceptance/2026-09-28-matrix/bettermodel-pet_griffon_phoenix-idle-java.png) | ![pet_griffon_phoenix idle Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-pet_griffon_phoenix-idle-bedrock.png) |
| ![pet_griffon_phoenix interact Java](images/acceptance/2026-09-28-matrix/bettermodel-pet_griffon_phoenix-interact-java.png) | ![pet_griffon_phoenix interact Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-pet_griffon_phoenix-interact-bedrock.png) |
| ![blacksmith_hm5_npc idle Java](images/acceptance/2026-09-28-matrix/bettermodel-blacksmith_hm5_npc-idle-java.png) | ![blacksmith_hm5_npc idle Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-blacksmith_hm5_npc-idle-bedrock.png) |
| ![blacksmith_hm5_npc wave Java](images/acceptance/2026-09-28-matrix/bettermodel-blacksmith_hm5_npc-wave-java.png) | ![blacksmith_hm5_npc wave Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-blacksmith_hm5_npc-wave-bedrock.png) |
| ![owl_crate idle Java](images/acceptance/2026-09-28-matrix/bettermodel-owl_crate-idle-java.png) | ![owl_crate idle Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-owl_crate-idle-bedrock.png) |
| ![owl_crate open Java](images/acceptance/2026-09-28-matrix/bettermodel-owl_crate-open-java.png) | ![owl_crate open Bedrock](images/acceptance/2026-09-28-matrix/bettermodel-owl_crate-open-bedrock.png) |

## Basket lighting isolation

| Java over temporary glass platform | Bedrock over the same platform |
|---|---|
| ![Java basket glass lighting probe](images/acceptance/2026-09-28-matrix/yaw-fix/basket-glass-platform-java.png) | ![Bedrock basket glass lighting probe](images/acceptance/2026-09-28-matrix/yaw-fix/basket-glass-platform-bedrock.png) |

Follow-up: the [29 September cloud-anchor regression](CLOUD_ANCHORS_2026-09-29.md)
confirms an automatic plugin fix for the unwanted particles. The subsequent
[mount-height regression](DISPLAY_SEATS_2026-09-29.md) corrects the basket lighting
on unchanged stone. The images here retain the earlier failing baseline.

Changing the platform is a diagnostic step, not a product fix. The normal stone
platform was restored. Unwanted particles remained visible throughout this probe.
