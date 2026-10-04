# Twilight 1.0.0-pre.7 - prerelease

This prerelease converts the content Java players actually receive from CraftEngine,
ItemsAdder, Nexo and other providers, shows datapack biomes and pack translations on
Bedrock, and was verified with 104 real menus and an original-art menu style suite.

- Treated each provider's generated pack (CraftEngine `resource_pack.zip`,
  ItemsAdder output, Nexo/Oraxen packs) as authoritative over its working folders.
  Packs of providers that are not installed, or that do not send their pack while
  another provider does, only fill gaps. CustomNameplates and BetterHUD packs are
  discovered; Nexo's vanilla asset cache is ignored. Complete builds of six real
  servers now convert, for example, 1,905 of 1,932 custom items on Survival.
- Showed datapack and plugin biomes (Terralith, Incendium, RealisticSeasons) on
  Bedrock as the vanilla biome with the closest colours and precipitation instead of
  Geyser's ocean fallback (`world.bedrock-biome-matching`).
- Merged the resource packs' translations like Java: Bedrock players see the names of
  datapack and plugin content and pack overrides such as an image as the ender chest
  title or a hidden inventory label (`ui.java-translations`).
- Darkened the images of container titles without a colour code like Java, with
  pre-darkened glyph copies (`ui.java-glyph-tint`; about 330 MiB more atlas memory on
  the largest tested server).
- Kept bold titles at Java's position (they were four units to the right) and made
  legacy colour codes in laid-out text take no space.

Results: every window of the 104 real menus and 97 title bands matched Java; the
other seven differ only in Bedrock's own text font. In the style suite every
technique matched except layers moved back over an earlier image, which Bedrock
text cannot reproduce. A custom datapack biome showed its closest vanilla biome
(cherry grove) instead of ocean. See the [UI campaign](docs/UI_CAMPAIGN_2026-10-04.md).
Item names and lore are not rewritten, and high-resolution glyph sampling differs.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
156 tests across 28 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The display adapter, text layout, biome and translation bridges
target Geyser 2.11.3 build 1247; other Geyser core versions require validation.

The notes below describe previous prereleases.

## Twilight 1.0.0-pre.6 - prerelease

This prerelease shows Java font content correctly on every Bedrock text surface
and fixes conversion defects found by complete builds of five real servers.

- Laid out Java text for Bedrock players in chat, the action bar, titles and
  subtitles, boss bars, scoreboards, entity names, text displays and the titles of
  hoppers, furnaces and other containers. Named-font images (for example
  CraftEngine ranks), remapped characters and ItemsAdder offsets now appear at
  Java's positions; centred lines follow Java's integer centring and chat keeps
  its spaces for wrapping. Controlled by `ui.java-text-surfaces`; the text layout
  no longer requires the container layout. See the
  [text surface review](docs/TEXT_SURFACES_2026-10-04.md).
- Stopped treating ItemsAdder's vanilla asset copies, temporary build folders and
  stale nested packs as sources (they hid hundreds of custom items) and used a
  renamed ItemsAdder output when `generated.zip` is absent.
- Resolved texture atlas sprite renames, decoded protected PNGs like Java, read
  object-form model textures, allowed vanilla models behind custom selectors and
  used Java's missing texture for undefined face textures.
- Kept characters from Java's own font sheets as Bedrock text and treated
  off-screen or transparent spacing images as advances, which removed
  private-use page overflow on real servers.
- Reported content Java rejects as well (malformed fonts, unreadable TrueType
  files, sound files present in no pack, skin-rendered heads) as notices in
  `build-report.json` instead of failing strict builds.

Every new surface measured zero offset against Java with independent SkyBlock
content (CraftEngine ranks and icons, CustomNameplates backgrounds); the six real
Survival menus and the typed container screens were unchanged. Complete builds of
the same five servers improved, for example from 125 of 130 to 837 of 841 custom
items on Survival. Remaining differences: item names and lore are not rewritten,
a glyph directly after text without a space and overlapping layers are one unit
off, and bitmap tint and high-resolution glyph sampling differ.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
138 tests across 25 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The display adapter and text layout target Geyser 2.11.3 build
1247; other Geyser core versions require validation.


## Twilight 1.0.0-pre.5 - prerelease

This prerelease lays out Java font-image menu titles on Bedrock with Java font
metrics, so real menu art lands on Java's GUI pixels.

- Laid out chest titles for Bedrock players with Java font metrics. Space
  providers, negative-height bitmap shifts and ItemsAdder offsets become exact
  invisible spacers; bitmap glyph bearings are corrected (removing the former
  one-unit offset); characters Java remaps in the default or named fonts receive
  private-use aliases instead of replacing Bedrock's glyphs. Controlled by
  `ui.java-text-layout`. See the [text layout review](docs/TEXT_LAYOUT_2026-10-03.md).
- Combined font definitions from every resource pack like Java and ignored
  TrueType fonts that Java cannot load.
- Kept the touch layout's native centred title with glyph substitution only.

Six real Survival menus (negative-height shifts, an ItemsAdder offset and a
512-pixel image) matched Java in the title area and on all measured slot rows;
hopper, furnace and dispenser screens stayed identical to vanilla Bedrock.
Remaining differences: a glyph directly after text without a space is one unit
right, overlapping title layers are not reproduced, bitmap tint differs, and chat,
lore, scoreboards and boss bars are not laid out yet.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
109 tests across 22 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The display adapter and title layout target Geyser 2.11.3 build
1247; other Geyser core versions require validation.

