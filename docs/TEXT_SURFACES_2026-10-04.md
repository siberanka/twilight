# Java text on every Bedrock surface and production builds - 4 October 2026

Twilight 1.0.0-pre.6 extends the Java font layout from chest titles to the other
places where servers draw custom font content: chat, the action bar, titles and
subtitles, boss bars, scoreboards, entity names and text displays (holograms,
nameplates), and the titles of other containers. The same release fixes several
conversion failures that independent content from four further real servers
exposed. All checks below were run against isolated copies; production servers
were only read.

## Text surfaces

Before this release, only chest titles were rewritten for Bedrock players. Named
fonts and characters Java remaps were sent to Bedrock unchanged, so a CraftEngine
rank image (`七` in its named rank font) appeared in chat as the CJK letter
`七`, and an ItemsAdder HUD in the action bar showed boxes for its offset
characters. Now every supported packet is laid out with the Java metrics already
generated for the pack:

| Surface | Packets | Line model |
| --- | --- | --- |
| Chat | system, player (signed content, sender, target), disguised | left-aligned |
| Action bar | action bar text, overlay system chat | centred |
| Titles | title, subtitle | centred |
| Boss bars | boss bar title | centred |
| Scoreboards | objective title, team display name/prefix/suffix, score display names | centred title, left-aligned lines |
| Entities | custom names, text-display text (multi-line) | centred per line |
| Containers | titles of hoppers, furnaces, dispensers and other non-chest screens | left-aligned |

Left-aligned lines start where their first character needs, so every image keeps
Java's distance to its neighbours. Centred lines are padded with leading or
trailing invisible spacers so Bedrock centres them where Java does; a leading
negative shift therefore moves a HUD image left exactly like on Java. Java
centres at `-width / 2` in integer arithmetic while Bedrock centres exactly, so
lines with an odd Java width receive one more unit of padding; the widths of
ordinary characters for this come from the vanilla client's own font sheets.
Ordinary spaces stay spaces wherever possible, so Bedrock still wraps chat at
them. Text without custom font characters reaches Geyser as the same packet.

`ui.java-text-surfaces` (default `true`) controls the new surfaces;
`ui.java-text-layout` no longer requires the Java container layout.

### Live measurements

An independent fixture was copied read-only from a second server (SkyBlock):
CraftEngine named-font ranks and icons whose code points (`U+4E00`..) collide with
a default-font shift character of the first fixture on purpose, and a
CustomNameplates background set with its off-screen spacing characters. Images
were located in both captures by masked template matching against the authored
art (Java) and the glyph cell actually served to Bedrock. Positions are in GUI
units at two screen pixels per unit; deltas are Bedrock minus Java.

| Scenario | Content | Result |
| --- | --- | --- |
| Action bar | 3 × ItemsAdder `:offset_-8:`, rank, icon | 0, 0 |
| Action bar with text | `Coins: 25 ` + icon + ` left` | 0 |
| Nameplate bar | CustomNameplates left, middle, right (39 units, odd) | 0, 0, 0 |
| Boss bar | rank, ` Boss `, icon | 0, 0 |
| Boss bar, odd width | rank, ` Boss! `, icon | 0, 0 |
| Chat | rank, ` Steve: merhaba `, icon (relative) | 0, 0 |
| Sidebar | rank in the objective title, icon in a score name | both shown |
| Title and subtitle | rank, icon | both shown |

With `ui.java-text-surfaces: false` the same scenarios showed the raw characters
on Bedrock in every surface. The first measurement of the nameplate bar was half a
unit left of Java, which established Java's integer centring; the correction
above removed it. Chest menus were re-measured with the release build: the six
real Survival menus still match Java (title band and window shift 0, 0) and
hopper, furnace and dispenser panels are identical to the vanilla Bedrock UI.

Measurements: [measurements](images/acceptance/2026-10-04-text-surfaces/measurements.json)
([hashes](images/acceptance/2026-10-04-text-surfaces/sha256.json)). The captures
show third-party rank and nameplate art and are therefore not published.

## Production builds of five real servers

A new audit runs the complete build (items, fonts with the layout, sounds, UI)
for each server's discovered sources, as the plugin would on that server. The
same five servers were built with the pre.5 release and with this release:

| Server | Custom items, pre.5 | Custom items, pre.6 | Problems pre.5 → pre.6 |
| --- | --- | --- | --- |
| Survival | 125 / 130 | 837 / 841 | 152 → 75 |
| SkyBlock | 0 / 0 | 29 / 52 (21 skin heads left native) | 44 → 43 |
| Box PvP v2 | 772 / 781 | 805 / 811 | 11 → 1 |
| Box PvP (legacy) | 532 / 843 | 539 / 845 | 313 → 301 |
| SMP Lifesteal | no custom content | no custom content | 0 → 0 |

The remaining Survival and SkyBlock problems are 113 vanilla sound events the
packs replace while `vanilla-override` is disabled (a configuration choice), four
item references that no pack provides, and one 10,000-unit full-screen overlay
image that Bedrock's font atlas cannot hold. The legacy Box PvP server lists items
from an older ItemsAdder configuration whose models no longer exist.

Defects found and fixed:

- **Vanilla asset copies shadowed server packs.** ItemsAdder keeps copies of the
  vanilla client assets under `storage/cache/vanilla_assets/<version>`. They were
  discovered as packs above the generated pack, so vanilla item definitions hid
  the custom ones (Survival: 711 custom items missing on Bedrock) and vanilla
  font definitions were merged as custom fonts. Such folders, temporary build
  folders and the stale self-hosted `pack.zip` inside ItemsAdder's working pack
  are no longer sources; a renamed ItemsAdder output (for example
  `generated_1.21.x.zip`) is used when `generated.zip` is absent.
- **Atlas sprite renames.** ItemsAdder's generated packs reference sprites such as
  `ia:2015`, which an atlas definition maps to the real texture. Twilight now
  resolves `single` and `directory` atlas sources like Java.
- **Protected packs.** Pack protection corrupts PNG chunk checksums and the zlib
  checksum, which Java ignores. Textures are now decoded leniently when the strict
  decoder fails (all colour types, bit depths, palettes, transparency, Adam7).
- **Java's own font sheets.** Fonts such as CustomNameplates' vertically shifted
  text reuse `ascii.png` and the unicode pages. Each character was copied as an
  untinted image alias, overflowing Bedrock's private-use pages (more than 50,000
  requests) and failing strict builds. Such characters are now ordinary text.
- **Off-screen and transparent spacing images.** Bitmaps placed thousands of units
  away or nearly transparent are spacing tricks; they become advances instead of
  baseline or oversized-image problems.
- **Model details.** Object-form texture entries (`{"sprite": ...}`) are read,
  custom selectors may show plain vanilla models (a barrier as a menu button), and
  faces whose texture variable no model defines use Java's missing texture.
  Player-skin heads, drawn by Java's special renderer, are left to Geyser's native
  rendering and noted.
- **Content Java rejects as well.** Malformed font files, unreadable TrueType
  fonts and sound files that exist in no pack are reported as notices in
  `build-report.json`. Bedrock then matches what Java players get, so they no
  longer stop a strict build; real conversion problems still do.

Measured counts per category are part of the published measurements; server
paths and content names are omitted.

## Remaining limits

- Item names and lore are not rewritten. Geyser hashes item data for inventory
  clicks and sends complete items back in creative mode, so changing them could
  alter the Java item; named-font images in lore therefore still show their raw
  characters on Bedrock.
- A glyph directly after ordinary text without a space is one unit right of Java;
  Bedrock cannot move the pen back. The same applies to overlapping layers such
  as text drawn over a nameplate background with a negative shift.
- Bitmap glyphs drawn from high-resolution textures are reduced to GUI units on
  Bedrock (Java samples them at screen resolution), so small lettering inside rank
  images is less sharp.
- Bold text and Bedrock's touch layout were not measured live on the new surfaces.

## Build and environment

- 138 tests in 25 suites passed locally (Java 25), including Java/Bedrock rendering
  models for left-aligned and centred lines, real protocol packets for every
  surface, lenient PNG decoding against an independent encoder, atlas renames and
  the discovery rules above.
- Java 26.2 client and Paper 26.2 server, Bedrock 1.26.5203.0, Geyser 2.11.3 build
  1247, both clients at two screen pixels per GUI unit.
- Tested fixture pack SHA-256 `651afb1ceb18e0c589da8e0425eb12e307a33defe9b7734b714b14d4cb007936`
  (1,177 entries); a repeated build was byte-identical, and a strict build with an
  injected defect was rejected while the deployed pack stayed unchanged.
- Test players stayed alive (health 20, death counters 0). The Bedrock content log
  showed no UI errors.
