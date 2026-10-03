# Wide bitmap glyphs and UI images - 2 October 2026

Twilight now enlarges a Unicode atlas cell when a Java bitmap needs more space.
It preserves the authored display dimensions instead of shrinking a wide rank
or menu image into a 16-pixel cell. The formerly omitted 41-by-9 rank and six
additional UI/HUD images render in the tested Bedrock chat. **Complete menu and
pixel parity is still not achieved.** A real inventory test exposes native title
clipping, and fractional scaling and horizontal padding still differ.

## Automatic conversion

Each glyph selects the smallest power-of-two cell from 16 through 512 pixels
that contains its width and visible vertical bounds. A page uses the largest
required cell among its glyphs. Each glyph retains its own Java display height
and ascent; a larger neighbor does not resize or vertically displace it.

Independent native calibration used pages from 256 through 8192 pixels wide.
In the tested Bedrock client, one texture pixel remains one GUI unit. The cell
origin relative to ordinary text is `4 - cellSize / 2`; placing the bitmap at
`cellSize / 2 + 3 - ascent` therefore reproduces Java's `7 - ascent` top offset.
Six equal 9-by-9 probes on different page sizes confirmed this relationship.
The [native calibration capture](images/acceptance/2026-10-02-wide-fonts/native-calibration-bedrock.png)
is diagnostic evidence, separate from the compiler-output screenshots below.
The diagnostic pages were removed before acceptance of actual compiler output.

Visible overflow beyond the supported bound remains a diagnostic and fails
strict compilation. Transparent padding cannot overwrite neighboring cells.
No source asset names, provider-specific exceptions or manual output corrections
are used by the implementation.

## Real-source coverage

The new fixture retains 36 real glyphs and six metric probes and adds six unused
Survival images with their original PNGs, code points, heights and ascents.
All 42 original PNG entries and isolated copies were verified unchanged.
Both clients used two screen pixels per GUI unit for these captures.

| Sample | Declared width x height | Ascent | Observed result |
| --- | --- | --- | --- |
| Player rank | 41 x 9 | 8 | All opaque pixels match both clients exactly; previously omitted. |
| Barrel menu image | 176 x 83 | 14 | Opaque pixels match both chat captures exactly. Native inventory title clips the image. |
| Fisherman portrait | 72 x 72 | -1 | Original 36 x 36 image uses the authored 2x enlargement; opaque pixels match both clients exactly. |
| Mana HUD image | 20.83 x 50 | -15 | Appears below the text as authored; Java fractional sampling differs from the integer Bedrock bitmap. |
| Lands menu image | 236 x 245 | 81 | Opaque pixels match; Bedrock removes 12 GUI units of transparent left padding. |
| Leaderboard image | 256 x 256 | 48 | Opaque pixels match both chat captures exactly, including the complete visible lower inventory area. |
| Exit banner | 198.36 x 74 | 10 | Image transfers, but fractional sampling differs. Not a pixel-parity pass. |

The five exact comparisons above concern opaque pixels after alignment, not
whole-screen equality, text advance, transparent margins or menu behavior.
For the fractional exit banner, the atlas also differs at 219 pixels from the
independent Pillow nearest-neighbor reference because sampling conventions
differ. This measurement is retained as a failure, not rounded into a pass.
The [measurements](images/acceptance/2026-10-02-wide-fonts/measurements.json)
include coordinates, display sizes and masked mean channel errors. Java's chat
culls offscreen line anchors even when a tall glyph could extend into view;
the capture helper keeps anchors visible without changing the source metrics.

| Compilation audit | Sources | Item mappings | Glyphs | Pages |
| --- | --- | --- | --- | --- |
| Box content | 30 | 772 / 781 | 622, previously 573 | 4 |
| Survival content | 138 | 125 / 130 | 751, previously 598 | 6 |
| Live mixed fixture | 4 | 167 / 168 | 48 / 48 | 5 |

All three repeated packs were byte-identical. Strict builds rejected remaining
problems and preserved the last diagnostic pack. The mixed fixture has one
known item with a missing face texture and no font omissions. Whole-source
audits still report malformed/missing assets, contextual font remapping,
unsupported spacing and extreme dimensions/baselines. Counts represent compiler
coverage, not hundreds of individually verified visual passes.

