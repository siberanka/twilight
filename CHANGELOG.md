# Changelog

All notable changes in Twilight are documented here.

## 1.0.0-pre.7 - 2026-10-04

- Treat each provider's generated pack (CraftEngine `resource_pack.zip`,
  ItemsAdder output, Nexo/Oraxen packs) as what Java players receive: it now
  outranks the provider's working folders. Packs of providers that are not
  installed, or whose settings do not send them while another provider sends a
  generated pack, only fill gaps. Discover CustomNameplates and BetterHUD packs
  and ignore Nexo's vanilla asset cache.
- Show datapack and plugin biomes on Bedrock as the vanilla biome with the
  closest grass, foliage, water and fog colours and precipitation, instead of
  Geyser's ocean fallback (`world.bedrock-biome-matching`).
- Merge the resource packs' `lang` files like Java and give them to Geyser, so
  Bedrock players see the names of datapack and plugin content and translation
  overrides such as an image as the ender chest title or a hidden inventory label
  (`ui.java-translations`).
- Darken the images of container titles without a colour code as Java does
  (Java multiplies glyphs by the default title colour 0x404040; Bedrock never
  tints resource-pack glyphs), using pre-darkened copies (`ui.java-glyph-tint`).
- Keep the origin spacers of bold titles unstyled; Bedrock drew them wider and
  moved bold titles about four units to the right. Legacy colour codes inside
  laid-out text no longer take space.
- Keep a blank inventory label blank on Bedrock (Bedrock treated an empty
  translation as missing).
- Verify the UI conversion with 104 menus of two real servers and an original-art
  style suite: every window and 97 of 104 title bands match Java; layers moved back
  over an earlier image remain unsupported. Complete builds of six real servers and
  a live custom-biome scene are in the [UI campaign](docs/UI_CAMPAIGN_2026-10-04.md).
  Older screenshots were retired in favour of current captures. 156 tests pass.

## 1.0.0-pre.6 - 2026-10-04

- Lay out Java text on every Bedrock text surface: chat, action bar, titles,
  boss bars, scoreboards, entity names, text displays and other container titles.
  Named fonts, remapped characters and space shifts now show the right images at
  Java's positions; centred lines follow Java's integer centring. Controlled by
  `ui.java-text-surfaces`; `ui.java-text-layout` no longer requires the container
  layout.
- Stop treating ItemsAdder's vanilla asset copies, temporary build folders and
  stale nested packs as sources; they hid hundreds of custom items. Use a renamed
  ItemsAdder output pack when `generated.zip` is absent.
- Resolve texture atlas sprite renames, decode protected PNGs like Java, read
  object-form model textures, allow vanilla models behind custom selectors and use
  Java's missing texture for undefined face textures.
- Keep characters from Java's own font sheets as Bedrock text and treat
  off-screen or transparent spacing images as advances, removing alias-page
  overflow on real servers.
- Report content Java rejects as well (malformed fonts, unreadable TrueType files,
  absent sound files, skin-rendered heads) as notices instead of failing strict
  builds.
- Verify every surface live with independent SkyBlock content and run complete
  production builds for five real servers; see the
  [text surface review](docs/TEXT_SURFACES_2026-10-04.md). 138 tests pass.

## 1.0.0-pre.5 - 2026-10-03

- Lay out chest titles for Bedrock players with Java font metrics. Negative and
  custom spacing (space providers, negative-height bitmaps), glyph bearings and
  characters Java remaps in the default or named fonts are reproduced with
  invisible spacer glyphs and private-use aliases. Real menu images now land on
  Java's exact GUI pixels, including the former one-unit offset. Controlled by
  `ui.java-text-layout`; the touch layout receives glyph substitution only.
- Combine font definitions from every resource pack like Java, and ignore
  TrueType fonts Java cannot load.
- Verify six real Survival menus, every chest size and unchanged hopper, furnace
  and dispenser screens in paired captures; see the
  [text layout review](docs/TEXT_LAYOUT_2026-10-03.md). 109 tests pass.

