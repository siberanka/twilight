# Live display bridge (development)

This implementation is under visual validation. It is not a claim of complete
BetterModel, ModelEngine, MythicMobs, or arbitrary Java entity support.

Twilight emits an independent centered geometry for each converted custom mesh.
The Java item geometry and the hand attachable remain separate. A shared Bedrock
client entity selects its mesh and texture using the generated display index.
The bridge reads that index from the **deployed** pack at server startup.
Changing or removing this index requires a full server restart, even if item
mappings are unchanged. Texture-only changes retain the ordinary reload path.

Geyser currently has no public item-display translator registration API. The
adapter therefore compiles against the pinned Geyser core build
`2.11.3-20260925.135253-13`. It registers a Java `item_display` translator only
when no other translator owns that type. Geyser remains responsible for each
connection's spawn, movement, metadata, removal, and dimension lifecycle.
No Java server entities or provider animation states are modified.

Each update carries both translation/scale/quaternion endpoints. The resource
pack uses shortest-arc quaternion SLERP at render time, with a normalized linear
fallback for nearly identical rotations. Left and right rotations remain
separate around nonuniform scale. A packed item/context property keeps the
protocol within Bedrock's 32-property limit, including timing and revision.
The rig includes Java's extra Y=180 item frame after the display transformation.
Explicit and fallback left-hand item contexts both receive Java mirroring.
The display mesh ignores the base entity invisibility flag, as Java does.
Zero view range hides a display without losing its selected item or transform;
restoring the range restores its current appearance. Positive-distance culling
still needs separate client render-distance coverage.

Only start-delay metadata resets the interpolation clock. Duration-only updates
retain both endpoints; pose updates without a start delta retain the existing
clock. Negative delay and interrupted interpolation have dedicated tests.
Quaternion-to-Euler extraction handles both gimbal poles, checked by evaluating
the emitted numeric Molang against independently composed quaternions.

The local real-model probe now assembles the original salamander in both clients;
its attack pose is visible in the reviewed captures. Three stationary carriers
produced 105 per-session displays. This is visual evidence for one real model,
not full provider or arbitrary-model certification.

A later ModelEngine R4.1.1 probe verified two direct API models and one new
MythicMobs model in both clients. A frozen attack pose matched the Java reference
after correcting entity invisibility and zero-view-range handling. See the
[real-content review](REAL_CONTENT_REVIEW.md) for paired images and limits.

Further validation must include mounted-display offsets,
per-viewer changes, negative scale, near-singular rotations, item replacement,
reload/reconnect, and removal. Font/UI adaptation, animated textures, armor-stand
model providers, tinting, billboard constraints and lighting require their own
coverage; they are not implied by successful item-display registration.

## Upstream credits and licensing

- [Geyser](https://github.com/GeyserMC/Geyser), by GeyserMC and contributors,
  copyright 2019–2026, MIT license. Geyser is a compile-only dependency and a
  separately installed runtime plugin. Twilight does not bundle its classes.
- [BetterModel](https://github.com/toxicity188/BetterModel), by toxicity188 and
  contributors, copyright 2024–2026, MIT license. Its public API and protocol
  implementation were inspected to understand display behavior. BetterModel's
  code, JAR, and user model assets are not distributed in Twilight.
- ModelEngine and MythicMobs test installations remain private. Their proprietary
  binaries and source content must not enter public artifacts.

The adapter and compiler code are authored by siberanka under Twilight's
LGPL-3.0-or-later license. See the upstream projects for their complete license
texts and contributor histories.
