# Twilight 1.0.0-beta.1 - first beta

> Türkçe: [aşağıda](#türkçe)

The first beta of Twilight and twilight-proxy. The features are complete for single servers and proxy
networks and run on production networks; behaviour can still change between betas. Read the
[beta status and compatibility](README.md#beta-status-and-compatibility) summary before installing.

What Twilight does, with no configuration:

- **Builds the Bedrock pack from your server's content.** Items, 3D models, armour, glyphs and menus,
  sounds, biomes and translations from ItemsAdder, CraftEngine, Nexo, Oraxen, BetterModel, ModelEngine,
  RealisticSeasons, CustomNameplates, datapacks and resource packs, validated and deployed to Geyser with
  snapshots for rollback.
- **Shows Java's layout on Bedrock.** Chest titles, chat, action bar, titles, boss bars, scoreboards and
  names use Java's font metrics, including stacked images and moved characters.
- **Serves networks.** twilight-proxy gives each Bedrock player the pack of the server they join, merges
  every server's custom items into the proxy's Geyser, runs the text layout there, works with login
  plugins and Floodgate, and can host the packs over HTTP.
- **Repairs itself after updates.** Stale Twilight files are moved out of Geyser, needed restarts are
  announced, and new versions are announced from GitHub or GitLab.

Changes since 1.0.0-pre.19: the update check orders stages as alpha, pre, beta, rc and release; the README
starts with the compatibility summary; issue, merge and pull request templates, a
[security policy](SECURITY.md) and [contribution notes](CONTRIBUTING.md) were added.

**Updating from 1.0.0-pre.14 to pre.19:** their update check sorts "beta" before "pre" and does not announce
this version. Replace `Twilight.jar` and `TwilightProxy.jar` by hand once; from now on new versions are
announced as usual.

Please report problems with the [issue templates](https://github.com/siberanka/twilight/issues/new/choose)
([GitLab](https://gitlab.com/siberanka/twilight/-/issues)), and vulnerabilities privately
([security policy](SECURITY.md)).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 248 tests
across 42 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

The notes below describe previous prereleases.

## Twilight 1.0.0-pre.19 - prerelease

This prerelease fixes two reports from a network with Geyser on the proxy
([field report](docs/FIELD_REPORT_2026-10-10.md)).

- **Packs load much faster where Geyser is not on the server.** Packs carried a second copy of every 3D
  item for item displays, which only a server's own Geyser uses. Every client still built it while
  loading. Backends of a proxy network now leave it out (`geyser.item-display-models: auto`), and pack
  JSON is compact. Survival's loading after joining fell from 156 s to under 10 s in a desktop client.
- **Players are no longer dropped while the packs load.** On the backend and on the proxy, Java
  keep-alives are answered for a client that is still loading, for up to 5 minutes
  (`loading-protection-seconds`). Each load of 10 s or more is logged.
- **Images in text on proxy networks.** twilight-proxy runs Twilight's text layout and translations in
  the proxy's Geyser. BoxPVP's ItemsAdder prefix (on U+A840, a character Bedrock draws with its own font)
  showed as a plain Unicode character because this ran only in a backend's Geyser.
- **Glyph pages.** `ui.max-glyph-cell: 256` keeps glyph pages at 4096x4096 for phones with little memory,
  and the build names pages of 8192x8192.

Nothing has to be done by hand: the first build after the start makes the new pack, and configuration
files without the new keys use the defaults. Item mappings do not change, so Geyser needs no restart.
Enabling the [pack host](WIKI.md#pack-hosting) on the proxy is recommended for large packs.

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 247 tests
across 42 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.18 - prerelease

This prerelease removes the manual steps an update needed, on proxy networks and single servers.

- **Stale files are moved out of Geyser automatically.** Old Twilight item mappings and packs left in
  Geyser by older versions, hand copies or sync tools caused broken item icons after updating. They are
  now moved to `retired/` before Geyser loads them, never deleted.
- **Backends on the same machine are read directly.** twilight-proxy finds them by port and loads each
  Twilight build from its folder. Geyser starts with every server's current items, without waiting for
  a player, and new builds arrive within seconds.
- **Restarts are announced and can be automatic.** Geyser registers custom items only when it starts.
  Admins are told when a restart is needed, and `when-empty` restarts once nobody is online.

Restart once after updating so Geyser registers the current items.

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 244 tests
across 42 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.17 - prerelease

This prerelease makes twilight-proxy work on proxies that also run Floodgate
([field report](docs/FIELD_REPORT_2026-10-09.md)).

- **Geyser attach with Floodgate.** Floodgate bundles its own copy of Geyser's event library. On a proxy
  that loaded Floodgate first (FlameCord, Waterfall, and Velocity, which does so by default), every
  earlier version failed with "loader constraint violation". Bedrock players then got no per-server
  packs and no pack host. Twilight and twilight-proxy now never link against that library. Tested on
  Velocity and Waterfall with Floodgate, including server switches of a Floodgate player.
- **Clearer failures.** An attach failure is logged with its full stack trace and with the jars the
  library came from.
- **Leftovers no longer count.** Login servers are read only from plugins the proxy loaded. Mapping
  files from older sync tools are named, with the items they map differently.

After updating from 1.0.0-pre.14 or older, restart Geyser once (the Bedrock item identifiers changed in
1.0.0-pre.15).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 239 tests
across 41 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.16 - prerelease

This prerelease clarifies two messages from the same field report
([field report](docs/FIELD_REPORT_2026-10-09.md)):

- **twilight-proxy on FlameCord and similar forks.** When the proxy lists a Geyser plugin but hides its
  classes from other plugins, twilight-proxy now says so, with the error, and keeps trying. Before, it
  reported "Geyser is not installed".
- **Twilight on backends without Geyser.** `/twilight status` and the start-up log now say that Geyser
  runs elsewhere, which is normal on a proxy network, and whether the pack is shared with twilight-proxy.
  Before, they reported an error.

1.0.0-pre.15 fixed conversions on Paper 1.21 servers with Java 17+, the Geyser attach on Velocity, custom
items on proxy networks and login server detection. Its update notes still apply: restart Geyser once
after updating from 1.0.0-pre.14 or older.

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 235 tests
across 41 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.15 - prerelease

This prerelease fixes the problems a proxy network reported with 1.0.0-pre.13; each was reproduced on
isolated servers first ([field report](docs/FIELD_REPORT_2026-10-09.md)).

- **Conversions work again on Paper 1.21 servers with Java 17+.** The content report no longer uses
  Gson reflection, which failed with "java.time.Instant#seconds" and stopped every build.
- **twilight-proxy finds Geyser reliably.** It starts after Geyser on Velocity, waits for a Geyser that
  is still loading, and logs why when it cannot attach. Before, such a Geyser was reported as "not
  installed" and Bedrock players got no per-server packs.
- **Custom items work on proxy networks.** twilight-proxy writes every backend's item mappings into the
  proxy's Geyser. The same Java item is the same Bedrock item on every backend, and conflicts between
  servers are listed.
- **Login servers are found automatically** in LeaderOS Auth, AuthMeVelocity, AuthMeBungee, LibreLogin
  and JPremium configurations.

After updating:

- restart Geyser once, because the Bedrock item identifiers changed;
- on a proxy, restart again after the first packs arrived;
- start the proxy's Java with `-Duser.language=en -Duser.country=US` when its locale is Turkish or
  Azerbaijani. Geyser then reads the item mappings; the log warns when this is needed.

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 234 tests
across 41 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.14 - prerelease

This prerelease adds an update check to Twilight and twilight-proxy.

- About 20 seconds after start and every six hours, each plugin reads the public release list on GitHub,
  or on the GitLab mirror when GitHub cannot be reached. A newer version is written to the console and
  shown, with a clickable link, to players with `twilight.update` (operators by default) or
  `twilight.proxy.update` when they join. `/twilight status` and `/twilightproxy` show the last result.
- Nothing about the server is sent and nothing is downloaded or installed; only version tags are read
  from the answer. Servers without internet access log one line and keep running unchanged.
- On by default; `update-check.enabled: false` turns it off and `update-check.notify-players: false`
  keeps it to the console, in `plugins/Twilight/config.yml` and `plugins/twilight-proxy/config.yml`.
  Existing configuration files need no change. See the [wiki](WIKI.md#updates).
- From this version on, every change is published as a new version; a published JAR is never replaced.

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 221 tests
across 39 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.13 - prerelease

This prerelease makes twilight-proxy's pack reconnects work with LeaderOS Auth Plus and similar login
plugins, BungeeGuard, Floodgate and Velocity modern forwarding, and closes a name-spoofing gap on
offline-mode networks.

- A reconnect counts as arrived only once the player is on its server. Login plugins that move players to
  their auth server after every other plugin no longer cause a second reconnect after the login.
- A fresh session that lands on another first server than expected (last-server reconnects, forced hosts)
  is reconnected once for that server's pack instead of keeping the wrong one.
- A Java player who takes a Bedrock player's name can no longer influence that player's reconnects, and a
  client's join address is used for a transfer only when it is a plain host name or IP address.
- Tested with LeaderOS Auth Plus 1.1.1 on BungeeCord (BungeeGuard, Floodgate) and Velocity (LimboAPI,
  Sonar): one reconnect per server change, with or without a login session. See the
  [login plugin test](docs/PROXY_AUTH_2026-10-09.md) and the
  [wiki](WIKI.md#reconnects-login-plugins-and-protections).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 215 tests
across 38 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.12 - prerelease

This prerelease makes twilight-proxy's pack reconnects reliable on networks with login plugins,
protections and large packs.

- Login plugins (AuthMe with AuthMeVelocity or AuthMeBungee, LibreLogin, nLogin, JPremium) keep the
  last word: a reconnected player may log in on the login server first and is then sent on to the
  server it chose. Before, a plugin that refused that server left the player without a server until it
  was dropped. A session's first server never causes a second reconnect; `login-servers` lists servers
  that never do.
- Reconnect deadlines grow with the pack (`transfer-timeout-seconds: auto`, up to an hour for very large
  packs) and every step is logged, including clients that do not come back within 60 seconds and the
  address they were sent to ("Server not found" on Bedrock), and a warning for a Velocity
  `login-ratelimit` longer than a reconnect.
- Geyser reads an immutable copy of each pack version, so packs rebuilt during a long download no longer
  break it; new packs are hashed before the first player needs them.
- Pack host: large downloads are no longer aborted by antivirus web shields (152 MiB: 123 s over HTTP
  instead of 188 s), and an address whose link did not work gets Geyser's transfer for 30 minutes with
  the reason in the log.
- Tested on Velocity and BungeeCord with a login plugin and packs up to 152 MiB: see the
  [reconnect test](docs/PROXY_RECONNECT_2026-10-08.md) and the
  [wiki](WIKI.md#reconnects-login-plugins-and-protections).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 214 tests
across 38 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.11 - prerelease

This prerelease adds the pack host: Bedrock players download the packs over HTTP from the server
that runs Geyser, and only they can.

- New `pack-host` section in Twilight's and twilight-proxy's `config.yml` (off by default). Like
  ItemsAdder's or CraftEngine's self-host it serves the packs from a TCP port, but a pack can only
  be fetched with a link minted for one Bedrock player connecting through Geyser: a random 256-bit
  token, valid for a few minutes and downloads, and by default only from that player's IP address.
  Every other request gets the same empty 404.
- Abuse limits: strict GET/HEAD parsing with time limits, connection and request limits per
  address, temporary blocks for guessing, a minimum transfer rate and immutable pack copies checked
  against Geyser's SHA-256.
- Bedrock-compatible: all file packs of a session are hosted together (Twilight's, Geyser's and
  other plugins'), pack options and content keys are kept, cached packs are not downloaded again, and
  a failed download falls back to Geyser's own transfer. Tested live with Bedrock for Windows on a
  backend and through Velocity; the fallback was tested with links to a closed port.
- Reverse proxies (HTTPS, `trusted-proxies`), NAT (`public-port`) and DDoS fronts are covered in the
  [wiki](WIKI.md#pack-hosting).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 209 tests
across 38 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.10 - prerelease

This prerelease draws custom boss bars and CustomNameplates' boss bar like Java and adds captures of
an animated mob model.

- Boss bars whose sprites a pack redraws (health and mana bars, notched overlays) are drawn with
  those sprites on Bedrock, in Java's order and cut at the bar's value; colour and style changes
  arrive live. Untouched colours keep Bedrock's bar.
- Long boss bar names are shown whole; before, a name longer than the first layer block was cut
  into pieces.
- CustomNameplates' default boss bar (three backgrounds with icons and shifted text) now fits
  Bedrock's 256-character limit and lands at Java's position.
- New report with captures and GIFs: [custom boss bars, CustomNameplates' boss bar and an animated
  model](docs/BOSSBARS_MODELS_2026-10-05.md).

The [JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 200 tests
across 37 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.9 - prerelease

This prerelease adds twilight-proxy: per-server Bedrock packs for networks that run Geyser on a
Velocity or BungeeCord proxy.

- Added `TwilightProxy.jar`. Each backend's pack comes from Twilight on that backend (`auto`), a
  file in `plugins/twilight-proxy/packs/` or a direct download link. A Bedrock player loads the pack
  of the server they join; moving to a server with another pack reconnects the client to Geyser
  (about five seconds with a cached pack) and sends it on. Tested live on Velocity 4.2.0 and
  BungeeCord 26.1 with two backends; see the [proxy test](docs/PROXY_2026-10-05.md).
- Twilight shares its exported pack over signed plugin messages. The secret is the one the proxy
  already shares with its servers (Velocity forwarding, BungeeGuard) or `proxy.secret`; requests
  carry timestamps and single-use nonces, clients can neither read nor forge the channel, transfers
  are bounded, and every pack is size-limited and checked before Geyser sees it.
- Bounded the text layout against crafted input (16384 characters, moves up to 2^20 units).
- Added the [wiki](WIKI.md) with every command, configuration key, file, API and the security model.

Releases now contain `Twilight.jar` (backends) and `TwilightProxy.jar` (proxies, optional). The
[JARs and SHA-256 files](artifacts/) were built locally under siberanka using Java 25; 191 tests
across 36 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, Geyser with custom content enabled;
for twilight-proxy, Velocity or BungeeCord with Geyser on the proxy. The runtime bridges target
Geyser 2.11.3; other Geyser core versions require validation.

## Twilight 1.0.0-pre.8 - prerelease

This prerelease reproduces stacked menu art and nameplates, shows custom and seasonal
biomes in their exact colours and makes every build work without setup.

- Drew text and images that Java moves back over earlier ones (stacked menu art,
  CustomNameplates backgrounds, shifted text) with one Bedrock label per layer in chest
  titles, the action bar and boss bars. Nine original-art menu styles, including four
  stacked images and a translucent highlight over the slots, land on Java's pixels.
- Showed custom biomes in their exact colours and climate through 25 redefined Bedrock
  biomes. RealisticSeasons' seasonal biomes are read from the server registry and the
  current season goes first; biome changes reach Bedrock players without a rejoin.
- Hid the Bedrock name of ridden players and mobs like Java and Bedrock's name tag box
  with CustomNameplates, so nameplates show only the plate.
- Added `sources.providers` (auto, generated, contents or off per provider),
  `sources.datapacks`, `geyser.send-pack-to-bedrock` and an export of every build to
  `plugins/Twilight/export` for a proxy Geyser or another plugin that sends the pack.
- Converted content Java tolerates (missing textures and models, screen-sized overlays,
  changed vanilla sounds) the way Java shows it: complete builds of seven production
  servers pass strict publication with the default configuration.

Results and remaining differences (text displays on mobs sit about 0.4 blocks lower,
the name tag box is hidden globally, hex text uses Bedrock's 28 colours) are in
[stacked images, nameplates and exact biomes](docs/LAYERS_BIOMES_2026-10-04.md).

The [JAR and SHA-256](artifacts/) were built locally under siberanka using Java 25;
183 tests across 34 suites passed. No hosted CI was run.

Runtime requirements: Java 21+, Paper/Folia/Spigot 1.21.4+, and Geyser with custom
content enabled. Twilight targets the newest Java and Bedrock versions first (tested
with Java 26.2 and Bedrock 1.26.5203.0). The display adapter, text layout, biome,
name and translation bridges target Geyser 2.11.3 build 1247 on the same server;
other Geyser core versions require validation.

## Twilight 1.0.0-pre.7 - prerelease

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

---

## Türkçe

### Twilight 1.0.0-beta.1 - ilk beta

Twilight ve twilight-proxy'nin ilk betası. Özellikler tek sunucular ve proxy'li ağlar için tamamdır ve üretim
ağlarında çalışıyor; davranış betalar arasında hâlâ değişebilir. Kurmadan önce
[beta durumu ve uyumluluk](README.md#beta-durumu-ve-uyumluluk) özetini okuyun.

Twilight'ın hiçbir yapılandırma gerektirmeden yaptıkları:

- **Bedrock paketini sunucunuzun içeriğinden derler.** ItemsAdder, CraftEngine, Nexo, Oraxen, BetterModel,
  ModelEngine, RealisticSeasons, CustomNameplates, datapack'ler ve kaynak paketlerinden eşyalar, 3B modeller,
  zırhlar, glifler ve menüler, sesler, biyomlar ve çeviriler; doğrulanır ve geri alma için anlık görüntülerle
  Geyser'a dağıtılır.
- **Java'nın yerleşimini Bedrock'ta gösterir.** Sandık başlıkları, sohbet, aksiyon çubuğu, başlıklar, boss
  çubukları, skor tabloları ve adlar, üst üste görseller ve taşınan karakterler dahil Java'nın font ölçülerini
  kullanır.
- **Ağlara hizmet eder.** twilight-proxy her Bedrock oyuncusuna katıldığı sunucunun paketini verir, her
  sunucunun özel eşyalarını proxy'deki Geyser'da birleştirir, yazı yerleşimini orada çalıştırır, giriş
  eklentileri ve Floodgate ile çalışır ve paketleri HTTP ile sunabilir.
- **Güncellemelerden sonra kendini onarır.** Eski Twilight dosyaları Geyser'dan taşınır, gereken yeniden
  başlatmalar duyurulur ve yeni sürümler GitHub veya GitLab'dan duyurulur.

1.0.0-pre.19'dan bu yana değişenler: güncelleme denetimi aşamaları alpha, pre, beta, rc ve kararlı sürüm olarak
sıralar; README uyumluluk özetiyle başlar; issue, birleştirme ve çekme isteği şablonları, bir
[güvenlik politikası](SECURITY.md#türkçe) ve [katkı notları](CONTRIBUTING.md#türkçe) eklendi.

**1.0.0-pre.14 ile pre.19 arasındaki bir sürümden güncelleme:** onların güncelleme denetimi "beta"yı "pre"den
önce sıralar ve bu sürümü duyurmaz. `Twilight.jar` ve `TwilightProxy.jar` dosyalarını bir kez elle değiştirin;
bundan sonra yeni sürümler her zamanki gibi duyurulur.

Sorunları [issue şablonlarıyla](https://github.com/siberanka/twilight/issues/new/choose)
([GitLab](https://gitlab.com/siberanka/twilight/-/issues)), güvenlik açıklarını ise gizli olarak
([güvenlik politikası](SECURITY.md#türkçe)) bildirin.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 42 paketteki 248
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

Aşağıdaki notlar önceki ön sürümleri anlatır.

#### Twilight 1.0.0-pre.19 - ön sürüm

Bu ön sürüm, Geyser'ın proxy'de çalıştığı bir ağdan gelen iki bildirimi düzeltir
([saha raporu](docs/FIELD_REPORT_2026-10-10.md)).

- **Geyser'ın sunucuda olmadığı yerlerde paketler çok daha hızlı yüklenir.** Paketler, eşya görüntüleri için
  her 3B eşyanın ikinci bir kopyasını taşıyordu; bunu yalnızca sunucunun kendi Geyser'ı kullanır. Yine de her
  istemci yüklerken bunu kuruyordu. Proxy'li ağın arka uçları artık bunu dışarıda bırakır
  (`geyser.item-display-models: auto`) ve paket JSON'u sıkışıktır. Survival'ın katıldıktan sonraki yüklemesi
  masaüstü istemcide 156 saniyeden 10 saniyenin altına indi.
- **Oyuncular artık paketler yüklenirken atılmaz.** Arka uçta ve proxy'de, hâlâ yükleyen bir istemcinin Java
  keep-alive'ları 5 dakikaya kadar onun yerine yanıtlanır (`loading-protection-seconds`). 10 saniye veya daha
  uzun süren her yükleme günlüğe yazılır.
- **Proxy'li ağlarda yazıdaki görseller.** twilight-proxy, Twilight'ın yazı yerleşimini ve çevirilerini
  proxy'deki Geyser'da çalıştırır. BoxPVP'nin ItemsAdder öneki (U+A840 üzerinde, Bedrock'un kendi fontuyla
  çizdiği bir karakter) bu iş yalnızca arka ucun Geyser'ında çalıştığı için düz bir Unicode karakteri olarak
  görünüyordu.
- **Glif sayfaları.** `ui.max-glyph-cell: 256`, belleği az olan telefonlar için glif sayfalarını 4096x4096'da
  tutar; derleme 8192x8192 sayfaları adlarıyla bildirir.

Elle yapılacak bir şey yok: açılıştan sonraki ilk derleme yeni paketi üretir; yeni anahtarları olmayan
yapılandırma dosyaları varsayılanları kullanır. Eşya eşlemeleri değişmez, bu yüzden Geyser'ın yeniden
başlatılması gerekmez. Büyük paketler için proxy'de [paket sunucusunu](WIKI.md#paket-sunucusu) açmanız önerilir.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 42 paketteki 247
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.18 - ön sürüm

Bu ön sürüm, proxy'li ağlarda ve tek sunucularda bir güncellemenin gerektirdiği elle yapılan adımları kaldırır.

- **Eski dosyalar Geyser'dan kendiliğinden taşınır.** Eski sürümlerin, elle yapılmış kopyaların veya eşitleme
  araçlarının Geyser'da bıraktığı eski Twilight eşya eşlemeleri ve paketleri, güncellemeden sonra bozuk eşya
  simgelerine yol açıyordu. Artık Geyser bunları yüklemeden önce `retired/` klasörüne taşınır, asla silinmez.
- **Aynı makinedeki arka uçlar doğrudan okunur.** twilight-proxy bunları porttan bulur ve her Twilight
  derlemesini klasöründen yükler. Geyser her sunucunun güncel eşyalarıyla, bir oyuncu beklemeden başlar; yeni
  derlemeler saniyeler içinde gelir.
- **Yeniden başlatmalar duyurulur ve otomatik olabilir.** Geyser özel eşyaları yalnızca açılışta kaydeder.
  Yeniden başlatma gerektiğinde yöneticilere bildirilir; `when-empty` kimse çevrimiçi değilken yeniden başlatır.

Geyser'ın güncel eşyaları kaydetmesi için güncellemeden sonra bir kez yeniden başlatın.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 42 paketteki 244
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.17 - ön sürüm

Bu ön sürüm twilight-proxy'yi Floodgate de çalıştıran proxy'lerde çalışır hâle getirir
([saha raporu](docs/FIELD_REPORT_2026-10-09.md)).

- **Floodgate ile Geyser'a bağlanma.** Floodgate, Geyser'ın olay kütüphanesinin kendi kopyasını içerir.
  Floodgate'i önce yükleyen bir proxy'de (FlameCord, Waterfall ve bunu varsayılan olarak yapan Velocity) önceki
  bütün sürümler "loader constraint violation" ile başarısız oluyordu. Bu durumda Bedrock oyuncuları sunucu
  başına paket almıyor, paket sunucusu da çalışmıyordu. Twilight ve twilight-proxy artık bu kütüphaneye asla
  bağlanmaz. Floodgate bulunan Velocity ve Waterfall'da, bir Floodgate oyuncusunun sunucu geçişleri dahil test
  edildi.
- **Daha açık hatalar.** Bir bağlanma hatası tam yığın iziyle ve kütüphanenin geldiği JAR'larla günlüğe yazılır.
- **Kalıntılar artık sayılmaz.** Giriş sunucuları yalnızca proxy'nin yüklediği eklentilerden okunur. Eski
  eşitleme araçlarının eşleme dosyalarının adı, farklı eşledikleri eşyalarla birlikte verilir.

1.0.0-pre.14 veya daha eski bir sürümden güncelledikten sonra Geyser'ı bir kez yeniden başlatın (Bedrock eşya
kimlikleri 1.0.0-pre.15'te değişti).

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 41 paketteki 239
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.16 - ön sürüm

Bu ön sürüm aynı saha raporundaki iki mesajı netleştirir ([saha raporu](docs/FIELD_REPORT_2026-10-09.md)):

- **FlameCord ve benzeri türevlerde twilight-proxy.** Proxy bir Geyser eklentisi listeleyip sınıflarını diğer
  eklentilerden gizlediğinde twilight-proxy artık bunu hatayla birlikte söyler ve denemeye devam eder. Önceden
  "Geyser is not installed" bildiriyordu.
- **Geyser'sız arka uçlarda Twilight.** `/twilight status` ve açılış günlüğü artık Geyser'ın başka yerde
  çalıştığını (proxy'li ağda normaldir) ve paketin twilight-proxy ile paylaşılıp paylaşılmadığını söyler.
  Önceden hata bildiriyordu.

1.0.0-pre.15; Java 17+ ile çalışan Paper 1.21 sunucularında dönüştürmeleri, Velocity'de Geyser'a bağlanmayı,
proxy'li ağlarda özel eşyaları ve giriş sunucusu algılamayı düzeltti. Onun güncelleme notları geçerlidir:
1.0.0-pre.14 veya daha eski bir sürümden güncelledikten sonra Geyser'ı bir kez yeniden başlatın.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 41 paketteki 235
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.15 - ön sürüm

Bu ön sürüm, proxy'li bir ağın 1.0.0-pre.13 ile bildirdiği sorunları düzeltir; her biri önce yalıtılmış
sunucularda yeniden üretildi ([saha raporu](docs/FIELD_REPORT_2026-10-09.md)).

- **Java 17+ ile çalışan Paper 1.21 sunucularında dönüştürmeler yeniden çalışır.** İçerik raporu artık Gson
  yansıması kullanmaz; bu yansıma "java.time.Instant#seconds" ile başarısız oluyor ve her derlemeyi
  durduruyordu.
- **twilight-proxy Geyser'ı güvenilir biçimde bulur.** Velocity'de Geyser'dan sonra başlar, hâlâ yüklenen bir
  Geyser'ı bekler ve bağlanamadığında nedenini günlüğe yazar. Önceden böyle bir Geyser "kurulu değil" olarak
  bildiriliyor ve Bedrock oyuncuları sunucu başına paket almıyordu.
- **Özel eşyalar proxy'li ağlarda çalışır.** twilight-proxy her arka ucun eşya eşlemelerini proxy'deki
  Geyser'a yazar. Aynı Java eşyası her arka uçta aynı Bedrock eşyasıdır ve sunucular arasındaki çakışmalar
  listelenir.
- **Giriş sunucuları kendiliğinden bulunur:** LeaderOS Auth, AuthMeVelocity, AuthMeBungee, LibreLogin ve
  JPremium yapılandırmalarından okunur.

Güncellemeden sonra:

- Bedrock eşya kimlikleri değiştiği için Geyser'ı bir kez yeniden başlatın;
- proxy'de ilk paketler geldikten sonra bir kez daha yeniden başlatın;
- proxy'nin dili Türkçe veya Azerice ise Java'sını `-Duser.language=en -Duser.country=US` ile başlatın.
  Geyser eşya eşlemelerini ancak böyle okur; gerektiğinde günlük uyarır.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 41 paketteki 234
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.14 - ön sürüm

Bu ön sürüm Twilight ve twilight-proxy'ye bir güncelleme denetimi ekler.

- Her eklenti açılıştan yaklaşık 20 saniye sonra ve her altı saatte bir herkese açık sürüm listesini GitHub'da,
  GitHub'a ulaşılamadığında GitLab yansısında okur. Daha yeni bir sürüm konsola yazılır ve `twilight.update`
  (varsayılan olarak operatörler) veya `twilight.proxy.update` iznine sahip oyunculara katıldıklarında
  tıklanabilir bir bağlantıyla gösterilir. `/twilight status` ve `/twilightproxy` son sonucu gösterir.
- Sunucu hakkında hiçbir şey gönderilmez, hiçbir şey indirilmez veya kurulmaz; yanıttan yalnızca sürüm
  etiketleri okunur. İnternet erişimi olmayan sunucular tek bir satır yazar ve değişmeden çalışmaya devam eder.
- Varsayılan olarak açıktır; `plugins/Twilight/config.yml` ve `plugins/twilight-proxy/config.yml` içinde
  `update-check.enabled: false` kapatır, `update-check.notify-players: false` yalnızca konsola yazar. Mevcut
  yapılandırma dosyalarında değişiklik gerekmez. [Wiki'ye](WIKI.md#güncellemeler) bakın.
- Bu sürümden itibaren her değişiklik yeni bir sürüm olarak yayımlanır; yayımlanmış bir JAR asla değiştirilmez.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 39 paketteki 221
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.13 - ön sürüm

Bu ön sürüm twilight-proxy'nin paket yeniden bağlanmalarını LeaderOS Auth Plus ve benzeri giriş eklentileri,
BungeeGuard, Floodgate ve Velocity modern yönlendirmesiyle çalışır hâle getirir ve çevrimdışı ağlardaki bir ad
taklidi açığını kapatır.

- Bir yeniden bağlanma ancak oyuncu sunucusundayken varmış sayılır. Oyuncuları diğer bütün eklentilerden sonra
  auth sunucusuna taşıyan giriş eklentileri artık girişten sonra ikinci bir yeniden bağlanmaya yol açmaz.
- Beklenenden başka bir ilk sunucuya düşen yeni bir oturum (son sunucuya geri bağlama, zorunlu sunucular)
  yanlış paketi tutmak yerine o sunucunun paketi için bir kez yeniden bağlanır.
- Bir Bedrock oyuncusunun adını alan bir Java oyuncusu artık o oyuncunun yeniden bağlanmalarını etkileyemez ve
  bir istemcinin katılma adresi aktarımda yalnızca düz bir alan adı veya IP adresiyse kullanılır.
- BungeeCord'da (BungeeGuard, Floodgate) ve Velocity'de (LimboAPI, Sonar) LeaderOS Auth Plus 1.1.1 ile test
  edildi: giriş oturumuyla veya oturumsuz, sunucu değişikliği başına tek yeniden bağlanma.
  [Giriş eklentisi testine](docs/PROXY_AUTH_2026-10-09.md) ve
  [wiki'ye](WIKI.md#yeniden-bağlanmalar-giriş-eklentileri-ve-korumalar) bakın.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 38 paketteki 215
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.12 - ön sürüm

Bu ön sürüm, twilight-proxy'nin paket yeniden bağlanmalarını giriş eklentileri, korumalar ve büyük paketler
bulunan ağlarda güvenilir hâle getirir.

- Giriş eklentileri (AuthMeVelocity veya AuthMeBungee ile AuthMe, LibreLogin, nLogin, JPremium) son sözü
  söyler: yeniden bağlanan bir oyuncu önce giriş sunucusunda giriş yapabilir ve sonra seçtiği sunucuya
  gönderilir. Önceden o sunucuyu reddeden bir eklenti oyuncuyu düşürülene kadar sunucusuz bırakıyordu. Bir
  oturumun ilk sunucusu asla ikinci bir yeniden bağlanmaya yol açmaz; `login-servers` hiç yol açmayan
  sunucuları listeler.
- Yeniden bağlanma süre sınırları paketle büyür (`transfer-timeout-seconds: auto`, çok büyük paketlerde bir
  saate kadar) ve her adım günlüğe yazılır; 60 saniye içinde geri gelmeyen istemciler ve gönderildikleri adres
  (Bedrock'ta "Sunucu bulunamadı") ile bir yeniden bağlanmadan uzun bir Velocity `login-ratelimit` için uyarı
  da buna dahildir.
- Geyser her paket sürümünün değişmez bir kopyasını okur; böylece uzun bir indirme sırasında yeniden derlenen
  paketler artık onu bozmaz; yeni paketlerin karması ilk oyuncu onlara ihtiyaç duymadan önce hesaplanır.
- Paket sunucusu: büyük indirmeler artık antivirüs web kalkanları tarafından yarıda kesilmez (152 MiB: 188 sn
  yerine HTTP ile 123 sn) ve bağlantısı çalışmayan bir adres, nedeni günlüğe yazılarak 30 dakika boyunca
  Geyser'ın aktarımını alır.
- Velocity ve BungeeCord üzerinde bir giriş eklentisi ve 152 MiB'e kadar paketlerle test edildi:
  [yeniden bağlanma testine](docs/PROXY_RECONNECT_2026-10-08.md) ve
  [wiki'ye](WIKI.md#yeniden-bağlanmalar-giriş-eklentileri-ve-korumalar) bakın.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 38 paketteki 214
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.11 - ön sürüm

Bu ön sürüm paket sunucusunu ekler: Bedrock oyuncuları paketleri Geyser'ı çalıştıran sunucudan HTTP ile
indirir ve bunu yalnızca onlar yapabilir.

- Twilight'ın ve twilight-proxy'nin `config.yml` dosyasında yeni `pack-host` bölümü (varsayılan olarak
  kapalı). ItemsAdder'ın veya CraftEngine'in self-host özelliği gibi paketleri bir TCP portundan sunar; ama bir
  paket yalnızca Geyser üzerinden bağlanan bir Bedrock oyuncusu için üretilmiş bir bağlantıyla alınabilir:
  256 bitlik rastgele bir belirteç, birkaç dakika ve birkaç indirme için geçerli ve varsayılan olarak
  yalnızca o oyuncunun IP adresinden. Diğer her istek aynı boş 404 yanıtını alır.
- Kötüye kullanım sınırları: zaman sınırlı katı GET/HEAD ayrıştırma, adres başına bağlantı ve istek
  sınırları, tahmin denemelerine geçici engeller, en düşük aktarım hızı ve Geyser'ın SHA-256 değeriyle
  denetlenen değişmez paket kopyaları.
- Bedrock uyumlu: bir oturumun bütün dosya paketleri birlikte sunulur (Twilight'ınki, Geyser'ınki ve diğer
  eklentilerinkiler), paket seçenekleri ve içerik anahtarları korunur, önbellekteki paketler yeniden
  indirilmez ve başarısız bir indirme Geyser'ın kendi aktarımına döner. Windows için Bedrock ile bir arka uçta
  ve Velocity üzerinden canlı test edildi; geri dönüş kapalı bir porta giden bağlantılarla test edildi.
- Ters proxy'ler (HTTPS, `trusted-proxies`), NAT (`public-port`) ve DDoS önyüzleri
  [wiki'de](WIKI.md#paket-sunucusu) anlatılır.

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 38 paketteki 209
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.10 - ön sürüm

Bu ön sürüm özel boss çubuklarını ve CustomNameplates boss çubuğunu Java gibi çizer ve animasyonlu bir mob
modelinin görüntülerini ekler.

- Sprite'larını bir paketin yeniden çizdiği boss çubukları (can ve mana çubukları, çentikli kaplamalar)
  Bedrock'ta bu sprite'larla, Java'nın sırasıyla ve çubuğun değerinde kesilerek çiziliyor; renk ve stil
  değişiklikleri canlı ulaşıyor. Dokunulmayan renkler Bedrock'un çubuğunu korur.
- Uzun boss çubuğu adları tamamen gösteriliyor; önceden ilk katman bloğundan uzun bir ad parçalara
  bölünüyordu.
- CustomNameplates'in varsayılan boss çubuğu (simgeli ve kaydırılmış yazılı üç arka plan) artık Bedrock'un
  256 karakter sınırına sığıyor ve Java'nın konumuna oturuyor.
- Görüntüler ve GIF'lerle yeni rapor: [özel boss çubukları, CustomNameplates boss çubuğu ve animasyonlu bir
  model](docs/BOSSBARS_MODELS_2026-10-05.md).

[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 37 paketteki 200
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.9 - ön sürüm

Bu ön sürüm twilight-proxy'yi ekler: Geyser'ı bir Velocity veya BungeeCord proxy'sinde çalıştıran ağlar için
sunucu başına Bedrock paketleri.

- `TwilightProxy.jar` eklendi. Her arka ucun paketi o arka uçtaki Twilight'tan (`auto`),
  `plugins/twilight-proxy/packs/` içindeki bir dosyadan veya doğrudan bir indirme bağlantısından gelir. Bir
  Bedrock oyuncusu katıldığı sunucunun paketini yükler; başka paketli bir sunucuya geçiş istemciyi Geyser'a
  yeniden bağlar (önbellekteki bir paketle yaklaşık beş saniye) ve yönlendirir. Velocity 4.2.0 ve
  BungeeCord 26.1 üzerinde iki arka uçla canlı test edildi; [proxy testine](docs/PROXY_2026-10-05.md) bakın.
- Twilight dışa aktardığı paketi imzalı eklenti mesajlarıyla paylaşır. Gizli anahtar, proxy'nin
  sunucularıyla zaten paylaştığı anahtardır (Velocity yönlendirmesi, BungeeGuard) veya `proxy.secret`'tir;
  istekler zaman damgası ve tek kullanımlık nonce taşır, istemciler kanalı ne okuyabilir ne de sahteleyebilir,
  aktarımlar sınırlıdır ve her paket Geyser görmeden önce boyutla sınırlanır ve denetlenir.
- Yazı yerleşimi özel hazırlanmış girdiye karşı sınırlandı (16384 karakter, en fazla 2^20 birimlik
  kaydırmalar).
- Her komutu, yapılandırma anahtarını, dosyayı, API'yi ve güvenlik modelini içeren [wiki](WIKI.md) eklendi.

Sürümler artık `Twilight.jar` (arka uçlar) ve `TwilightProxy.jar` (proxy'ler, isteğe bağlı) içeriyor.
[JAR'lar ve SHA-256 dosyaları](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 36 paketteki 191
test geçti. Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+, özel içeriği açık Geyser; twilight-proxy
için proxy'de Geyser bulunan Velocity veya BungeeCord. Çalışma zamanı köprüleri Geyser 2.11.3'ü hedefler;
diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.8 - ön sürüm

Bu ön sürüm üst üste menü görsellerini ve ad etiketlerini yeniden üretir, özel ve mevsimsel biyomları
birebir renkleriyle gösterir ve her derlemeyi kurulumsuz çalışır hâle getirir.

- Java'nın öncekilerin üzerine geri aldığı yazı ve görseller (üst üste menü görselleri, CustomNameplates
  arka planları, kaydırılmış yazı) sandık başlıklarında, aksiyon çubuğunda ve boss çubuklarında katman başına
  bir Bedrock etiketiyle çizildi. Dört üst üste görsel ve yuvaların üzerinde yarı saydam bir vurgu dahil
  dokuz özgün görselli menü stili Java'nın piksellerine oturuyor.
- Özel biyomlar yeniden tanımlanmış 25 Bedrock biyomuyla birebir renkleri ve iklimleriyle gösterildi.
  RealisticSeasons'ın mevsimsel biyomları sunucu kayıt defterinden okunur ve geçerli mevsim önce gelir; biyom
  değişiklikleri Bedrock oyuncularına yeniden katılmadan ulaşır.
- Binilen oyuncuların ve mobların Bedrock adı Java gibi, CustomNameplates ile Bedrock'un ad etiketi kutusu da
  gizlendi; böylece ad etiketleri yalnızca etiketi gösterir.
- `sources.providers` (sağlayıcı başına auto, generated, contents veya kapalı), `sources.datapacks`,
  `geyser.send-pack-to-bedrock` ve bir proxy Geyser'ı veya paketi gönderen başka bir eklenti için her
  derlemenin `plugins/Twilight/export` dizinine dışa aktarımı eklendi.
- Java'nın tolere ettiği içerik (eksik dokular ve modeller, ekran boyutunda kaplamalar, değiştirilmiş vanilla
  sesler) Java'nın gösterdiği şekilde dönüştürüldü: yedi üretim sunucusunun tam derlemeleri varsayılan
  yapılandırmayla katı yayını geçiyor.

Sonuçlar ve kalan farklar (moblardaki yazı görüntüleri yaklaşık 0,4 blok aşağıda duruyor, ad etiketi kutusu
genel olarak gizleniyor, hex yazı Bedrock'un 28 rengini kullanıyor)
[üst üste görseller, ad etiketleri ve birebir biyomlar](docs/LAYERS_BIOMES_2026-10-04.md) belgesindedir.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 34 paketteki 183 test geçti.
Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Twilight
önce en yeni Java ve Bedrock sürümlerini hedefler (Java 26.2 ve Bedrock 1.26.5203.0 ile test edildi).
Görüntü bağdaştırıcısı, yazı yerleşimi, biyom, ad ve çeviri köprüleri aynı sunucudaki Geyser 2.11.3 derleme
1247'yi hedefler; diğer Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.7 - ön sürüm

Bu ön sürüm Java oyuncularının CraftEngine, ItemsAdder, Nexo ve diğer sağlayıcılardan gerçekten aldığı
içeriği dönüştürür, datapack biyomlarını ve paket çevirilerini Bedrock'ta gösterir; 104 gerçek menü ve
özgün görselli bir menü stil takımıyla doğrulandı.

- Her sağlayıcının ürettiği paket (CraftEngine `resource_pack.zip`, ItemsAdder çıktısı, Nexo/Oraxen
  paketleri) çalışma klasörlerine göre yetkili kabul edildi. Kurulu olmayan veya başka bir sağlayıcı
  gönderirken kendi paketini göndermeyen sağlayıcıların paketleri yalnızca boşlukları doldurur.
  CustomNameplates ve BetterHUD paketleri keşfedilir; Nexo'nun vanilla varlık önbelleği yok sayılır. Altı
  gerçek sunucunun tam derlemeleri artık örneğin Survival'da 1.932 özel eşyadan 1.905'ini dönüştürüyor.
- Datapack ve eklenti biyomları (Terralith, Incendium, RealisticSeasons) Bedrock'ta Geyser'ın okyanus yedeği
  yerine en yakın renklere ve yağışa sahip vanilla biyom olarak gösterildi (`world.bedrock-biome-matching`).
- Kaynak paketlerinin çevirileri Java gibi birleştirildi: Bedrock oyuncuları datapack ve eklenti içeriğinin
  adlarını ve ender sandığı başlığı olarak bir görsel veya gizli envanter etiketi gibi paket geçersiz
  kılmalarını görür (`ui.java-translations`).
- Renk kodu olmayan konteyner başlıklarının görselleri Java gibi, önceden koyulaştırılmış glif kopyalarıyla
  koyulaştırıldı (`ui.java-glyph-tint`; test edilen en büyük sunucuda yaklaşık 330 MiB ek atlas belleği).
- Kalın başlıklar Java'nın konumunda tutuldu (dört birim sağdaydılar) ve yerleştirilmiş yazıdaki eski renk
  kodlarının yer kaplaması önlendi.

Sonuçlar: 104 gerçek menünün her penceresi ve 97 başlık bandı Java ile eşleşti; diğer yedisi yalnızca
Bedrock'un kendi yazı fontunda farklı. Stil takımında, Bedrock yazısının yeniden üretemediği önceki bir
görselin üzerine geri alınan katmanlar dışında her teknik eşleşti. Özel bir datapack biyomu okyanus yerine en
yakın vanilla biyomu (kiraz bahçesi) gösterdi. [Arayüz kampanyasına](docs/UI_CAMPAIGN_2026-10-04.md) bakın.
Eşya adları ve açıklamaları yeniden yazılmaz ve yüksek çözünürlüklü glif örneklemesi farklıdır.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 28 paketteki 156 test geçti.
Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Görüntü
bağdaştırıcısı, yazı yerleşimi, biyom ve çeviri köprüleri Geyser 2.11.3 derleme 1247'yi hedefler; diğer
Geyser çekirdek sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.6 - ön sürüm

Bu ön sürüm Java font içeriğini her Bedrock yazı yüzeyinde doğru gösterir ve beş gerçek sunucunun tam
derlemelerinin bulduğu dönüşüm hatalarını düzeltir.

- Java yazısı Bedrock oyuncuları için sohbette, aksiyon çubuğunda, başlık ve alt başlıklarda, boss
  çubuklarında, skor tablolarında, varlık adlarında, yazı görüntülerinde ve huni, fırın ve diğer
  konteynerlerin başlıklarında yerleştirildi. Adlandırılmış font görselleri (örneğin CraftEngine rütbeleri),
  yeniden eşlenmiş karakterler ve ItemsAdder kaydırmaları artık Java'nın konumlarında görünür; ortalı
  satırlar Java'nın tam sayı ortalamasını izler ve sohbet kaydırma için boşluklarını korur.
  `ui.java-text-surfaces` ile denetlenir; yazı yerleşimi artık konteyner yerleşimini gerektirmez.
  [Yazı yüzeyi incelemesine](docs/TEXT_SURFACES_2026-10-04.md) bakın.
- ItemsAdder'ın vanilla varlık kopyaları, geçici derleme klasörleri ve eskimiş iç içe paketler artık kaynak
  sayılmıyor (yüzlerce özel eşyayı gizliyorlardı) ve `generated.zip` yoksa yeniden adlandırılmış bir
  ItemsAdder çıktısı kullanılıyor.
- Doku atlası sprite yeniden adlandırmaları çözüldü, korumalı PNG'ler Java gibi çözüldü, nesne biçimli model
  dokuları okundu, özel seçicilerin arkasında vanilla modellere izin verildi ve tanımsız yüz dokuları için
  Java'nın eksik dokusu kullanıldı.
- Java'nın kendi font sayfalarındaki karakterler Bedrock yazısı olarak tutuldu ve ekran dışı veya saydam
  aralık görselleri ilerleme sayıldı; bu, gerçek sunuculardaki özel kullanım sayfası taşmasını giderdi.
- Java'nın da reddettiği içerik (bozuk fontlar, okunamayan TrueType dosyaları, hiçbir pakette bulunmayan ses
  dosyaları, kaplamayla çizilen kafalar) katı derlemeleri başarısız kılmak yerine `build-report.json` içinde
  bildirim olarak raporlandı.

Her yeni yüzey bağımsız SkyBlock içeriğiyle (CraftEngine rütbeleri ve simgeleri, CustomNameplates arka
planları) Java'ya karşı sıfır kayma ölçtü; altı gerçek Survival menüsü ve türlü konteyner ekranları
değişmedi. Aynı beş sunucunun tam derlemeleri iyileşti; örneğin Survival'da 130 özel eşyadan 125'inden 841
özel eşyadan 837'sine. Kalan farklar: eşya adları ve açıklamaları yeniden yazılmıyor, yazının hemen ardından
boşluksuz gelen bir glif ve üst üste binen katmanlar bir birim kayık, bitmap renklendirmesi ve yüksek
çözünürlüklü glif örneklemesi farklı.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 25 paketteki 138 test geçti.
Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Görüntü
bağdaştırıcısı ve yazı yerleşimi Geyser 2.11.3 derleme 1247'yi hedefler; diğer Geyser çekirdek sürümleri
doğrulama gerektirir.

#### Twilight 1.0.0-pre.5 - ön sürüm

Bu ön sürüm Java font görselli menü başlıklarını Bedrock'ta Java font ölçüleriyle yerleştirir; böylece
gerçek menü görselleri Java'nın arayüz piksellerine oturur.

- Sandık başlıkları Bedrock oyuncuları için Java font ölçüleriyle yerleştirildi. Boşluk sağlayıcıları,
  negatif yükseklikli bitmap kaydırmaları ve ItemsAdder kaydırmaları birebir görünmez aralık gliflerine
  dönüşür; bitmap glif kenar payları düzeltilir (önceki bir birimlik kayma kaldırıldı); Java'nın varsayılan
  veya adlandırılmış fontlarda yeniden eşlediği karakterler Bedrock'un gliflerinin yerini almak yerine özel
  kullanım takma adları alır. `ui.java-text-layout` ile denetlenir.
  [Yazı yerleşimi incelemesine](docs/TEXT_LAYOUT_2026-10-03.md) bakın.
- Her kaynak paketinin font tanımları Java gibi birleştirildi ve Java'nın yükleyemediği TrueType fontlar yok
  sayıldı.
- Dokunmatik yerleşimin yerel ortalı başlığı yalnızca glif değiştirmeyle korundu.

Altı gerçek Survival menüsü (negatif yükseklik kaydırmaları, bir ItemsAdder kaydırması ve 512 piksellik bir
görsel) başlık alanında ve ölçülen tüm yuva satırlarında Java ile eşleşti; huni, fırın ve fırlatıcı ekranları
vanilla Bedrock ile aynı kaldı. Kalan farklar: yazının hemen ardından boşluksuz gelen bir glif bir birim
sağda, üst üste binen başlık katmanları yeniden üretilmiyor, bitmap renklendirmesi farklı ve sohbet,
açıklamalar, skor tabloları ve boss çubukları henüz yerleştirilmiyor.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 22 paketteki 109 test geçti.
Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Görüntü
bağdaştırıcısı ve başlık yerleşimi Geyser 2.11.3 derleme 1247'yi hedefler; diğer Geyser çekirdek sürümleri
doğrulama gerektirir.

#### Twilight 1.0.0-pre.4 - ön sürüm

Bu ön sürüm Java font görselli menüleri Bedrock masaüstü sandık ekranlarında kullanılabilir yapar ve geniş
bitmap glifleri yazarın verdiği boyutta korur.

- Bedrock masaüstü sandık ekranları için bir Java konteyner yerleşimi üretildi. Geniş başlık görselleri artık
  kaydırılmaz, kısa çizgiyle bölünmez veya kırpılmaz; başlık ve envanter etiketleri Java'nın konumlarını,
  rengini ve çizim sırasını kullanır; sandık, oyuncu envanteri ve kısayol çubuğu satırları 1 ile 6 sandık
  satırı için Java'nın aralığını korur. Kısmi arayüz Bedrock'un vanilla arayüzüyle birleşir ve
  `ui.java-container-layout` ile kapatılabilir. [Konteyner yerleşimi incelemesine](docs/CONTAINER_LAYOUT_2026-10-03.md)
  bakın.
- Bitmap font atlas hücreleri her glifin Java görüntü boyutu ve ascent değeri korunarak kendiliğinden
  büyütüldü; böylece geniş rütbe etiketleri ve büyük arayüz/HUD görselleri artık küçültülmez veya atlanmaz.
  [Geniş glif incelemesine](docs/WIDE_GLYPHS_2026-10-02.md) bakın.
- Başlık yerleşimi ve katmanları, yuva aralığı, diğer ekranlar için vanilla varsayılanlar, glif ad
  alanları, çok satırlı glif sayfaları, geniş glif pikselleri ve komşu hücreler için regresyonlar eklendi.
- Kabul sözleşmesi 40 alana ve 289 gerekli senaryoya genişletildi.

Eşlenmiş Java/Bedrock görüntüleri 9'dan 54 yuvaya her sandık boyutunu ölçtü ve huni, fırlatıcı ve fırın
ekranlarının vanilla Bedrock arayüzüyle aynı kaldığını doğruladı. Bilinen farklar sürüyor: Bedrock bitmap
glifleri yazı rengiyle renklendirmez, her bitmap glifi bir arayüz birimi sağa çizer, saydam sol dolguyu
kırpar ve kesirli boyutları farklı örnekler. Java boşluk ilerlemeleri, diğer konteyner türleri, dokunmatik
yerleşimler, açıklama kutuları, canlı HUD'lar, tam animasyon ve birinci şahıs eşdeğerliği henüz
desteklenmiyor.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 20 paketteki 95 test geçti.
Barındırılan CI çalıştırılmadı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Geliştirme
aşamasındaki görüntü bağdaştırıcısı özellikle Geyser 2.11.3 derleme 1247'yi hedefler; diğer Geyser çekirdek
sürümleri doğrulama gerektirir.

#### Twilight 1.0.0-pre.3 - ön sürüm

Bu ön sürüm özel eşya dönüşünü ve doku dönüşümünü düzeltir; desteklenmeyen arayüz dönüşümünü ve Geyser
yeniden başlatma gereksinimlerini açık hâle getirir.

- Bitmap glif taban çizgileri Java yükseklik/ascent değerleri ve ölçülmüş Bedrock hücre koordinatlarıyla
  düzeltildi; negatif ascent, taşma ve sayfa bağımsızlığı regresyonları eklendi.
  [Font ölçüleri incelemesine](docs/FONT_METRICS_2026-10-01.md) bakın.
- Envanter simgeleri ilk ham dokuyu dışa aktarmak yerine model yüzlerinden ve devralınan Java arayüz
  pozundan çizildi.
- Tek tek durağan bileşik alt model pozları korunarak ayrık BetterModel kafa parçaları ve istenmeyen eğim
  [yeni örnek regresyonunda](docs/COMPOSITE_MODELS_2026-09-30.md) düzeltildi.
- Üçüncü şahıs model çerçevesi dönüşümü, sol el aynalaması ve bir Euler tekilliği düzeltildi.
- Döndürülmüş küboid eksenleri, varsayılan yüz UV'leri ve yüz UV dönüşü düzeltildi.
- Sprite sayfalarını germek yerine yazarın verdiği ilk animasyon karesi dışa aktarıldı; tam animasyon
  oynatımı desteklenmiyor.
- Aşırı büyük arayüz glifleri, görünür taban çizgisi taşması ve özel aralık katı modda reddedildi. Tanı
  dışa aktarımları atlamaları raporlar.
- Eşleme yeniden başlatma durumu yapılandırma yeniden yüklemesi, tekrarlanan dağıtımlar ve geri alma boyunca
  korundu. Geyser yeniden yüklemesi değişen eşya eşlemelerini etkinleştiremez.
- Geyser eşleme enum ayrıştırmasını bozan JVM yerel ayarları için bir uyarı eklendi.
- Canlı bir eşya görüntüsü köprüsü eklendi ve sabit MythicMobs modelleri dahil örneklenen BetterModel ve
  ModelEngine pozları doğrulandı.
- Görüntü görünmezliği ve sıfır görüş menzili işleme düzeltildi; etkin olmayan ModelEngine ateş katmanları
  artık başıboş düzlemler olarak görünmüyor.
- Bağlı görüntülerin yönü kafa ve gövde yaw değerleri hizalanıp yinelenen ağ yaw değeri kaldırılarak
  düzeltildi.
- Görünmez sıfır yarıçaplı bulut çapaları kendiliğinden uyarlandı; olağan bulutlar ve bağlı yolcular
  korunurken istenmeyen ModelEngine parçacıkları kaldırıldı.
- Buluta binen görüntü yüksekliği Java ile eşleşecek şekilde düzeltildi; test edilen sepetin katı zemindeki
  karanlık görünümü gündüz/gece ışıklandırması korunarak giderildi.
- Kaynak paketi kaplamaları hedef Minecraft sürümüne göre seçildi ve açık vanilla doku bağımlılıkları çözüldü.
- Noktalı ve büyük harfli kaplama dizin adları kabul edilerek sürümlü Survival içeriğinin keşfi düzeltildi.
- Devre dışı sağlayıcılar için olay kancaları atlandı.

[Gerçek içerik incelemesi](docs/REAL_CONTENT_REVIEW.md) Java referanslarını, Bedrock gözlemlerini ve kabul
edilmemiş sohbet/arayüz denetimlerini içerir. Java kaynak varlıkları değişmedi. Bu kontrol noktası tam görsel
eşdeğerlik veya üretim arayüz desteği iddia etmez. [Ek yedi modellik matris](docs/MODEL_MATRIX_2026-09-28.md)
yön, ışıklandırma ve parçacık hatalarını düzeltmelerin devam kanıtıyla kaydeder.
[Bulut çapası regresyonu](docs/CLOUD_ANCHORS_2026-09-29.md) dört ModelEngine modelini, canlı
yarıçap/görünürlük değişikliklerini, yolcu korunmasını ve istemci yeniden bağlanmasını kapsar.
[Binek yüksekliği regresyonu](docs/DISPLAY_SEATS_2026-09-29.md) Java'nın bağlantı noktasını ölçer ve sepeti
değişmemiş taş üzerinde gündüz ve gece doğrular. [Genişletilmiş içerik denetimleri](docs/BROAD_CONTENT_2026-09-29.md)
tüm kaynak derlemesini, 36 envanter örneğini, altı ek elde tutulan eşyayı, 28 emojiyi ve Survival menü
görsellerini kapsar; tam birinci şahıs, animasyonlu doku ve menü eşdeğerliğinin hâlâ olmadığını doğrular.

[JAR ve SHA-256](artifacts/) siberanka adına Java 25 ile yerelde derlendi; 19 paketteki 84 test geçti.
Barındırılan CI çalıştırılmadı. Otomatik GitHub/GitLab hat tetikleyicileri kapalı.

Çalışma zamanı gereksinimleri: Java 21+, Paper/Folia/Spigot 1.21.4+ ve özel içeriği açık Geyser. Geliştirme
aşamasındaki görüntü bağdaştırıcısı özellikle Geyser 2.11.3 derleme 1247'yi hedefler; diğer Geyser çekirdek
sürümleri doğrulama gerektirir. Tam animasyonlu dokular, renklendirme, billboard davranışı ve arayüz
uyarlaması açık kabul işi olarak kalıyor.

Son font görüntüleri altı birebir normalleştirilmiş yükseklik/taban çizgisi birleşimini, desteklenen 35
gerçek glifi ve test edilen envanter başlığını doğrular. Java başlık renk modülasyonu, geniş rütbe etiketleri
ve tam özel arayüz arka planları desteklenmiyor veya görsel olarak farklı. Aşırı büyük içerik küçültülmek
yerine raporlanır. Kesin kapsam için [ölçülmüş karşılaştırmaya](docs/FONT_METRICS_2026-10-01.md) bakın.
