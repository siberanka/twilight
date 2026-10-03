# Twilight 1.0.0-pre.4 prerelease build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
95 tests across 20 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and
[visual review](../docs/REAL_CONTENT_REVIEW.md) before deployment.
This prerelease adds a Java container layout for Bedrock desktop chest screens:
font-image menu titles are no longer clipped, and titles, labels and slot rows
keep Java's positions for 1 to 6 chest rows. See the
[container layout review](../docs/CONTAINER_LAYOUT_2026-10-03.md).

It also includes the live display bridge and ModelEngine visibility,
display-facing, automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md)
and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md).
The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records
remaining defects. Adaptive font cells preserve wide ranks and larger UI/HUD
images at their declared size; see the [wide glyph review](../docs/WIDE_GLYPHS_2026-10-02.md).

Full animation, first-person, chat, UI and entity parity remain incomplete.
Bitmap glyph tint, a one-unit glyph offset, transparent left padding, Java
spacing advances and fractional sampling still differ. Other container types,
touch layouts, tooltips and live HUDs keep their current limitations.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md), the
[new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) and the
[font metrics regression](../docs/FONT_METRICS_2026-10-01.md) document earlier
item, glyph and menu results.
