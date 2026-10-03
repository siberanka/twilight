# Twilight 1.0.0-pre.4-SNAPSHOT development build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
89 tests across 19 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and
[visual review](../docs/REAL_CONTENT_REVIEW.md) before deployment.
This development checkpoint includes the live display bridge and ModelEngine
visibility, display-facing, automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md) and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md). The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records remaining defects. Full animation, first-person, chat, UI, and entity
parity remain incomplete. Adaptive font cells now preserve wide ranks and larger
UI/HUD images at their declared size. Native title clipping, contextual tint,
padding and fractional sampling still differ; full UI layout is not supported.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md) document the
overlay-name fix, automatic model-based inventory previews, and current item, glyph and menu limitations.

The [new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) covers the
automatic preservation of individual composite child poses and additional real assets.

The [font metrics regression](../docs/FONT_METRICS_2026-10-01.md) documents automatic
Java height/ascent placement and the new glyph samples.

The [wide glyph review](../docs/WIDE_GLYPHS_2026-10-02.md) records six additional
UI/HUD samples, pixel measurements, memory costs and actual inventory failures.
This snapshot does not replace the published `v1.0.0-pre.3` release assets.