## 1.0.0-pre.4 - 2026-10-03

- Generate a Java container layout for Bedrock desktop chest screens. Wide
  font-image titles are no longer wrapped, hyphenated or clipped. Title and
  inventory labels use Java's positions, colour and drawing order, and chest,
  inventory and hotbar rows keep Java's spacing for 1 to 6 chest rows. The
  partial UI is merged into Bedrock's vanilla UI and can be disabled with
  `ui.java-container-layout`.
- Verify all six chest sizes and hopper, dispenser and furnace screens in paired
  Java/Bedrock captures with scripted measurements; record Bedrock's untinted
  bitmap glyphs and one-unit glyph offset in the
  [container layout review](docs/CONTAINER_LAYOUT_2026-10-03.md). 95 tests pass.

- Automatically enlarge bitmap-font atlas cells while retaining each glyph's
  Java display dimensions and ascent. Wide rank labels and larger UI/HUD images
  no longer need to be shrunk or omitted solely because they exceed 16 pixels.
- Add pixel-preservation and neighboring-glyph regressions; all 87 tests pass.
  Repeat whole-source audits and compare six further real UI/HUD images in both
  clients. Record remaining title clipping, horizontal padding and fractional
  sampling differences in the [wide glyph review](docs/WIDE_GLYPHS_2026-10-02.md).
- Expand the acceptance contract to 40 areas and 289 required scenarios after
  reviewing external feature documentation and the available menu corpus.
  Add namespace-collision and multi-row glyph-sheet regressions; the local
  suite now passes 89 tests. This expands validation, not runtime UI support.

## 1.0.0-pre.3 - 2026-10-02

- Preserve Java bitmap glyph heights and ascents with measured Bedrock cell
  coordinates. Support representable negative ascents and reject visible
  overflow without silently shrinking or clipping; isolate transparent padding
  from neighboring characters.

- Preserve each static composite child's display transforms in display entities,
  attachables and inventory previews. This fixes detached BetterModel head parts
  and unintended head/body tilt without changing provider assets. Generated
  sprite children remain visible beside cuboid children.

- Render custom inventory icons from Java model geometry and inherited GUI
  transforms instead of the first texture. Preserve face UVs, UV quarter turns,
  element rotation/rescale, depth and transparency in static previews.

- Accept Java-compatible dotted and uppercase pack overlay directory names;
  retain traversal protection. Extended real-source testing exposed versioned
  directories that previously stopped Survival content discovery.
- Match Java's full-height passenger attachment for cloud-mounted item displays.
  This corrects their vertical position and the potion basket's dark lighting
  above solid blocks, without changing textures, shaders or brightness overrides.
- Automatically represent invisible, zero-radius Java cloud anchors with inert
  Bedrock actors. Preserve cloud metadata and mounted passengers across changes;
  ordinary clouds retain Geyser translation. This removes the unwanted particles
  observed around ModelEngine models without editing provider assets.
- Keep item-display head yaw aligned with body yaw and apply entity yaw once,
  correcting mounted BetterModel models that faced away from their Java reference.
- Add a seven-model Java/Bedrock comparison with captured failures and follow-up
  evidence; lighting and full pose parity remain acceptance gaps.
- Match Java display visibility: ignore base entity invisibility and hide
  zero-view-range displays while retaining their current item and transform.
- Added a development Geyser item-display bridge with separate quaternion
  interpolation, Java item-frame rotation, and correct start/duration semantics.
- Added live BetterModel and ModelEngine bone-item registry discovery. Strict
  conversion rejects incomplete provider discovery.
- Select pack overlays by the target Minecraft format and declaration order;
  inactive overlays are no longer imported as independent packs.
- Resolve explicit vanilla model textures and their frame metadata from the
  verified client cache while preserving authored texture overrides.
- Require a restart for display-index changes, and validate pack ZIP data before
  replacing deployed files.
