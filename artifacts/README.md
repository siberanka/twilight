# Twilight 1.0.0-pre.9 prerelease build

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256) (backend servers)

[Download TwilightProxy.jar](TwilightProxy.jar?raw=true) | [SHA-256](TwilightProxy.jar.sha256) (Velocity and BungeeCord proxies, optional)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build :twilight-proxy:build --offline --no-daemon --no-configuration-cache`.
191 tests across 36 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, `twilight-proxy/` and `protocol/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md), the [wiki](../WIKI.md) and the
[stacked images, nameplates and exact biomes](../docs/LAYERS_BIOMES_2026-10-04.md)
report before deployment. This prerelease adds twilight-proxy, per-server Bedrock packs for
networks with Geyser on a Velocity or BungeeCord proxy
([proxy test](../docs/PROXY_2026-10-05.md)). It draws text and images that Java moves back
over earlier ones (stacked menu art, CustomNameplates backgrounds) with one Bedrock
label per layer, shows custom and RealisticSeasons biomes in their exact colours,
applies Java's name rules to nameplates, lets each provider's source and the Bedrock
pack delivery be chosen in `config.yml`, and exports every build to
`plugins/Twilight/export`. Complete builds of seven production servers pass strict
publication with the default configuration.

It builds on the [UI campaign](../docs/UI_CAMPAIGN_2026-10-04.md) (104 real menus),
the Java text layout on every Bedrock text surface
([text surface review](../docs/TEXT_SURFACES_2026-10-04.md)), the chest title
layout ([text layout review](../docs/TEXT_LAYOUT_2026-10-03.md)) and the container
layout ([container layout review](../docs/CONTAINER_LAYOUT_2026-10-03.md)), and
includes the live display bridge, [cloud-anchor corrections](../docs/CLOUD_ANCHORS_2026-09-29.md)
and [Java-compatible cloud mount height](../docs/DISPLAY_SEATS_2026-09-29.md).

Full animation, first-person, UI and entity parity remain incomplete. Item names
and lore keep raw font characters, text displays riding mobs sit about 0.4 blocks
lower, hex text uses Bedrock's 28 colours, a glyph directly after text without a
space differs by a unit, and high-resolution glyph sampling differs.

The [extended content checks](../docs/BROAD_CONTENT_2026-09-29.md), the
[new-sample regression](../docs/COMPOSITE_MODELS_2026-09-30.md) and the
[font metrics regression](../docs/FONT_METRICS_2026-10-01.md) document earlier
item, glyph and menu results.
