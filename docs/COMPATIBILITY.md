# Twilight compatibility contract

This document separates implemented behavior from planned compatibility. A provider is supported only when its public API or generated standard assets pass the same conversion and validation pipeline; directory recognition alone is discovery support.

The [3 October review](COMPATIBILITY_REVIEW_2026-10-03.md) and
[acceptance catalog](acceptance/catalog.json) define the broader required test
surface. Planned scenarios do not change the support statuses below.

## Runtime platforms

| Platform | Status | Notes |
|---|---|---|
| Paper 1.21.4+ | Implemented | Primary compile and runtime target. |
| Folia 1.21.4+ | Implemented core | Global scheduler adapter; conversion work runs on a dedicated worker. Runtime entity work must remain region-safe. |
| Spigot 1.21.4+ | Implemented core | Bukkit scheduler fallback; no Paper-only hard link at runtime. |
| Fabric client | Unsupported | Retired. Twilight is server-only. |

Java 21 is the minimum bytecode level. Local builds use Java 25.

## Source discovery

| Source | Discovery | Conversion |
|---|---|---|
| ItemsAdder contents/data/cache/generated packs/API | Implemented | Authored roots override data/cache; generated ZIP is fallback. First-load, load-data, and pack-compressed events are observed. |
| CraftEngine resources/cache/generated packs/API | Implemented | `loadedItems`, reload, and pack-generation events are supported; richer component mapping remains in progress. |
| Nexo/Oraxen packs/API | Implemented | Standard item assets implemented; Oraxen array registries and item-load/pack events are supported, while version-specific API failures fall back to files and are logged. |
| BetterModel/ModelEngine generated packs | Partial | Static geometry and a development live item-display bridge are implemented. Sampled BetterModel/ModelEngine poses and stationary MythicMobs models verified; full animations remain in progress. |
| Standalone `.bbmodel` sources | Discovered | Source discovery does not imply independent rig/animation conversion; generated Java item assets are currently required. |
| RealisticSeasons pack layers | Implemented | Pack is indexed as a seasonal layer; live season/biome presentation conversion remains in progress. |
| World datapacks | Implemented | `server.properties` `level-name`, Bukkit worlds, dimensions, ZIP/folder datapacks. |
| MythicMobs runtime mobs | Not implemented by Twilight | Provider can spawn Java mobs; cross-client custom entity rendering requires a runtime entity bridge. See the real-content review. |
| Additional configured sources | Implemented | Path containment, link, size, entry, and archive safety checks apply. |

Twilight does not extract protected assets or reproduce paid plugin internals. Integrations use public APIs, documented file formats, and assets available to the server operator.

## Item conversion

Implemented:

- Geyser custom item mapping format v2.
- Modern Java item model roots and Geyser-compatible condition, range, and select predicates.
- Static composites retain separate child geometry and authored display poses
  in item displays, attachables and static inventory previews; generated sprite
  children are retained alongside cuboids. Dynamic composites remain unsupported.
- Legacy numeric custom model data.
- Layered 2D PNG composition and deterministic item atlas entries.
- Java cuboids, default and explicit per-face UVs, face UV rotations, element rotations, texture atlases, Bedrock geometry and attachables.
- First-person right/left, third-person right/left, and head display transforms; authored hand translation, rotation, and scale are preserved without implicit fitting.
- Model-parent-derived handheld presentation, independent of the mapping's vanilla base item.
- Native Bedrock bow/crossbow pose and pull controllers for single-layer texture-only replacements.
- Runtime-selected Java geometry and per-state display transforms for volumetric legacy bow/crossbow pull stages; arrow and rocket charge types remain distinct.
- Generated Bedrock resource paths remain below Geyser's 80-character portability boundary, including runtime state animations.
- Legacy fishing-rod idle/cast variants through Geyser's `fishing_rod_cast` predicate.
- Strict conversion mode and stable pack UUIDs.

Open release gates:

- Full current Java item node/property matrix, including nested dynamic composites and native special renderers.
- Exact GUI rendering for complex 3D models rather than source-texture fallback icons.
- Animated `.mcmeta` to Bedrock flipbook conversion (only the first authored frame currently exports).
- Full visual hand-pose parity, especially first-person framing and complex provider models; see the [real-content review](REAL_CONTENT_REVIEW.md).
- Live acceptance of every provider/version state matrix, including fishing lines, 3D bow/crossbow state transitions, shields, tridents/spears, armor, and elytra.
- Component-rich runtime definitions and creative/recipe presentation for every provider version.

## Other custom content