- Corrected custom third-person item rotation axes, signs, and model-frame
  conversion to preserve Java display orientation, including compound angles.
- Applied Java's left-hand mirroring to explicitly authored left-hand poses.
- Fixed quaternion-to-Euler conversion at the positive 90-degree singularity,
  which could reverse an item's orientation.
- Added regressions for yaw, all three orientation axes, explicit left-hand
  transforms, and both Euler singularities. Full in-game visual parity remains
  unverified.
- Corrected rotated cuboid X/Y signs, preserved face UV rotations, and derived
  omitted face UV rectangles from Java element bounds.
- Sampled the first authored animation frame for static texture export instead
  of stretching a whole animation sheet; tall static PNGs are no longer cropped.
- Rejected oversized GUI bitmap glyphs and custom spacing in strict mode instead
  of silently shrinking menus into emoji-sized cells. Diagnostic builds report
  these omissions; full Bedrock GUI adaptation remains unsupported.
- Reported a required server restart when custom item mappings differ from the
  running Geyser registry. Texture-only reloads remain available; configuration
  reloads no longer clear the pending restart requirement.
- Added a JVM-locale diagnostic for Geyser's Turkish/Azeri enum-parsing issue.
- Skipped optional event registration for disabled providers, avoiding closed
  classloader errors when an incompatible ModelEngine version fails to enable.
- Disabled automatic GitHub and GitLab pipeline triggers. Builds are run and
  tested locally under siberanka; pushes do not run hosted builds.

## 1.0.0-pre.2 - 2026-09-22

### Java-to-Bedrock presentation

- Fixed custom tool and weapon pose selection by deriving Geyser's handheld
  presentation from the resolved Java model parent chain instead of the base
  Minecraft item identifier.
- Preserved authored Java first- and third-person translation, rotation, and
  scale without automatic geometry fitting that could shrink or reposition a
  model into an incorrect held pose.
- Fixed chat emoji height by composing every Bedrock Unicode page on a stable
  16-pixel grid and bottom-aligning each glyph independently, including pages
  that also contain oversized GUI glyphs.

### Validation

- Added regressions for model-parent pose selection, exact hand transforms,
  and mixed-size bitmap glyphs sharing one Unicode page.
- Removed pre-fix pose and emoji captures from the public acceptance gallery.

## 1.0.0-pre.1 - 2026-09-21

### Server platform

- Added one server plugin for Paper, Folia, and Spigot with a Bukkit service
  API, operation events, commands, structured logs, and controlled Geyser
  reloads.
- Added deterministic provider discovery, lifecycle hooks, command
  synchronization, source fingerprints, and delayed settle checks for
  ItemsAdder, CraftEngine, Nexo, Oraxen, ModelEngine, and BetterModel.
- Added authored `contents` and `resources`, provider `data` and `cache`,
  datapack, configured source, and generated-pack fallback layers.

### Java-to-Bedrock conversion

- Added current and legacy item-definition handling, Geyser custom mappings,
  layered textures, Java cuboids, display transforms, equipment attachables,
  block states, creative metadata, and animated textures.
- Preserved Bedrock's native pose, pull, charge, cast, and line behavior for
  exact texture-only bow, crossbow, and fishing-rod recolours.
- Added runtime-selected Java state geometry for layered, transformed,
  animated, and volumetric weapons, including distinct crossbow arrow and
  rocket states.
- Added bitmap-font conversion with Unicode collision protection and layered
  custom-sound conversion with recursive references and OGG validation.
- Added hash-verified, version-matched Mojang model, texture, font, and sound
  fallback for explicitly referenced vanilla assets.

### Reliability

- Added strict publication validation, bounded resource names, safe path and
  archive handling, deterministic output, transactional deployment,
  last-known-good restoration, and bounded backups.
- Added focused source, font, and sound audits plus 28 automated tests for the
  current server compiler and deployment path.
- Added GitHub Actions and GitLab CI pipelines that build and test the same
  tagged source before publishing `Twilight.jar` and its SHA-256 checksum.
