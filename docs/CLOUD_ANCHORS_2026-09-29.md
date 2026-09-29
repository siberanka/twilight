# Automatic cloud-anchor adaptation — 29 September 2026

Twilight now removes the unwanted particles around the tested ModelEngine models
automatically. ModelEngine uses invisible Java area-effect clouds with radius zero
as model anchors. Geyser's normal cloud translator clamps the radius to at least
0.5, which made these anchors emit particles on Bedrock.

The display bridge inherits Geyser's cloud translator and selects an invisible
armor-stand actor only when the Java cloud is invisible and its radius is exactly
zero. The Java entity and its metadata remain unchanged. Positive-radius and
visible clouds retain Geyser's normal representation. Live radius/flag changes
switch back automatically. A complete metadata snapshot and passenger links are
restored when replacing the client actor; the server-side mount graph is retained.
There are no model-name exceptions, source edits, operator repair commands, or
resource-pack changes. Another integration's registered cloud translator or custom
spawn definition is left alone.

## Validation

The local Java 25 build passed 67 tests across 17 suites. The isolated Paper 26.2
server used ModelEngine R4.1.1, MythicMobs 5.13.1 and Geyser 2.11.3 build 1247.
It was fully restarted with the new JAR before capturing evidence. Acceptance
used only the plugin implementation; earlier diagnostic packet mutations were
not applied to this process. Read-only runtime inspection recorded the actors
and mount relationships.

Four unchanged models from the previous matrix were sampled in seven paired
Java/Bedrock poses: bear idle/attack, crab idle/hide, archer idle/attack1 and the
static potion basket. Unwanted particles were absent in all seven Bedrock samples;
all four meshes remained visible. An inspection while the models were in range
found 17 adapted anchors and 148 passenger links, with zero broken backreferences.

The separate BetterModel 3.4.2 snapshot test server was also restarted with the
same JAR. Griffon idle/interact, blacksmith idle/wave and owl-crate idle/open
produced six more paired samples. All three meshes remained visible and the
previous facing correction remained intact. These captures sample running
animations sequentially, so differences such as the owl lid being at different
stages are not frame-exact comparisons. Full animation parity remains unaccepted.

A disposable vanilla cloud and invulnerable, stationary pig passenger tested
changes on the same Java entity:

| Java state | Bedrock representation | Result |
|---|---|---|
| Invisible, radius 1.5 | Normal cloud | Particles remain visible |
| Invisible, radius 0 | Invisible armor stand | Particles absent; passenger retained |
| Visible, radius 0 | Normal cloud | Normal Geyser clamping restored |
| Invisible, radius 0 again | Invisible armor stand | Repeat transition passes |
| Invisible, radius 1.5 again | Normal cloud | Prior particle/color metadata retained |
| Reconnect while invisible/radius 0 | Invisible armor stand | Passenger reappears; particles absent |

Each transition retained all 120 in-range cloud passenger links with zero broken
backreferences. Cleanup removed the disposable cloud and passenger, leaving 13
provider anchors and 119 links. Counts differ from the earlier model inspection
because the camera moved and the entity tracker changed the in-range set.
The normal-cloud check establishes preservation of Geyser behavior, not Java/Bedrock
particle-style equivalence.

Both players ended at 20/20 health. All ten saved player death counters across
the two isolated test worlds were zero. Production assets were read-only; all
seven selected source hashes still matched the earlier inventory. No combat
skills were imported. Client launch, connection and known prompt handling used
the private `ai/client-join/join.py` automation documented in
`ai/CLIENT_TEST_AUTOMATION.md`; the new wide pause-menu template was also checked.
The local cloud transition runner changes the disposable Java entity, never
Geyser's live representation.

## Remaining defects

The potion basket still becomes nearly black above the stone platform. Some
crab/archer pose differences also remain. This change does not establish full
animation timing, lighting, material, billboard, first-person, glyph or UI parity.
The [earlier matrix](MODEL_MATRIX_2026-09-28.md) retains the failing baseline and
the lighting-isolation evidence. This is a development checkpoint, not an accepted
release. Original artwork remains owned by its creators; screenshots are
compatibility evidence and do not license redistribution of the source assets.

## Evidence