## Twilight 1.0.0-pre.4 - prerelease

This prerelease makes Java font-image menus usable on Bedrock desktop chest
screens and preserves wide bitmap glyphs at their authored size.

- Generated a Java container layout for Bedrock desktop chest screens. Wide
  title images are no longer wrapped, hyphenated or clipped; title and
  inventory labels use Java's positions, colour and drawing order; chest,
  player inventory and hotbar rows keep Java's spacing for 1 to 6 chest rows.
  The partial UI merges into Bedrock's vanilla UI and can be disabled with
  `ui.java-container-layout`. See the [container layout review](docs/CONTAINER_LAYOUT_2026-10-03.md).
- Enlarged bitmap-font atlas cells automatically while keeping each glyph's
  Java display size and ascent, so wide rank labels and large UI/HUD images are
  no longer shrunk or omitted. See the [wide glyph review](docs/WIDE_GLYPHS_2026-10-02.md).
- Added regressions for title placement and layering, slot spacing, vanilla
  defaults for other screens, glyph namespaces, multi-row glyph sheets, wide
  glyph pixels and neighboring cells.
- Expanded the acceptance contract to 40 areas and 289 required scenarios.

Paired Java/Bedrock captures measured every chest size from 9 to 54 slots and
confirmed that hopper, dispenser and furnace screens remain identical to the
vanilla Bedrock UI. Known differences remain: Bedrock does not tint bitmap
glyphs with the text colour, draws each bitmap glyph one GUI unit to the right,
trims transparent left padding and samples fractional sizes differently. Java
space advances, other container types, touch layouts, tooltips, live HUDs,
complete animation and first-person parity are not yet supported.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
95 tests across 20 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The development display adapter specifically targets Geyser
2.11.3 build 1247; other Geyser core versions require validation.


## Twilight 1.0.0-pre.3 - prerelease

This prerelease corrects custom item rotation and texture conversion and makes
unsupported UI conversion and Geyser restart requirements explicit.

- Corrected bitmap glyph baselines using Java height/ascent and measured Bedrock cell coordinates; added negative-ascent, overflow and page-independence regressions. See the [font metrics review](docs/FONT_METRICS_2026-10-01.md).
- Rendered inventory icons from model faces and the inherited Java GUI pose instead of exporting the first raw texture.
- Preserved individual static composite child poses, fixing separated BetterModel
  head pieces and unintended tilt in the [new-sample regression](docs/COMPOSITE_MODELS_2026-09-30.md).
- Corrected third-person model-frame conversion, left-hand mirroring, and an Euler singularity.
- Corrected rotated cuboid axes, default face UVs, and face UV rotation.
- Exported the first authored animation frame instead of stretching sprite sheets; full animation playback remains unsupported.
- Rejected oversized GUI glyphs, visible baseline overflow and custom spacing in strict mode. Diagnostic exports report omissions.
- Preserved mapping restart state across configuration reload, repeated deploys, and rollback. Geyser reload cannot activate changed item mappings.
- Added a warning for JVM locales that break Geyser mapping enum parsing.
- Added a live item-display bridge and verified sampled BetterModel and ModelEngine poses, including stationary MythicMobs models.
- Corrected display invisibility and zero-view-range handling; inactive ModelEngine fire layers no longer appear as stray planes.
- Corrected mounted display facing by aligning head and body yaw and removing duplicate mesh yaw.
- Automatically adapted invisible zero-radius cloud anchors, removing unwanted ModelEngine particles while preserving ordinary clouds and mounted passengers.
- Corrected cloud-mounted display height to match Java, fixing the tested basket's dark appearance on solid ground while retaining day/night lighting.
- Selected resource-pack overlays for the target Minecraft version and resolved explicit vanilla texture dependencies.
- Accepted dotted and uppercase overlay directory names, fixing discovery of versioned Survival content.
- Skipped event hooks for disabled providers.

The [real-content review](docs/REAL_CONTENT_REVIEW.md) includes Java references,
Bedrock observations, and unaccepted chat/UI checks. Java source assets were
unchanged. This checkpoint does not claim full visual parity or production GUI support.
The [additional seven-model matrix](docs/MODEL_MATRIX_2026-09-28.md) records
orientation, lighting and particle defects, with follow-up evidence for corrections.
The [cloud-anchor regression](docs/CLOUD_ANCHORS_2026-09-29.md) covers four ModelEngine
models, live radius/visibility changes, passenger retention and client reconnection.
The [mount-height regression](docs/DISPLAY_SEATS_2026-09-29.md) measures Java's
attachment point and verifies the basket on unchanged stone in daylight and at night.
The [extended content checks](docs/BROAD_CONTENT_2026-09-29.md) cover whole-source
compilation, 36 inventory examples, six additional held items, 28 emoji and Survival menu images; they
confirm that full first-person, animated-texture and menu parity is still absent.

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
84 tests across 19 suites passed. No hosted CI was run. Automatic GitHub/GitLab pipeline
triggers are disabled.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. The development display adapter specifically targets Geyser
2.11.3 build 1247; other Geyser core versions require validation. Full animated
textures, tint, billboard behavior, and UI adaptation remain open acceptance work.

Final font captures verify six exact normalized height/baseline combinations,
35 supported real glyphs and the tested inventory title. Java title color
modulation, wide rank tags and full custom UI backgrounds remain unsupported
or visually different. Oversized content is reported rather than downscaled.
See the [measured comparison](docs/FONT_METRICS_2026-10-01.md) for the precise scope.
