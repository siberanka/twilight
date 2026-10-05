# Twilight wiki

Twilight converts the custom content of a Java server (ItemsAdder, Nexo, CraftEngine, Oraxen,
BetterModel, ModelEngine, CustomNameplates, BetterHUD, datapacks and RealisticSeasons) into a Bedrock
resource pack and Geyser mappings, and makes Bedrock players see it the way Java players do. The
project ships two plugins:

| Plugin | Runs on | Purpose |
|---|---|---|
| `Twilight.jar` | Paper, Folia, Spigot 1.21.4+ (backend servers) | Builds the Bedrock pack, deploys it to Geyser, lays out text, biomes and names at runtime |
| `TwilightProxy.jar` (twilight-proxy) | Velocity, BungeeCord/Waterfall (proxies) | Gives every backend server its own Bedrock pack through Geyser on the proxy |

This page is the reference for administrators and plugin developers. Measurements and screenshots
are in the [reports](docs/); limits per feature are in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Contents

1. [Requirements](#requirements)
2. [Installation](#installation)
3. [How a build works](#how-a-build-works)
4. [Commands and permissions](#commands-and-permissions)
5. [Configuration reference: Twilight](#configuration-reference-twilight)
6. [twilight-proxy](#twilight-proxy)
7. [Files and folders](#files-and-folders)
8. [Developer API](#developer-api)
9. [Plugin-message protocol](#plugin-message-protocol)
10. [Security model](#security-model)
11. [Troubleshooting](#troubleshooting)
12. [Building from source](#building-from-source)

## Requirements

| Component | Version |
|---|---|
| Java | 21 or newer (Twilight is built with Java 25, release 21) |
| Backend server | Paper, Folia or Spigot 1.21.4 or newer; newest versions first (tested on Paper 26.2) |
| Geyser | 2.x with custom content enabled; the runtime bridges are validated against Geyser 2.11.3 |
| Bedrock clients | The current release (tested with 1.26.5203); older ones as far as Geyser supports them |
| Proxy (optional) | Velocity 3.x/4.x or BungeeCord/Waterfall with Geyser on the proxy (tested on Velocity 4.2.0 and BungeeCord 26.1) |

## Installation

### A single server

1. Put `Twilight.jar` in `plugins/` next to Geyser-Spigot and start the server.
2. Nothing has to be configured. After the server and the content plugins have loaded, Twilight
   finds their packs, builds the Bedrock pack, deploys it to Geyser and reloads Geyser.
3. Bedrock players receive the pack the next time they join. When item mappings changed, the
   console asks for one restart, because Geyser registers items only at startup.

### A network with a proxy

1. Install `Twilight.jar` on every backend that has custom content; Geyser-Spigot on the backends
   is not needed.
2. Install Geyser and `TwilightProxy.jar` on the proxy.
3. Forward player data with Velocity's `modern` (or `bungeeguard`) forwarding or BungeeGuard. Both
   plugins then share that secret automatically. Without it, set the same `secret` in
   twilight-proxy's `config.yml` and `proxy.secret` in each backend's Twilight `config.yml`.
4. Each Bedrock player now loads the pack of the server they join; see [twilight-proxy](#twilight-proxy).

## How a build works

1. **Discovery.** Twilight reads the folders and generated packs of the installed providers. The
   pack a provider sends to Java players outranks its working folders, which fill gaps; a provider
   whose settings do not send its pack, or whose plugin is not installed, only fills gaps. World
   datapacks and `sources.additional` are added. Each provider can be limited with
   `sources.providers`.
2. **Runtime items.** Recipe results, online inventories and the providers' item registries are read
   through public APIs, so items that exist only at runtime are converted too.
3. **Compilation.** Item models, textures, fonts, sounds, translations, biome looks and UI files are
   converted. Content that Java itself shows broken (a missing texture or model, a screen-sized
   overlay glyph) is converted the way Java shows it and reported as a notice. Content that cannot be
   represented safely is a problem; with `generation.strict: true` a build with problems is not
   published, except for the very first pack of a server.
4. **Export and deployment.** The pack and Geyser mappings are written to
   `plugins/Twilight/export/` and deployed transactionally to Geyser (with snapshots for rollback).
5. **Runtime.** While players play, Twilight lays out Java text for Bedrock (titles, chat, action bar,
   boss bars, scoreboards, names, text displays), applies Java's name rules, maps custom biomes and
   forwards biome updates.

Builds run again automatically after provider reloads, provider pack commands and RealisticSeasons
season changes. Unchanged inputs are detected by a fingerprint and skipped.

## Commands and permissions

### Twilight (backend)

| Command | Description |
|---|---|
| `/twilight status` | Operation state, last scan, input fingerprint, Geyser folder and snapshots |
| `/twilight scan` | Discover and inspect sources without building |
| `/twilight convert` (alias `build`) | Scan, build, export and deploy |
| `/twilight deploy` | Deploy the last build to Geyser again |
| `/twilight rollback [n]` | Restore the n-th newest Geyser snapshot (default 1) |
| `/twilight reload` | Reload and validate `config.yml` |

Alias: `/tw`. Permission: `twilight.admin` (default: operators). Long operations run off the server
thread; only one runs at a time.

### twilight-proxy

| Command | Description |
|---|---|
| `/twilightproxy` | Shared-secret state, Geyser presence and the pack of every server |
| `/twilightproxy reload` | Reload `config.yml` and every pack |

Alias: `/twproxy`. Permission: `twilight.proxy.admin`.

## Configuration reference: Twilight

`plugins/Twilight/config.yml`. Every key has a working default; the file only needs editing to
change behaviour.

### Top level

| Key | Default | Meaning |
|---|---|---|
| `vanilla-override` | `false` | Allow the pack to replace vanilla Bedrock assets (sounds, glyphs) that Java packs change |

### `generation`

| Key | Default | Meaning |
|---|---|---|
| `strict` | `true` | A build with problems is not published (the first pack of a server still is) |
| `auto-build-on-startup` | `true` | Build after the server and providers have loaded |
| `sync-provider-changes` | `true` | Build again after provider reload/pack events and commands |
| `startup-delay-ticks` | `40` | Delay before automatic builds (1-72000) |
| `provider-command-delay-ticks` | `100` | Delay after a provider command, for its output to be written |
| `maximum-source-bytes` | `1073741824` | Upper limit of all source bytes read in one build |
| `maximum-archive-entries` | `100000` | Upper limit of entries per source archive |
| `download-vanilla-assets` | `true` | Download the version's client assets from Mojang (hash-verified) when a build needs them |

### `sources`

| Key | Default | Meaning |
|---|---|---|
| `auto-discover` | `true` | Find providers and world datapacks on their own |
| `providers.<name>` | `auto` | Per provider (`itemsadder`, `nexo`, `craftengine`, `oraxen`, `bettermodel`, `modelengine`, `customnameplates`, `betterhud`, `realisticseasons`): `auto` / `true`, `generated` (only the generated pack), `contents` (only working folders), `off` / `false` |
| `datapacks` | `true` | Read the worlds' datapacks |
| `additional` | `[]` | More packs or datapacks (folders or ZIPs), relative to the server folder |

### `geyser`

| Key | Default | Meaning |
|---|---|---|
| `directory` | `auto` | Geyser's data folder; `auto` finds `plugins/Geyser-*` |
| `deploy-after-build` | `true` | Deploy each successful build |
| `reload-after-deploy` | `true` | Run `geyser reload` after deploying when no restart is required |
| `backups-to-keep` | `3` | Geyser snapshots kept for `/twilight rollback` (1-20) |
| `send-pack-to-bedrock` | `true` | Let Geyser send the pack; `false` when a proxy or another plugin sends `export/Twilight.mcpack` |

### `ui`

| Key | Default | Meaning |
|---|---|---|
| `java-container-layout` | `true` | Java's chest layout and title position in Bedrock chest screens |
| `java-text-layout` | `true` | Lay out text with Java font metrics (spaces, negative spaces, images) |
| `java-text-surfaces` | `true` | Apply the layout to chat, action bar, titles, boss bars, scoreboards, names and text displays |
| `java-glyph-tint` | `true` | Darkened image copies for container titles without a colour, as Java draws them |
| `java-text-layers` | `true` | Text and images drawn back over earlier ones get one label per layer (chest titles, action bar, boss bars) |
| `nametag-background` | `auto` | Bedrock's name tag box: `auto` (hidden when CustomNameplates' name tags are on), `hidden`, `bedrock` |
| `java-translations` | `true` | Use the resource packs' translations for Bedrock players |

### `world`

| Key | Default | Meaning |
|---|---|---|
| `bedrock-biome-matching` | `true` | Exact colours and climate for custom biomes in 25 redefined Bedrock biomes, the closest vanilla biome otherwise, and live biome updates |

### `proxy`

| Key | Default | Meaning |
|---|---|---|
| `share-pack` | `true` | Offer the exported pack to twilight-proxy over signed plugin messages |
| `secret` | `""` | Secret shared with twilight-proxy (16+ characters); empty uses Paper's Velocity secret or BungeeGuard tokens |

Changes to `ui` and `world` keys need a new build (`/twilight convert`); `proxy` keys and
`geyser.send-pack-to-bedrock` take effect on restart.

## twilight-proxy

Bedrock loads resource packs once, when it connects. twilight-proxy therefore registers, for each
Bedrock session, the pack of the server the player is about to join (through Geyser's
`SessionLoadResourcePacksEvent`). When the player later moves to a server with another pack, the
client is transferred back to Geyser, loads that pack and is sent to the server it asked for. The
reconnect takes about five seconds when the client has the pack cached; otherwise Bedrock asks once
to download it. Servers with the same pack never cause a reconnect.

### Configuration

`plugins/twilight-proxy/config.yml`:

```yaml
packs:
  default: auto          # servers that are not listed
  server:
    lobby: auto                                  # pack built by Twilight on that backend
    smp: https://example.com/packs/smp.mcpack    # direct download link (cached, refreshed)
    survival: survival.zip                       # file in plugins/twilight-proxy/packs/
    hub: none                                    # no per-server pack
transfer-on-switch: true
transfer-address: ""     # empty = the address the player joined with
transfer-port: 0         # 0 = the port the player joined with
initial-server: ""       # empty = the proxy's first server
secret: ""               # empty = Velocity forwarding secret or BungeeGuard token
max-pack-size-mb: 256
download-timeout-seconds: 60
url-refresh-minutes: 60  # 0 = only at start and reload
```

| Key | Meaning |
|---|---|
| `packs.default` | Source for servers without an entry |
| `packs.server.<name>` | `auto`, `none`, a file name in `packs/`, or an `http(s)` link to a `.zip`/`.mcpack` |
| `transfer-on-switch` | Reconnect Bedrock players whose next server needs another pack |
| `transfer-address`, `transfer-port` | Where transferred players reconnect (useful behind a load balancer) |
| `initial-server` | Server whose pack a new session loads |
| `secret` | Shared secret for `auto` packs |
| `max-pack-size-mb` | Largest accepted pack (1-2048) |
| `download-timeout-seconds` | Timeout of a download (5-600) |
| `url-refresh-minutes` | How often links are checked for a new version with their ETag (0-10080) |

Every pack is checked before use: it must be a ZIP below the size limit with `manifest.json` at its
root and no entry that could escape a folder. A pack that fails the check, a failed download or an
incomplete transfer keeps the previous pack.

### Sources

- **auto**: Twilight on the backend announces its exported pack when a player joins and after each
  build. The proxy requests it if its copy differs and stores it in `cache/<server>.mcpack`. A 2.2 MiB
  pack arrives in about four seconds.
- **file**: read from `plugins/twilight-proxy/packs/` at start and reload.
- **link**: downloaded in the background over HTTP(S) to `cache/link-<hash>.mcpack`; redirects to
  plain HTTP are not followed.

## Files and folders

### Backend: `plugins/Twilight/`

| Path | Content |
|---|---|
| `config.yml` | Settings ([reference](#configuration-reference-twilight)) |
| `build/current/pack.zip` | The last built Bedrock pack |
| `build/current/custom_mappings/` | Geyser item and block mappings of that build |
| `build/current/build-report.json` | Counts, problems and notices of the build |
| `export/Twilight.mcpack` | The last successful pack, replaced atomically; for proxies and other plugins |
| `export/custom_mappings/twilight_*.json` | Geyser mappings matching the exported pack |
| `backups/geyser/<time>/` | Geyser snapshots for `/twilight rollback` |
| `cache/vanilla/<version>/` | Hash-verified Mojang client assets |
| `logs/<operation>-log-<time>.txt` | One log per operation (sources, fingerprint, results) |
| `reports/content-report.json` | The last scan's discovery report |
| `deployment.properties` | Files Twilight owns in Geyser's folder, with hashes |

In Geyser's folder Twilight owns `packs/twilight.zip`, `custom_mappings/twilight_*.json` and
`locales/overrides/`; nothing else is touched.

### Pack entries (inside `pack.zip`)

| Path | Content |
|---|---|
| `manifest.json` | Pack header with a stable UUID and a version derived from the content |
| `attachables/`, `models/entity/`, `animations/`, `render_controllers/`, `textures/` | Converted items and models |
| `font/glyph_XX.png` | Glyph pages for custom font images and their aliases |
| `ui/chest_screen.json`, `ui/hud_screen.json`, `ui/ui_common.json` | Chest layout and text layer labels |
| `biomes/<name>.client_biome.json`, `fogs/twilight_<name>.json` | Redefined Bedrock biomes for custom biome looks |
| `materials/ui3D.material` | Hidden name tag box (`ui.nametag-background`) |
| `texts/*.lang` | Pack translations for Bedrock UI keys |
| `sounds/sound_definitions.json`, `sounds/` | Converted sounds |
| `twilight/*.json` | Tables for Twilight's runtime bridges (text layout, biome slots, display variants); Bedrock ignores them |

### Proxy: `plugins/twilight-proxy/`

| Path | Content |
|---|---|
| `config.yml` | Settings ([reference](#twilight-proxy)) |
| `packs/` | Pack files named in `config.yml` |
| `cache/<server>.mcpack` | Packs received from Twilight on the backends |
| `cache/link-<hash>.mcpack` (+ `.etag`) | Downloaded packs |

## Developer API

### Twilight (backend)

`com.siberanka.twilight.api.TwilightApi` is registered in Bukkit's services manager:

```java
TwilightApi twilight = Bukkit.getServicesManager().load(TwilightApi.class);
if (twilight != null && !twilight.isOperationRunning()) twilight.requestConvert();
```

| Method | Description |
|---|---|
| `boolean isOperationRunning()` | Whether a scan, build or deployment is running |
| `boolean requestScan()` | Start a scan; false while another operation runs |
| `boolean requestConvert()` | Start a build (scan, build, export, deploy) |
| `boolean requestDeploy()` | Deploy the last build again |
| `Optional<ContentReport> lastContentReport()` | The last scan's report |
| `Path dataDirectory()` | `plugins/Twilight`; the exported pack is `export/Twilight.mcpack` |

Events (package `com.siberanka.twilight.api.event`, fired on the server thread):

| Event | Data |
|---|---|
| `TwilightScanCompleteEvent` | `report()`: sources, item definitions, models, biomes |
| `TwilightBuildCompleteEvent` | `result()`: converted items, glyphs, sounds, problems, pack SHA-256 |
| `TwilightDeployCompleteEvent` | `result()`: deployed files, snapshot, whether a restart is required |
| `TwilightOperationFailedEvent` | `operation()`, `logFile()`, `failure()` |

### twilight-proxy

`com.siberanka.twilight.proxy.api.TwilightProxyApi`, available after twilight-proxy is enabled
(add it as a soft dependency):

```java
TwilightProxyApi packs = TwilightProxyApi.get();
packs.pack("survival").ifPresent(path -> { /* read-only .mcpack */ });
```

| Method | Description |
|---|---|
| `Optional<Path> pack(String server)` | The checked pack of a server |
| `Optional<String> packSha256(String server)` | Its SHA-256 (hex) |
| `boolean reload()` | Reload configuration and packs |

## Plugin-message protocol

Channel `twilight:proxy`. All values are big-endian; every message ends with an HMAC-SHA256 tag.

| Field | Size |
|---|---|
| Magic `TW` | 2 bytes |
| Version (1) | 1 byte |
| Type: 1 announce, 2 request, 3 chunk | 1 byte |
| Body | see below |
| HMAC-SHA256 over everything before it | 32 bytes |

| Type | Direction | Body |
|---|---|---|
| Announce | backend → proxy | time (8), pack SHA-256 (32), size (8) |
| Request | proxy → backend | time (8), random nonce (16), wanted SHA-256 (32) |
| Chunk | backend → proxy | request nonce (16), index (4), total (4), length (2), data (up to 30000) |

The key is `HMAC-SHA256(secret, "twilight-proxy-pack-v1")`. Times must be within 120 seconds of
the receiver's clock; messages larger than 30128 bytes are dropped unread.

## Security model

- **Secret first.** Without a shared secret twilight-proxy does not request packs and Twilight
  neither announces nor answers anything on the channel.
- **Unforgeable, unreadable by clients.** The proxy consumes every message on `twilight:proxy` in
  both directions: clients never receive pack data and cannot send messages to a backend. Messages
  carry an HMAC tag, a timestamp and (requests) a nonce that Twilight accepts only once, so a client
  connected straight to a backend cannot forge or replay a request.
- **Bounded work.** Twilight serves one transfer at a time, at most two chunks per tick, with a
  30-second pause between complete transfers, and stops when the player leaves or the pack changes.
  The proxy accepts a transfer only for its own nonce, in order, of the announced size and SHA-256,
  runs at most four at once, drops idle ones after 30 seconds and deletes partial files.
- **Checked packs.** Size limit, ZIP structure, root `manifest.json` and safe entry names are checked
  before Geyser sees a pack. Configured file names cannot leave `packs/`; links must be http(s)
  without credentials.
- **Bounded transfers per player.** A Bedrock player is transferred at most four times in five
  minutes; a pending destination expires after ten minutes.
- **Text layout limits.** Text longer than 16384 characters or a move wider than 2^20 units is not
  laid out (it is sent unchanged), so crafted text cannot make the layout or its output grow without
  bound.
- **Builds.** Source sizes and archive entry counts are limited, symbolic links are refused, vanilla
  downloads are hash-verified, and deployment only replaces files Twilight owns.

## Troubleshooting

| Symptom | Check |
|---|---|
| Bedrock players get no pack | `/twilight status`; `plugins/Twilight/logs/`; `geyser.send-pack-to-bedrock` |
| "Restart the server to activate changed Geyser item mappings" | Geyser registers items at startup; restart once |
| A build is not published | `build/current/build-report.json` → `problems`; strict builds keep the last good pack |
| `auto` packs never arrive on the proxy | `/twilightproxy` shows "auto packs off": set the same `secret` on both sides or use modern forwarding |
| Bedrock players reconnect on every server switch | Expected when servers use different packs; same packs never reconnect |
| Transferred players end up on the wrong server | `transfer-address`/`transfer-port` must reach the same proxy |
| Custom biomes look like vanilla ones | More than 25 distinct looks, or `world.bedrock-biome-matching: false` |

## Building from source

```text
./gradlew :twilight:build :twilight-proxy:build --offline --no-configuration-cache
```

Outputs: `twilight/build/libs/Twilight.jar` and `twilight-proxy/build/libs/TwilightProxy.jar`. The
plugin-message protocol lives in `protocol/` and is compiled into both. Licensed under
[LGPL-3.0-or-later](LICENSE.LESSER).