All captures were visually reviewed. [Results](images/acceptance/2026-09-29-clouds/results.json)
and [SHA-256 hashes](images/acceptance/2026-09-29-clouds/sha256.json) accompany the
thirteen paired samples and five transition/reconnect images (31 PNGs).
The [transition assertions](images/acceptance/2026-09-29-clouds/transitions.json)
record metadata and passenger counts. Reconnection starts fresh metadata; the
previous session's unused effect color is not carried into the new flame cloud.

| Java | Bedrock |
|---|---|
| ![Bear idle Java](images/acceptance/2026-09-29-clouds/modelengine-bear_brown-idle-java.png) | ![Bear idle Bedrock](images/acceptance/2026-09-29-clouds/modelengine-bear_brown-idle-bedrock.png) |
| ![Bear attack Java](images/acceptance/2026-09-29-clouds/modelengine-bear_brown-attack-java.png) | ![Bear attack Bedrock](images/acceptance/2026-09-29-clouds/modelengine-bear_brown-attack-bedrock.png) |
| ![Crab idle Java](images/acceptance/2026-09-29-clouds/modelengine-crab_hermit-idle-java.png) | ![Crab idle Bedrock](images/acceptance/2026-09-29-clouds/modelengine-crab_hermit-idle-bedrock.png) |
| ![Crab hide Java](images/acceptance/2026-09-29-clouds/modelengine-crab_hermit-hide-java.png) | ![Crab hide Bedrock](images/acceptance/2026-09-29-clouds/modelengine-crab_hermit-hide-bedrock.png) |
| ![Archer idle Java](images/acceptance/2026-09-29-clouds/modelengine-angel_gm_archer_one-idle-java.png) | ![Archer idle Bedrock](images/acceptance/2026-09-29-clouds/modelengine-angel_gm_archer_one-idle-bedrock.png) |
| ![Archer attack Java](images/acceptance/2026-09-29-clouds/modelengine-angel_gm_archer_one-attack1-java.png) | ![Archer attack Bedrock](images/acceptance/2026-09-29-clouds/modelengine-angel_gm_archer_one-attack1-bedrock.png) |
| ![Basket Java](images/acceptance/2026-09-29-clouds/modelengine-basket_with_health_potions-static-java.png) | ![Basket Bedrock](images/acceptance/2026-09-29-clouds/modelengine-basket_with_health_potions-static-bedrock.png) |
| ![Griffon idle Java](images/acceptance/2026-09-29-clouds/bettermodel-pet_griffon_phoenix-idle-java.png) | ![Griffon idle Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-pet_griffon_phoenix-idle-bedrock.png) |
| ![Griffon interact Java](images/acceptance/2026-09-29-clouds/bettermodel-pet_griffon_phoenix-interact-java.png) | ![Griffon interact Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-pet_griffon_phoenix-interact-bedrock.png) |
| ![Blacksmith idle Java](images/acceptance/2026-09-29-clouds/bettermodel-blacksmith_hm5_npc-idle-java.png) | ![Blacksmith idle Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-blacksmith_hm5_npc-idle-bedrock.png) |
| ![Blacksmith wave Java](images/acceptance/2026-09-29-clouds/bettermodel-blacksmith_hm5_npc-wave-java.png) | ![Blacksmith wave Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-blacksmith_hm5_npc-wave-bedrock.png) |
| ![Owl idle Java](images/acceptance/2026-09-29-clouds/bettermodel-owl_crate-idle-java.png) | ![Owl idle Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-owl_crate-idle-bedrock.png) |
| ![Owl open Java](images/acceptance/2026-09-29-clouds/bettermodel-owl_crate-open-java.png) | ![Owl open Bedrock](images/acceptance/2026-09-29-clouds/bettermodel-owl_crate-open-bedrock.png) |

| Normal cloud with passenger | Invisible point anchor with passenger |
|---|---|
| ![Positive radius](images/acceptance/2026-09-29-clouds/transition-positive-bedrock.png) | ![Zero radius invisible](images/acceptance/2026-09-29-clouds/transition-zero-invisible-bedrock.png) |
| ![Visible zero radius](images/acceptance/2026-09-29-clouds/transition-zero-visible-bedrock.png) | ![Reconnect](images/acceptance/2026-09-29-clouds/transition-reconnect-bedrock.png) |

![Normal cloud restored](images/acceptance/2026-09-29-clouds/transition-restore-bedrock.png)
