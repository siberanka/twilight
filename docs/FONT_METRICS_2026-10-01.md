# Bitmap glyph heights and baselines — 1 October 2026

Follow-up: the [2 October wide glyph review](WIDE_GLYPHS_2026-10-02.md) extends
this measured placement to larger atlas cells. The results below retain the
original prerelease scope and evidence.

The converter read Java's `ascent` but did not use it when placing glyphs.
All images were aligned to the bottom of a 16-pixel cell. An ordinary
height-9, ascent-8 emoji consequently appeared four GUI units too low in
the tested Bedrock client.

Twilight now places each glyph at `11 - ascent` within its cell and scales
its source bitmap to the declared `height`. This is independent of source
resolution, neighboring glyphs and page membership. It also supports negative
ascents when the visible image fits. Visible overflow is reported rather than
silently cropped or reduced. Transparent padding can extend outside a cell;
clipping prevents it from erasing a neighboring character.

## Measurement and regression coverage

Java's bitmap top is `7 - ascent` relative to the text origin. A separate
native-page calibration in Bedrock 1.26.5203.0 measured the cell top four GUI
units above ordinary `H` text, with one texture pixel per GUI unit. Combining
these measurements gives the cell placement above. The calibration pack was
diagnostic only; acceptance uses pages produced by the actual compiler.

The final candidate pack was generated in full by the compiler and loaded on a
fresh isolated server. Its font pages match the isolated font regression output
byte for byte. Final Java/Bedrock captures were reviewed on 1 October after the
foreground blocker cleared. Six probes have identical declared heights and
text-relative vertical positions after normalizing the clients' GUI scales:

| Height / ascent | Java top offset | Bedrock top offset |
| --- | --- | --- |
| 4 / 4 | 3 | 3 |
| 4 / 2 | 5 | 5 |
| 4 / 0 | 7 | 7 |
| 2 / -2 | 9 | 9 |
| 8 / 8 | -1 | -1 |
| 9 / 8 | -1 | -1 |

Offsets are GUI units relative to the adjacent `H`. Java uses three screen
pixels per GUI unit in these captures; Bedrock uses two. The 35 supported
real-source glyphs are visible without lower-line clipping. The wide rank is
still omitted with a diagnostic. The tested menu title retains the square and
emoji height/alignment, but **title color modulation differs**: Java darkens
the bitmap with its text color while Bedrock retains the original glyph colors.
This is a passed baseline/height check, not complete glyph or UI parity.

Four new automated tests cover measured positions, representable negative
ascents, different source resolutions, page independence, visible overflow,
and transparent padding beside occupied cells. All 84 tests in 19 suites passed
locally on Java 25. No hosted CI was used.

The new real-source selection contains 24 previously unused emoji and 12
additional item/rank glyphs. Source images and Java provider metrics were copied
unchanged. A 41-unit-wide rank label still requires a layout adapter and is
reported as unsupported. It is not a conversion success.

Whole-source audits also ran against the BoxPVP and Survival test material:
772/781 and 125/130 item mappings respectively. These counts are compiler
coverage, not individual visual passes. Repeated packs were byte-identical;
strict failures preserved the previous diagnostic outputs.

## Reviewed evidence

| Evidence | Result |
| --- | --- |
| [Native Bedrock calibration](images/acceptance/2026-10-01-fonts/native-probe-bedrock.png) | Independent four-pixel squares at known cell offsets establish the text origin. This manually assembled probe is not plugin-output acceptance. |
| [Java real-source reference](images/acceptance/2026-10-01-fonts/before-real-java.png) and [Bedrock before correction](images/acceptance/2026-10-01-fonts/before-real-bedrock.png) | 36 new glyphs compared; baseline error is visible and the wide rank label is omitted by diagnostic conversion. |
| [Java metric probes](images/acceptance/2026-10-01-fonts/fixed-probe-java.png) | Six height/ascent combinations, including a negative ascent, form the unchanged Java reference. |
| [Java menu title and inventory](images/acceptance/2026-10-01-fonts/fixed-menu-java.png) | Title combines ordinary text, a metric square and a real emoji. The matching Bedrock capture below confirms placement; text-color modulation still differs. The two previously documented invalid Java item references remain. |
| [Bedrock metric probes](images/acceptance/2026-10-01-fonts/fixed-probe-bedrock.png) | All six measured heights and offsets match the Java reference, including negative ascent. |
| [Final Java real glyphs](images/acceptance/2026-10-01-fonts/fixed-real-java.png) and [final Bedrock real glyphs](images/acceptance/2026-10-01-fonts/fixed-real-bedrock.png) | 35 supported glyphs visible; one wide rank remains unsupported. Native text spacing and UI scale differ. |
| [Final Bedrock menu](images/acceptance/2026-10-01-fonts/fixed-menu-bedrock.png) | Title square/emoji align vertically; bitmap tint differs from Java. The existing missing-source inventory exceptions remain. |

[Measurements](images/acceptance/2026-10-01-fonts/measurements.json) distinguish
compiler checks from visual acceptance. [Image hashes](images/acceptance/2026-10-01-fonts/sha256.json)
cover unchanged captures. Third-party source packs and local automation stay
outside the public repository.

## Related projects reviewed

- [smashyalts/java2bedrockclient](https://github.com/smashyalts/java2bedrockclient/tree/8ef56991acf1e97f13c883ac476bed0c44ed44fd)
  (GPL-3.0) exposes bitmap-font conversion and reports approximation. Its
  [font stage](https://github.com/smashyalts/java2bedrockclient/blob/8ef56991acf1e97f13c883ac476bed0c44ed44fd/packages/core/src/convert/stages/fontsStage.ts)
  uses page-relative height/ascent heuristics. Twilight uses measured fixed
  metrics so adding another glyph cannot move existing ones.
- [AZPixel-Team/Java2Bedrock](https://github.com/AZPixel-Team/Java2Bedrock/tree/ffe3f128db5ef251b267bd5ae7f9ff7f4315fd3e)
  (AGPL-3.0) contains glyph-sheet utilities; its README marks the project
  unmaintained. It was reviewed as prior work, not adopted as a dependency.
- [GeyserMC/PackConverter](https://github.com/GeyserMC/PackConverter/tree/48cb61a874a9e0f6a064eec75da2724caccc49f0)
  (MIT) is a related resource-pack converter and explicitly distinguishes its
  texture conversion from full custom-item mapping.

No source code from these projects was copied. Twilight's implementation and
tests remain under the repository's existing license; existing upstream author
and license notices are retained.

## Limits

Declared Java display dimensions are preserved; oversized content is never
shrunk to fit a cell. High-resolution source textures are sampled at their
authored Java display height, not fitted to a smaller fallback size. Native
bitmap pages still cannot preserve every source texel or contextual text tint.

This correction does not claim identical chat line spacing, antialiasing or
client UI scale. Wide rank labels, oversized GUI backgrounds, custom advances
and contextual font remapping still need a layout adapter. First-person model
poses and complete animated-texture playback also remain open; this is a
prerelease, not an assertion of universal Java/Bedrock parity.
