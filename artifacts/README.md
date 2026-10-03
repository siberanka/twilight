# Twilight 1.0.0-pre.5 prerelease build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
109 tests across 22 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and
[visual review](../docs/REAL_CONTENT_REVIEW.md) before deployment.
This prerelease lays out chest titles with Java font metrics: negative spaces,
ItemsAdder offsets, remapped default-font characters and glyph bearings, so real
menu art lands on Java's GUI pixels ([text layout review](../docs/TEXT_LAYOUT_2026-10-03.md)).
It builds on the Java container layout for desktop chest screens
([container layout review](../docs/CONTAINER_LAYOUT_2026-10-03.md)).

It also includes the live display bridge and ModelEngine visibility,
display-facing, automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md)
and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md).
The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records
remaining defects. Adaptive font cells preserve wide ranks and larger UI/HUD
images at their declared size; see the [wide glyph review](../docs/WIDE_GLYPHS_2026-10-02.md).

Full animation, first-person, chat, UI and entity parity remain incomplete.
Bitmap glyph tint, a glyph directly after text without a space, overlapping
title layers, text outside chest titles and fractional sampling still differ. Other container types,
touch layouts, tooltips and live HUDs keep their current limitations.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md), the
[new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) and the
[font metrics regression](../docs/FONT_METRICS_2026-10-01.md) document earlier
item, glyph and menu results.