## Reviewed client evidence

| Test | Java | Bedrock |
| --- | --- | --- |
| Six metric probes | [Reference](images/acceptance/2026-10-02-wide-fonts/probes-java.png) | [Converted](images/acceptance/2026-10-02-wide-fonts/probes-bedrock.png) |
| 36 real glyphs, including wide rank | [Reference](images/acceptance/2026-10-02-wide-fonts/real-java.png) | [All visible](images/acceptance/2026-10-02-wide-fonts/real-bedrock.png) |
| Barrel image in chat | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-1-java.png) | [Converted](images/acceptance/2026-10-02-wide-fonts/wide-1-bedrock.png) |
| Portrait | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-2-java.png) | [Converted](images/acceptance/2026-10-02-wide-fonts/wide-2-bedrock.png) |
| Negative-ascent HUD image | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-3-java.png) | [Sampling difference](images/acceptance/2026-10-02-wide-fonts/wide-3-bedrock.png) |
| Lands image | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-4-java.png) | [Left padding differs](images/acceptance/2026-10-02-wide-fonts/wide-4-bedrock.png) |
| Leaderboard image | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-5-java.png) | [Converted](images/acceptance/2026-10-02-wide-fonts/wide-5-bedrock.png) |
| Exit banner | [Reference](images/acceptance/2026-10-02-wide-fonts/wide-6-java.png) | [Sampling difference](images/acceptance/2026-10-02-wide-fonts/wide-6-bedrock.png) |
| Actual 36-item inventory with barrel title | [Darkened image overlays slots](images/acceptance/2026-10-02-wide-fonts/menu-java.png) | [Title image clipped by native label](images/acceptance/2026-10-02-wide-fonts/menu-bedrock.png) |

The inventory pair is a **recorded failure**, not successful custom-menu
adaptation. The glyph works in chat but exceeds the native title label's space.
It needs a runtime/UI adapter that also accounts for Java spacing and tint.
The existing two invalid Java item references remain visible in this inventory.

Captures are unchanged client screenshots. [SHA-256 hashes](images/acceptance/2026-10-02-wide-fonts/sha256.json)
identify the evidence. Original third-party packs, server data and test helpers
are not distributed. Test players remained creative; both had health 20 at the
final save and all ten saved player death counters remained zero.

## Build and remaining limits

The locally built `1.0.0-pre.4-SNAPSHOT` JAR passed 87 tests in 19 suites.
Three new regressions cover enlarged ascenders/descenders, every pixel of a
wide label plus its small neighbor, and a large menu bitmap. Existing model,
animation, mapping and deployment tests remain in the full build. This round
does not claim a fresh live pose/animation matrix for every provider.

- JAR SHA-256: `37a4195d2d4e36c23d7afe61d8c46741879601927fd76ff8358b9c427f6d4e24`
- Tested compiler pack SHA-256: `28b132858cdb19a45379bc2a039001af6a4a1325c00aee10dbeb5ac2b6e73ed3`
- Java 26.2, Bedrock 1.26.5203.0, Geyser 2.11.3 build 1247; local build only.

An 8192-square RGBA atlas occupies 256 MiB before compression. This fixture's
five pages total 384.5 MiB of raw pixels; peak compiler/client memory can be
higher. Acceptance on this Windows client does not certify low-memory devices.

Preserving authored dimensions does not preserve every high-resolution source
texel: native font pixels still use integer GUI units. Fractional resampling,
horizontal bearings/advances, title clipping and tint, contextual named fonts,
dynamic HUD placement and full interactive menu layout remain open. First-person
model poses and complete animated-texture playback also remain open. A successful
compiler build is not a guarantee of fidelity in every usage context.

The native glyph-page structure was checked against Microsoft's
[resource-pack contents reference](https://learn.microsoft.com/en-us/minecraft/creator/documents/comprehensivepackcontents?view=minecraft-bedrock-stable)
and [NhanAZ/glyph](https://github.com/NhanAZ/glyph) (GPL-3.0).
No external code was imported. The placement formula and runtime limits above
come from Twilight's own client measurements.
