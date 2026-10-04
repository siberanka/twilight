# Stacked images, nameplates and exact biomes - 4 October 2026

Twilight 1.0.0-pre.8 closes the gaps left by the [UI campaign](UI_CAMPAIGN_2026-10-04.md):
images and text that Java draws back over earlier images, nameplate plugins, custom
biome colours and seasonal biomes. Every capture below is original art or a vanilla
scene, taken on the isolated test server (Paper 26.2, Geyser 2.11.3, Java 26.2 and
Bedrock 1.26.5203.0 clients at GUI scale 2). Third-party content (CustomNameplates,
RealisticSeasons, server menus) is reported as measurements only.

## Stacked images in menus

Java moves the pen back with negative spaces and draws later images over earlier
ones. A Bedrock label only moves forward, so Twilight splits such a title into up to
four layers, one label each, at runtime for every packet: each layer keeps Java's
positions, the lower layers are drawn first and the top layer last. Text shifted up
or down by a font (CustomNameplates' shift fonts, ascents) gets one-unit line steps;
text Java draws without a shadow goes to its own layer. Nothing is precomputed per
title, so every plugin and every placeholder value works.

| Style | Technique | Window / title offset | Art difference |
| --- | --- | --- | --- |
| Stacked four | panel, banner over it, badge over the banner, bold text over the badge | 0, 0 / 0, 0 | 0.3 |
| Highlight row | translucent bar drawn over the first slot row, then icons and text moved back over the header | 0, 0 / 0, 0 | 0.2 |
| Layered | banner moved back over the header with a `-121` space, then bold text | 0, 0 / 0, 0 | 0.2 |
| Header, no colour | header after an `-8` space, title without a colour (Java darkens it) | 0, 0 / 0, 0 | 0.2 |
| Header, white | the same with `&f` | 0, 0 / 0, 0 | 0.2 |
| Negative-height shifts | eight Jobs-style negative-height bitmaps, then the header | 0, 0 / 0, 0 | 0.4 |
| Icon row, hex text, bold text | icons with `+4`/`-1` spaces, `&#FFAA00`, `&b&l` | 0, 0 / 0, 0 | see note |

Offsets are in GUI units: `0, 0` means the art lands on Java's pixels. The art
difference is the mean absolute luminance difference inside the title panel (0-255).
Titles without a panel image show the world behind the menu there, which each client
dims differently (about 30); their text and icons are at the same positions.

<table>
  <tr><td><img src="images/acceptance/2026-10-04-layers/menus/stacked-four.gif" alt="Animation alternating the Java and Bedrock captures of the four-layer menu; nothing moves" width="100%"></td>
      <td><img src="images/acceptance/2026-10-04-layers/menus/highlight-row.gif" alt="Animation alternating Java and Bedrock: translucent highlight over the slots" width="100%"></td></tr>
</table>

The animations alternate the Java and the Bedrock capture of the same menu every
1.1 seconds; only the label changes. Side-by-side pairs:
[stacked four](images/acceptance/2026-10-04-layers/menus/stacked-four.png),
[highlight row](images/acceptance/2026-10-04-layers/menus/highlight-row.png),
[layered](images/acceptance/2026-10-04-layers/menus/layered-bold.png)
([animation](images/acceptance/2026-10-04-layers/menus/layered-bold.gif)),
[header without colour](images/acceptance/2026-10-04-layers/menus/header-uncoloured.png),
[white header](images/acceptance/2026-10-04-layers/menus/header-white.png),
[icon row](images/acceptance/2026-10-04-layers/menus/icon-row.png),
[negative-height shifts](images/acceptance/2026-10-04-layers/menus/negative-height.png),
[hex text](images/acceptance/2026-10-04-layers/menus/hex-text.png),
[bold text](images/acceptance/2026-10-04-layers/menus/bold-text.png);
[measurements](images/acceptance/2026-10-04-layers/measurements.json),
[hashes](images/acceptance/2026-10-04-layers/sha256.json).

Bedrock spaces label lines one unit apart for the vertical steps and centres each
line in that unit, which raised top-anchored labels (chest titles, boss bar names) by
4.5 units; the label offsets compensate it. A run before the fix measured the title
4 units too high; the runs above are exact.

## Boss bars and the action bar

The same layers serve boss bar names (CustomNameplates backgrounds) and the action
bar. Boss bars whose sprites the Java pack makes transparent stay hidden.

<img src="images/acceptance/2026-10-04-layers/hud.png" alt="Java and Bedrock: gold text over coin icons on the boss bar and bold text over icons on the action bar" width="100%">

The boss bar art occupies screen rows 6-19 on both clients; the action bar text is
72 units above the bottom edge on both.

## Nameplates

Nameplate plugins mount text displays on the player. Java never draws the name of a
living entity that carries passengers (armor stands follow their own rule), so only
the plugin's plate is visible; Bedrock drew the plain name as well. Twilight now
applies Java's rule to the Bedrock name of ridden players and mobs and restores it
when the last passenger leaves. Text displays with a transparent background were
drawn on Bedrock's dark name tag box; `ui.nametag-background` hides that box
(`auto`: when CustomNameplates' name tags are enabled).

<img src="images/acceptance/2026-10-04-layers/nameplate.png" alt="A villager carrying a text display with a badge image and gold text, on Java and Bedrock; the villager's own name is hidden on both" width="100%">

With the real CustomNameplates 3.0.42 and its Survival configuration (measurements
only), Bedrock now shows the plate without the second plain name and without the two
dark boxes behind the image and the text part; the boss bar backgrounds, icons and
shifted text matched Java in an earlier run of the same session.

## Custom biomes

Bedrock refuses biome definitions it does not know, but it takes the look of a
vanilla biome from the resource pack. Twenty-five Bedrock biomes exist only for old
worlds (legacy hills, edges and mutated variants) and no Java biome maps to them. The
pack redefines them with the grass, foliage, water, underwater fog, fog and sky colours
of the server's custom biomes, and Geyser sends their climate (temperature, downfall,
rain or snow). Custom biomes come from the datapacks and from the server registry, so
plugin biomes (RealisticSeasons) are included. With more custom looks than slots, the
looks of the current season go first, then the ones the vanilla biomes approximate
worst; every other custom biome shows as the closest vanilla biome or slot.

| Floor of a datapack biome (mean RGB) | Java | Bedrock |
| --- | --- | --- |
| 1.0.0-pre.7 (closest vanilla biome) | 112, 129, 65 | 102, 123, 54 |
| 1.0.0-pre.8 (exact slot) | 111, 128, 64 | 110, 127, 64 |

<table>
  <tr><td width="60%"><img src="images/acceptance/2026-10-04-layers/biome.png" alt="Grass of a custom datapack biome on Java and Bedrock in the same colour" width="100%"></td>
      <td><img src="images/acceptance/2026-10-04-layers/biome-live.gif" alt="Animation of a 5 by 5 grid of custom biomes changing colours on Bedrock after each fillbiome" width="100%"></td></tr>
</table>

Java changes biomes in place (`/fillbiome`, seasons plugins) with a packet Geyser
does not translate, so Bedrock kept the old biomes until the chunk reloaded. Twilight
now sends such chunks to the Bedrock player again through the normal chunk path; the
animation shows a grid of 25 custom biomes changing several times without a rejoin.
All 25 slots show their own grass and leaf colours ([grid](images/acceptance/2026-10-04-layers/slots.png));
at the borders Bedrock blends neighbouring biomes over a wider radius than Java.

RealisticSeasons 11.12.1 with the Survival configuration (measurements only)
registered 188 seasonal biomes. Twilight read them from the registry, asked
RealisticSeasons' API which ones the current season shows and gave those slots first;
the season change event rebuilds the pack. In winter, a forest floor measured 104,
118, 127 on Java and 104, 118, 128 on Bedrock (98, 115, 111 before the season was
taken into account). RealisticSeasons' own data and packets are not changed.

## Automatic builds on real servers

`ServerBuildAuditMain` ran complete builds of seven production servers read-only with
the default configuration. Content that Java itself tolerates is now converted the way
Java shows it and reported as a notice: textures that exist in no pack become Java's
missing texture, items whose model exists nowhere keep their base item, changed
vanilla sounds keep Bedrock's sound unless `vanilla-override` is enabled, and
screen-sized overlay glyphs are left out while text after them keeps its position.

| Server | Custom items | Glyphs | Problems | Notices |
| --- | --- | --- | --- | --- |
| Survival | 1,915 / 1,932 | 767 | 0 | 175 |
| SkyBlock | 31 / 52 | 31 | 0 | 64 |
| Boxpvp v2 | 884 / 902 | 622 | 0 | 18 |
| BoxPVP | 613 / 928 | 618 | 0 | 319 |
| Kaynak | 2,713 / 3,014 | 762 | 0 | 375 |
| SMP-Lifesteal, Auth | no custom content | 0 | 0 | 0 |

Every build passes strict publication. When a server's first strict build still finds
a problem and no pack is deployed yet, Twilight deploys a first pack without that
content instead of none; later builds stay strict.

## Research

- The [no nametag background](https://www.curseforge.com/minecraft-bedrock/texture-packs/no-nametag-background)
  pack by chromehaert showed that Bedrock's name tag box is drawn with the
  `name_tag` material, whose blending a pack can change. Twilight generates its own
  material file with the same idea; no file of that pack is distributed.
- [Mojang's bedrock-samples](https://github.com/Mojang/bedrock-samples) and the
  [client biome reference](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/clientbiomesreference/examples/components/client_biome_components)
  document the client biome and fog formats Twilight writes.
- [Chest GUI Generator](https://github.com/wlsgunzz/Chest-GUI-Generator) builds static
  chest overlays per title from a pack. Twilight lays titles out per packet instead,
  so titles with placeholders and stacked layers need no per-title setup.
- The name rules come from the Java 26.2 client's renderers: living entities hide
  their name while carrying passengers, armor stands keep it.

## Packet handling

The changes are made where Geyser translates Java packets for each Bedrock player
(Geyser's own translator registry), not with a separate packet library: Twilight sees
the exact text, entities and chunks Geyser is about to translate, changes nothing for
Java players and needs no extra dependency. This requires Geyser on the same server
as Twilight (Geyser-Spigot); a proxy-only Geyser receives the pack but not these
runtime layers.

## Remaining differences

- A text display riding a mob is drawn about 0.4 blocks lower on Bedrock (Geyser's
  mount offset); on players the plate sits where Java draws it.
- Bedrock's name tag box is hidden for every name tag or none; Java decides per text
  display.
- Hex colours in text are shown in the nearest of Bedrock's 28 text colours.
- Bedrock blends biome colours over a wider radius at biome borders.
- More custom looks than 25 slots share the closest slot or vanilla biome.