Supported bitmap providers reachable from `minecraft:default` use adaptive 16-to-512-pixel cells with measured Java height/ascent placement. Each page grows to contain its largest glyph while retaining every glyph's authored display dimensions and baseline. No image is shrunk to fit a smaller cell. Visible overflow beyond these bounds and custom space advances fail strict compilation; diagnostic builds report omissions. See the [wide glyph regression](WIDE_GLYPHS_2026-10-02.md) for native calibration, pixel comparisons and memory costs. Representable negative ascents are supported. Unreadable generated layers can fall back to a valid lower-priority source asset. Collision-free BMP private-use glyphs from named fonts can join the global Bedrock atlas.

When `vanilla-override` is disabled, normal Unicode cells from the Java default font are rejected rather than replacing Bedrock's vanilla glyphs. Differing named-font images that reuse one code point and named glyphs outside the private-use range require an outbound component-remapping bridge, so strict publication currently rejects them.

Explicit `minecraft:` texture references absent from the custom pack are resolved from the running server version's Mojang client JAR. Twilight accepts only official HTTPS hosts and verifies version metadata, advertised size, and SHA-1 before caching the archive. `generation.download-vanilla-assets` can disable network retrieval; an existing verified cache remains usable.

Layered Java `sounds.json` files are merged with `replace` semantics and converted to `sounds/sound_definitions.json`. File and event references, OGG assets, weight, volume, pitch, streaming, and compatible attenuation distances are preserved. Unqualified Java file references correctly resolve through `minecraft`; emitted paths retain a namespace segment to avoid Bedrock file collisions. Explicit vanilla sound dependencies are fetched through the version's SHA-1-verified Mojang asset index. With `vanilla-override` disabled, definitions identical to vanilla are skipped and changed vanilla events reject strict publication.

Desktop chest screens use a generated Java container layout by default (`ui.java-container-layout`). Titles are unwrapped and unclipped, title and inventory labels use Java's positions and drawing order, and chest, player-inventory and hotbar rows keep Java's spacing for 1 to 6 chest rows. Partial UI definitions are merged into Bedrock's vanilla UI; other containers and touch layouts keep the vanilla Bedrock layout. See the [container layout review](CONTAINER_LAYOUT_2026-10-03.md).

Chest titles are laid out for Bedrock players with Java font metrics by default (`ui.java-text-layout`). Font definitions from all packs are combined like Java; space providers, negative-height bitmaps and empty TrueType glyphs become exact advances through invisible spacer glyphs; glyph bearings are corrected; characters that Java remaps in `minecraft:default` or named fonts receive private-use aliases instead of replacing Bedrock's glyphs. Six real menus match Java in the title area; a glyph directly after text, overlapping layers, tint and non-title text are not laid out yet. See the [text layout review](TEXT_LAYOUT_2026-10-03.md).

Compiler diagnostics reject contextual named-font remapping, supplementary-plane code points, out-of-cell baseline controls and unresolved protected/corrupt source PNG data. Runtime font limitations are separate: Bedrock does not tint private-use bitmap glyphs with the text colour, draws each bitmap glyph one GUI unit right of Java and trims transparent left padding, and fractional sampling can lose detail. Outside chest titles, Java space-provider advances and remapped characters are not yet laid out. The latest paired tests cover six metric probes, 36 real glyphs and six additional UI/HUD images. Large images rendering in chat do not establish interactive menu or dynamic HUD parity. Atlas pages can reach 8192 square (256 MiB raw RGBA per page); constrained-device acceptance remains unverified.

Discovery counters also cover sounds, blockstates, `.bbmodel` files, and datapack biome definitions. Production conversion remains gated until each remaining subsystem has structural tests and real Java/Bedrock acceptance evidence:

- named fonts, shader/spacing menus, HUD, and language overrides;
- custom blocks, furniture, mobs, bones, animations, hitboxes, nameplates, and equipment;
- live Bedrock playback acceptance for provider-triggered custom sound events;
- custom biome colors, climates, seasons, and RealisticSeasons transitions;
- custom skulls and waypoint icons.

## Vanilla preservation

`vanilla-override` defaults to `false`. Twilight maps only detected custom item definitions or legacy override values. It does not emit a mapping for an untouched vanilla stack. Simple custom items may reuse vanilla Bedrock presentation semantics; complex custom shapes keep dedicated geometry and transforms.

## Geyser deployment

Twilight owns only `packs/twilight.zip`, `custom_mappings/twilight_*`, and explicitly generated locale override files recorded in its deployment manifest. Deployment stages and hashes all files, snapshots the previous owned set, publishes with atomic replacement where supported, rolls back on failure, and retains three snapshots by default. Deploy and rollback compare mappings with the startup snapshot: a difference reports a required server restart and suppresses the ineffective Geyser reload. Configuration reload preserves that state. Unchanged mappings permit a controlled resource reload. Unrelated Geyser files are never removed.

Provider reload/pack commands and supported completion events are debounced. Twilight hashes the actual source bytes and live item descriptors before rebuilding, skips duplicate deploys when content is unchanged, and performs a delayed settle check for providers that finish writing after their command returns.
