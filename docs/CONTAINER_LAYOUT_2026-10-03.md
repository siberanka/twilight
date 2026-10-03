# Java container layout on Bedrock chest screens - 3 October 2026

Font-image menus are drawn by Java as part of the container title. Bedrock's
native chest title wrapped such an image at 90% of the panel width,
hyphenated it and clipped it, and its slot rows did not keep Java's spacing.
Twilight 1.0.0-pre.4 now generates a Java-compatible desktop chest layout
automatically. A real 176 x 83 menu image from the Survival test content now
appears in full and lines up with its slots in every chest row count from 1 to 6.

This report covers the **desktop (classic) chest family**: chests, large chests,
ender chests, shulker boxes and barrels, as Geyser presents generic 9 x 1 to
9 x 6 menus. It does not claim parity for touch layouts, other container types,
menu actions or tooltips.

## Automatic conversion

`ui.java-container-layout` (default `true`) adds two partial Bedrock UI
definitions to the generated pack. Bedrock merges them into its vanilla
`chest` and `common` namespaces; vanilla elements are not copied or replaced.

| Java behavior | Bedrock adaptation |
| --- | --- |
| The title is drawn unwrapped and unclipped at (8, 6). | The chest title label uses its text size and Java's position relative to the first slot frame. |
| The title is drawn after slot frames and before items. | The title layer is above slot frames and below selection, hover and item renderers. |
| Default title text colour is `0x404040`. | The label's default colour is `0x404040`; explicit colour codes still apply. |
| The player inventory frame starts 13 units below the last chest row; the hotbar starts 4 units below the inventory. | The top half is raised by 2 units and chest screens set a hotbar offset of 4 units through a variable. |
| The `Inventory` label is one unit right of and ten above the player inventory, drawn after the title. | Chest screens set its offset and a layer above the title through variables. |

The shared hotbar template and inventory label read these values from
variables whose defaults equal vanilla Bedrock. Hopper, dispenser and furnace
screens were pixel-identical to the vanilla UI in the captured panel areas.
The inventory label is rebuilt with Bedrock's `modifications` mechanism,
because the vanilla label is an unnamed array entry with literal values.
Disable the option to restore the unmodified Bedrock chest UI.

## Measured results

Both clients ran at two screen pixels per GUI unit. Each empty menu used the
title `H<8 x 8 probe>H<41 x 9 rank>H`. Positions are GUI units relative to the
first chest slot. The scripted comparison measured title glyph runs and rows,
every slot row (chest, player inventory and hotbar), and the first glyph of the
inventory label.

| Chest slots | Vanilla Bedrock layout | Twilight layout |
| --- | --- | --- |
| 9, 18, 27 | Wide title wraps and clips; title text 1 unit left and 2 units lower; inventory 2 and hotbar 3 units higher; inventory label 1 unit left and 1 lower | Title rows, slot rows, hotbar and inventory label identical to Java |
| 36, 45, 54 | As above, except the title text is 1 unit lower | Title rows, slot rows, hotbar and inventory label identical to Java |

The remaining title difference occurs only for custom bitmap glyphs: Bedrock
draws each glyph one unit to the right of Java's position. Their rows match
Java, and so do ordinary text and the character after a glyph without
transparent left columns. The
[layout measurements](images/acceptance/2026-10-03-container-layout/layout-measurements.json)
and [vanilla baseline](images/acceptance/2026-10-03-container-layout/vanilla-measurements.json)
contain the complete per-size data.

### Bitmap glyph behavior measured in the title

- Bedrock trims transparent columns at the left edge of a glyph, draws the
  first opaque column one unit after the pen position and advances by the
  opaque width plus one. Java draws from the pen including transparent
  columns and advances by the width up to the rightmost opaque column plus one.
  A glyph without left transparency therefore appears one unit to the right,
  and left padding narrows the Bedrock advance. A contextual layout adapter is
  needed to correct this; it is not implemented.
- Bedrock does not tint private-use bitmap glyphs. Java multiplies them by the
  text colour, so an image in a default-colour title is darkened by `0x404040`
  on Java and unchanged on Bedrock. White (`&f`) titles match. A red `&c`
  probe was `(255, 0, 85)` on Java and `(255, 0, 255)` on Bedrock.

## Reviewed captures

All captures used the release build (`1.0.0-pre.4`), except the vanilla
baseline, which used the same build with the layout disabled.

| Case | Java | Bedrock |
| --- | --- | --- |
| White-title menu image, 27 slots | [Reference](images/acceptance/2026-10-03-container-layout/white-27-java.png) | [Full image, slots aligned](images/acceptance/2026-10-03-container-layout/white-27-bedrock.png) |
| White-title menu image, 54 slots | [Reference](images/acceptance/2026-10-03-container-layout/white-54-java.png) | [Full image, slots aligned](images/acceptance/2026-10-03-container-layout/white-54-bedrock.png) |
| Default-colour title | [Image darkened by `0x404040`](images/acceptance/2026-10-03-container-layout/default-tint-54-java.png) | [Image not tinted](images/acceptance/2026-10-03-container-layout/default-tint-54-bedrock.png) |
| Probe glyphs | [Reference](images/acceptance/2026-10-03-container-layout/glyph-bearing-27-java.png) | [One-unit glyph offset, no tint](images/acceptance/2026-10-03-container-layout/glyph-bearing-27-bedrock.png) |
| Vanilla Bedrock UI, 27 slots | - | [Image missing, title hyphenated](images/acceptance/2026-10-03-container-layout/before-white-27-bedrock.png) |

Crops are taken from unchanged client screenshots and identified by
[SHA-256 hashes](images/acceptance/2026-10-03-container-layout/sha256.json).
The Java client language was English and the Bedrock client language Turkish,
so the inventory labels differ in text. Two invalid Java item references remain
visible as missing-texture items in the Java menu.

## Build and environment

- 95 tests in 20 suites passed locally; four new tests cover title placement,
  layering, inventory and hotbar spacing, vanilla defaults and the option switch.
- Release JAR SHA-256: `13d45d42b078211266879d1f86ce57b9e0c6961d2ef8be5fe5b71ac4787d3b16`.
- Tested pack SHA-256: `1723b9024707aed15493a75098e317f3e102b2ade780ed6af570cfe445d504b9`
  (1,165 entries). Two repeated builds were byte-identical, and a rejected
  strict build left the last pack unchanged.
- Java 26.2, Paper 26.2 build 121, Geyser 2.11.3 build 1247 and Bedrock
  1.26.5203.0 on Windows. Local build only; no hosted CI.
- The Bedrock content log reported no UI errors. Test players remained creative
  with health 20, and all saved death counters remained zero.

## Remaining limits

- Contextual bitmap tint, the one-unit glyph offset, Java space-provider
  advances (including negative spaces) and multi-layer title composition need a
  runtime text/layout adapter.
- Touch (pocket) layouts, controller focus and other container screens such as
  hoppers, dispensers, furnaces and anvils keep the vanilla Bedrock layout.
- Bedrock's panel frame and close button remain native. Java's image is drawn
  above them where it overlaps.
- Menu interactions, tooltips and live HUD state are outside this change.
