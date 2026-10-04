# Twilight 1.0.0-pre.7 prerelease build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
156 tests across 28 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and the
[UI campaign](../docs/UI_CAMPAIGN_2026-10-04.md) before deployment.
This prerelease converts the packs providers actually deliver to Java players
(CraftEngine, ItemsAdder, Nexo/Oraxen, CustomNameplates, BetterHUD), shows
datapack and plugin biomes as their closest vanilla biome, merges resource-pack
translations, and darkens uncoloured menu titles like Java. 104 real menus of two
servers were compared live: every window and 97 title bands match Java.
It builds on the Java text layout on every Bedrock text surface
([text surface review](../docs/TEXT_SURFACES_2026-10-04.md)), the chest title
layout ([text layout review](../docs/TEXT_LAYOUT_2026-10-03.md)) and the container
layout ([container layout review](../docs/CONTAINER_LAYOUT_2026-10-03.md)).

It also includes the live display bridge and ModelEngine visibility,
display-facing, automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md)
and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md).
The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records
remaining defects. Adaptive font cells preserve wide ranks and larger UI/HUD
images at their declared size; see the [wide glyph review](../docs/WIDE_GLYPHS_2026-10-02.md).

Full animation, first-person, UI and entity parity remain incomplete. Item names
and lore keep raw font characters, layers moved back over an earlier image are not
reproduced, a glyph directly after text without a space differs by a unit, and
high-resolution glyph sampling differs. Touch layouts, tooltips and live HUD state
keep their current limitations.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md), the
[new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) and the
[font metrics regression](../docs/FONT_METRICS_2026-10-01.md) document earlier
item, glyph and menu results.
