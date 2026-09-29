# Twilight 1.0.0-pre.3 development build (not released)

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build --offline --no-daemon --no-configuration-cache`.
67 tests across 17 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md) and
[visual review](../docs/REAL_CONTENT_REVIEW.md) before deployment.
This development checkpoint includes the live display bridge and ModelEngine
visibility, display-facing and automatic [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md). The [additional model matrix](../docs/MODEL_MATRIX_2026-09-28.md) records remaining defects. Full animation, first-person, chat, UI, and entity
parity remain incomplete. It is not an accepted release.
