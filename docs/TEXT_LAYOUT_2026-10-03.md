# Java title layout for Bedrock chest screens - 3 October 2026

> Later change (1.0.0-pre.6): the layout now also covers chat, the action bar,
> titles, boss bars, scoreboards, entity names and other container titles, and no
> longer requires the container layout. See the
> [text surface review](TEXT_SURFACES_2026-10-04.md).

Twilight 1.0.0-pre.5 lays out container titles for Bedrock players with Java font
metrics. Real Java menu titles combine negative-space characters with large GUI
images. On Bedrock these titles previously failed in three ways: characters that
Java remaps in `minecraft:default` (for example `七` or `ἃ`) showed Bedrock's own
letters, negative and custom spacing was lost, and every bitmap glyph was drawn
one GUI unit right of Java. Six real Survival menus now land on the same GUI
pixels as on Java in the title area, and on all measured slot rows.

## Measured Bedrock glyph rules

Diagnostic glyph pages were measured in the Bedrock 1.26.5203 chest title:

- A glyph is drawn from its first column with alpha above zero, placed one unit
  after the pen. Its advance is its inked width plus one. Java draws column zero
  at the pen and advances by its width up to the rightmost inked column, plus one.
- Pixels with alpha 1 count as ink while remaining invisible. A fully transparent
  glyph advances four units.
- Vertical placement relative to text is identical to Java for atlas cells from
  16 to 512 pixels.
- Bedrock cannot advance by exactly one unit or move left within a line.

## Automatic conversion

`ui.java-text-layout` (default `true`, requires `ui.java-container-layout`) adds:

| Part | Behaviour |
| --- | --- |
| Java font model | Font definitions from every resource pack are combined like Java (higher-priority packs first). Bitmap advances use Java's formula, including negative heights (`(int)(0.5 + width × height / cellHeight) + 1`); space providers and empty TrueType glyphs become advances. TrueType files Java cannot load (no `maxp` table) are ignored, as on Java. |
| Private-use aliases | Characters Java remaps in the default font or a named font, and colliding or supplementary named-font glyphs, receive private-use code points on unused pages, so Bedrock's own glyphs stay intact. |
| Spacers | An invisible page of alpha-1 glyphs provides advances of 2 to 33 units. Small glyphs receive a variant with one invisible trailing column to absorb one-unit gaps. |
| Layout table | `twilight/text-layout.json` stores each character's Java advance, Bedrock code point and inked columns. |
| Chest UI | The chest title label starts 256 units left of Java's title origin. |
| Runtime | A Geyser translator wrapper rewrites chest titles for Bedrock players with leading spacers, Java-exact gaps and aliases. Styles, colours and the component tree are preserved; translated components are preceded by spacers. It follows Geyser start-up and reloads. |

Bedrock's touch (pocket) layout keeps its centred native title. There, glyphs are
substituted without positioning and Java-only spacing characters are removed.
Titles of hoppers, dispensers, furnaces and other containers are not changed.

## Real-menu results

Menu titles from the Survival DeluxeMenus definitions were reproduced with their
original font providers and images in an isolated fixture. Both clients ran at
two screen pixels per GUI unit. Shifts are the best alignment of the Bedrock
capture to Java, in GUI units: title band only, and the whole menu window.

| Menu (slots) | Title mechanism | Title shift | Menu shift |
| --- | --- | --- | --- |
| Jobs farmer (54) | 8 × negative-height bitmap `七`, image remapped from `ἃ` | 0, 0 | 0, 0 |
| Rules (36) | ItemsAdder `:offset_-8:` space provider, image `Ⱘ` | 0, 0 | 0, 0 |
| Minion panel (54) | 8 × negative-height bitmap `ꯈ`, image `ꯄ` | 0, 0 | 0, 0 |
| Wallet (27) | 12 × negative-height bitmap `㈁`, 512-cell image `㈆` | 0, 0 | 0, 0 |
| Server info (54) | 8 × `ꯈ`, image `ᭅ` | 0, 0 | 0, 0 |
| Barrel (27) | private-use image `` | 0, 0 (pixel-identical band) | 0, 0 |

Before this layout, the same barrel menu was one unit to the right
([Bedrock before](images/acceptance/2026-10-03-text-layout/before-barrel-27-bedrock.png)).
Now: [Java](images/acceptance/2026-10-03-text-layout/barrel-27-java.png) and
[Bedrock](images/acceptance/2026-10-03-text-layout/barrel-27-bedrock.png).
The other five menus use third-party GUI art and are published as measurements
only ([measurements](images/acceptance/2026-10-03-text-layout/measurements.json),
[hashes](images/acceptance/2026-10-03-text-layout/sha256.json)).

The chest-size suite (9 to 54 slots) kept Java's title rows, slot rows, hotbar and
inventory label. Hopper, furnace and dispenser panels remained pixel-identical to
the vanilla Bedrock UI. The Bedrock content log reported no UI errors.

A live check also found that Java 26.2 does not load the ItemsAdder
`negative_spaces.ttf` file (no `maxp` table): its characters appear as fallback
glyphs on Java. Twilight now matches this instead of applying the font's advances.

## Build and environment

- 109 tests in 22 suites passed locally, including Java/Bedrock rendering models
  for spacing, glyph bearings, negative heights, TrueType advances, cross-pack font
  merging, aliasing, spacer invisibility and the reflective component rewrite.
- Release JAR SHA-256: `1824ec2b3f6ee5d91ad778479f47c98623dd4a9427a1258c9bc01c310fd5d78b`.
- Tested fixture pack SHA-256: `2075b2ac0280bb1d55308e9ca90a386fb79fd4ab1e49a91cce7bffafc9902b6c`
  (1,170 entries); two repeated builds were byte-identical, and a rejected strict
  build left the last pack unchanged.
- Java 26.2, Paper 26.2 build 121, Geyser 2.11.3 build 1247, Bedrock 1.26.5203.0
  on Windows; local build only, no hosted CI. Test players stayed alive (health 20,
  all saved death counters zero).

## Remaining limits

- A glyph directly after ordinary text (no space between) is still one unit right,
  because Bedrock cannot move the pen left. With a space between, it is exact.
- Overlapping layers (a title that moves backwards to draw text over an image) are
  not reproduced; such moves are counted as approximations.
- Ordinary characters are assumed to have the same widths on both clients;
  non-ASCII text before a glyph can shift it if the widths differ.
- Bitmap tint, bold glyphs, chat, item names, lore, scoreboards and boss bars are
  not laid out yet; aliased characters appear correctly only in chest titles.
- The touch layout was covered by unit tests, not by a live touch-client capture.
