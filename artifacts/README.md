# Twilight 1.0.0-pre.6 prerelease build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
138 tests across 25 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and
[visual review](../docs/REAL_CONTENT_REVIEW.md) before deployment.
This prerelease lays out Java text with Java font metrics on every Bedrock text
surface: chest and other container titles, chat, action bar, titles, boss bars,
scoreboards, entity names and text displays
([text surface review](../docs/TEXT_SURFACES_2026-10-04.md)). Complete builds of
five real servers led to discovery, atlas, PNG and font fixes; one server now
converts 837 of 841 custom items instead of 125.
It builds on the Java chest title layout
([text layout review](../docs/TEXT_LAYOUT_2026-10-03.md)) and container layout
([container layout review](../docs/CONTAINER_LAYOUT_2026-10-03.md)).

It also includes the live display bridge and ModelEngine visibility,
display-facing, automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md)
and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md).
The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records
remaining defects. Adaptive font cells preserve wide ranks and larger UI/HUD
images at their declared size; see the [wide glyph review](../docs/WIDE_GLYPHS_2026-10-02.md).

Full animation, first-person, UI and entity parity remain incomplete. Item names
and lore keep raw font characters, a glyph directly after text without a space
and overlapping layers still differ, and bitmap tint and high-resolution glyph
sampling differ. Touch layouts, tooltips and live HUD state keep their current
limitations.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md), the
[new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) and the
[font metrics regression](../docs/FONT_METRICS_2026-10-01.md) document earlier
item, glyph and menu results.
