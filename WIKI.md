# Twilight wiki

> Türkçe: [aşağıda](#türkçe)

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
6. [twilight-proxy](#twilight-proxy-1)
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
4. Each Bedrock player now loads the pack of the server they join; see [twilight-proxy](#twilight-proxy-1).

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
| `textures/ui/twilight_boss_bar/` | Boss bar sprites a pack redraws, drawn by the HUD like Java |
| `texts/*.lang` | Pack translations for Bedrock UI keys |
| `sounds/sound_definitions.json`, `sounds/` | Converted sounds |
| `twilight/*.json` | Tables for Twilight's runtime bridges (text layout, biome slots, display variants); Bedrock ignores them |

### Proxy: `plugins/twilight-proxy/`

| Path | Content |
|---|---|
| `config.yml` | Settings ([reference](#twilight-proxy-1)) |
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

---

## Türkçe

### Twilight wiki

Twilight bir Java sunucusunun özel içeriğini (ItemsAdder, Nexo, CraftEngine, Oraxen, BetterModel,
ModelEngine, CustomNameplates, BetterHUD, datapack'ler ve RealisticSeasons) bir Bedrock kaynak paketine ve
Geyser eşlemelerine dönüştürür ve Bedrock oyuncularının onu Java oyuncularının gördüğü gibi görmesini sağlar.
Proje iki eklentiyle gelir:

| Eklenti | Çalıştığı yer | Amaç |
|---|---|---|
| `Twilight.jar` | Paper, Folia, Spigot 1.21.4+ (arka uç sunucular) | Bedrock paketini derler, Geyser'a dağıtır, çalışma zamanında yazıyı, biyomları ve adları yerleştirir |
| `TwilightProxy.jar` (twilight-proxy) | Velocity, BungeeCord/Waterfall (proxy'ler) | Proxy'deki Geyser üzerinden her arka uç sunucuya kendi Bedrock paketini verir |

Bu sayfa yöneticiler ve eklenti geliştiricileri için başvuru kaynağıdır. Ölçümler ve ekran görüntüleri
[raporlardadır](docs/); özellik başına sınırlar [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) içindedir.

#### İçindekiler

1. [Gereksinimler](#gereksinimler)
2. [Kurulum](#kurulum)
3. [Bir derleme nasıl çalışır](#bir-derleme-nasıl-çalışır)
4. [Komutlar ve izinler](#komutlar-ve-izinler)
5. [Yapılandırma başvurusu: Twilight](#yapılandırma-başvurusu-twilight)
6. [twilight-proxy](#twilight-proxy-4)
7. [Dosyalar ve klasörler](#dosyalar-ve-klasörler)
8. [Geliştirici API'si](#geliştirici-apisi)
9. [Eklenti mesajı protokolü](#eklenti-mesajı-protokolü)
10. [Güvenlik modeli](#güvenlik-modeli)
11. [Sorun giderme](#sorun-giderme)
12. [Kaynaktan derleme](#kaynaktan-derleme)

#### Gereksinimler

| Bileşen | Sürüm |
|---|---|
| Java | 21 veya üstü (Twilight Java 25 ile, release 21 olarak derlenir) |
| Arka uç sunucu | Paper, Folia veya Spigot 1.21.4 veya üstü; önce en yeni sürümler (Paper 26.2 üzerinde test edildi) |
| Geyser | Özel içeriği açık 2.x; çalışma zamanı köprüleri Geyser 2.11.3'e karşı doğrulandı |
| Bedrock istemcileri | Güncel sürüm (1.26.5203 ile test edildi); eskileri Geyser'ın desteklediği kadar |
| Proxy (isteğe bağlı) | Proxy'de Geyser bulunan Velocity 3.x/4.x veya BungeeCord/Waterfall (Velocity 4.2.0 ve BungeeCord 26.1 üzerinde test edildi) |

#### Kurulum

##### Tek sunucu

1. `Twilight.jar` dosyasını Geyser-Spigot'un yanına, `plugins/` içine koyun ve sunucuyu başlatın.
2. Hiçbir şeyin yapılandırılması gerekmez. Sunucu ve içerik eklentileri yüklendikten sonra Twilight
   paketlerini bulur, Bedrock paketini derler, Geyser'a dağıtır ve Geyser'ı yeniden yükler.
3. Bedrock oyuncuları paketi bir sonraki katılışlarında alır. Eşya eşlemeleri değiştiyse konsol bir kez
   yeniden başlatma ister, çünkü Geyser eşyaları yalnızca açılışta kaydeder.

##### Proxy'li bir ağ

1. Özel içeriği olan her arka uca `Twilight.jar` kurun; arka uçlarda Geyser-Spigot gerekmez.
2. Proxy'ye Geyser ve `TwilightProxy.jar` kurun.
3. Oyuncu verisini Velocity'nin `modern` (veya `bungeeguard`) yönlendirmesiyle ya da BungeeGuard ile iletin.
   İki eklenti de bu gizli anahtarı kendiliğinden paylaşır. Bu yoksa twilight-proxy'nin `config.yml`
   dosyasında `secret` ve her arka ucun Twilight `config.yml` dosyasında `proxy.secret` için aynı değeri
   ayarlayın.
4. Artık her Bedrock oyuncusu katıldığı sunucunun paketini yükler; [twilight-proxy](#twilight-proxy-4)
   bölümüne bakın.

#### Bir derleme nasıl çalışır

1. **Keşif.** Twilight kurulu sağlayıcıların klasörlerini ve üretilmiş paketlerini okur. Bir sağlayıcının
   Java oyuncularına gönderdiği paket, boşlukları dolduran çalışma klasörlerinin önüne geçer; ayarları
   paketini göndermeyen veya eklentisi kurulu olmayan bir sağlayıcı yalnızca boşlukları doldurur. Dünya
   datapack'leri ve `sources.additional` eklenir. Her sağlayıcı `sources.providers` ile sınırlanabilir.
2. **Çalışma zamanı eşyaları.** Tarif sonuçları, çevrim içi envanterler ve sağlayıcıların eşya kayıtları
   genel API'ler üzerinden okunur; böylece yalnızca çalışma zamanında var olan eşyalar da dönüştürülür.
3. **Derleme.** Eşya modelleri, dokular, fontlar, sesler, çeviriler, biyom görünümleri ve arayüz dosyaları
   dönüştürülür. Java'nın kendisinin bozuk gösterdiği içerik (eksik bir doku veya model, ekran boyutunda bir
   kaplama glifi) Java'nın gösterdiği şekilde dönüştürülür ve bildirim olarak raporlanır. Güvenle temsil
   edilemeyen içerik bir sorundur; `generation.strict: true` ile sorunlu bir derleme, bir sunucunun ilk
   paketi dışında yayımlanmaz.
4. **Dışa aktarma ve dağıtım.** Paket ve Geyser eşlemeleri `plugins/Twilight/export/` dizinine yazılır ve
   Geyser'a işlemsel olarak dağıtılır (geri alma için anlık görüntülerle).
5. **Çalışma zamanı.** Oyuncular oynarken Twilight, Java yazısını Bedrock için yerleştirir (başlıklar,
   sohbet, aksiyon çubuğu, boss çubukları, skor tabloları, adlar, yazı görüntüleri), Java'nın ad kurallarını
   uygular, özel biyomları eşler ve biyom güncellemelerini iletir.

Derlemeler sağlayıcı yeniden yüklemelerinden, sağlayıcı paket komutlarından ve RealisticSeasons mevsim
değişikliklerinden sonra kendiliğinden yeniden çalışır. Değişmemiş girdiler bir parmak iziyle algılanır ve
atlanır.

#### Komutlar ve izinler

##### Twilight (arka uç)

| Komut | Açıklama |
|---|---|
| `/twilight status` | İşlem durumu, son tarama, girdi parmak izi, Geyser klasörü ve anlık görüntüler |
| `/twilight scan` | Derlemeden kaynakları keşfeder ve inceler |
| `/twilight convert` (takma ad `build`) | Tarar, derler, dışa aktarır ve dağıtır |
| `/twilight deploy` | Son derlemeyi Geyser'a yeniden dağıtır |
| `/twilight rollback [n]` | En yeni n'inci Geyser anlık görüntüsünü geri yükler (varsayılan 1) |
| `/twilight reload` | `config.yml` dosyasını yeniden yükler ve doğrular |

Takma ad: `/tw`. İzin: `twilight.admin` (varsayılan: operatörler). Uzun işlemler sunucu iş parçacığı
dışında çalışır; aynı anda yalnızca biri çalışır.

##### twilight-proxy

| Komut | Açıklama |
|---|---|
| `/twilightproxy` | Paylaşılan gizli anahtar durumu, Geyser varlığı ve her sunucunun paketi |
| `/twilightproxy reload` | `config.yml` dosyasını ve her paketi yeniden yükler |

Takma ad: `/twproxy`. İzin: `twilight.proxy.admin`.

#### Yapılandırma başvurusu: Twilight

`plugins/Twilight/config.yml`. Her anahtarın çalışan bir varsayılanı vardır; dosyanın yalnızca davranışı
değiştirmek için düzenlenmesi gerekir.

##### Üst düzey

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `vanilla-override` | `false` | Paketin Java paketlerinin değiştirdiği vanilla Bedrock varlıklarının (sesler, glifler) yerini almasına izin verir |

##### `generation`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `strict` | `true` | Sorunlu bir derleme yayımlanmaz (bir sunucunun ilk paketi yine yayımlanır) |
| `auto-build-on-startup` | `true` | Sunucu ve sağlayıcılar yüklendikten sonra derler |
| `sync-provider-changes` | `true` | Sağlayıcı yeniden yükleme/paket olaylarından ve komutlarından sonra yeniden derler |
| `startup-delay-ticks` | `40` | Otomatik derlemelerden önceki gecikme (1-72000) |
| `provider-command-delay-ticks` | `100` | Bir sağlayıcı komutundan sonra, çıktısının yazılması için gecikme |
| `maximum-source-bytes` | `1073741824` | Bir derlemede okunan tüm kaynak baytlarının üst sınırı |
| `maximum-archive-entries` | `100000` | Kaynak arşivi başına girdi üst sınırı |
| `download-vanilla-assets` | `true` | Bir derleme gerektirdiğinde sürümün istemci varlıklarını Mojang'dan indirir (karma doğrulamalı) |

##### `sources`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `auto-discover` | `true` | Sağlayıcıları ve dünya datapack'lerini kendiliğinden bulur |
| `providers.<ad>` | `auto` | Sağlayıcı başına (`itemsadder`, `nexo`, `craftengine`, `oraxen`, `bettermodel`, `modelengine`, `customnameplates`, `betterhud`, `realisticseasons`): `auto` / `true`, `generated` (yalnızca üretilmiş paket), `contents` (yalnızca çalışma klasörleri), `off` / `false` |
| `datapacks` | `true` | Dünyaların datapack'lerini okur |
| `additional` | `[]` | Ek paketler veya datapack'ler (klasörler veya ZIP'ler), sunucu klasörüne göreli |

##### `geyser`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `directory` | `auto` | Geyser'ın veri klasörü; `auto` `plugins/Geyser-*` klasörünü bulur |
| `deploy-after-build` | `true` | Her başarılı derlemeyi dağıtır |
| `reload-after-deploy` | `true` | Yeniden başlatma gerekmediğinde dağıtımdan sonra `geyser reload` çalıştırır |
| `backups-to-keep` | `3` | `/twilight rollback` için saklanan Geyser anlık görüntüleri (1-20) |
| `send-pack-to-bedrock` | `true` | Paketi Geyser'ın göndermesine izin verir; bir proxy veya başka bir eklenti `export/Twilight.mcpack` dosyasını gönderiyorsa `false` |

##### `ui`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `java-container-layout` | `true` | Bedrock sandık ekranlarında Java'nın sandık yerleşimi ve başlık konumu |
| `java-text-layout` | `true` | Yazıyı Java font ölçüleriyle yerleştirir (boşluklar, negatif boşluklar, görseller) |
| `java-text-surfaces` | `true` | Yerleşimi sohbete, aksiyon çubuğuna, başlıklara, boss çubuklarına, skor tablolarına, adlara ve yazı görüntülerine uygular |
| `java-glyph-tint` | `true` | Renksiz konteyner başlıkları için Java'nın çizdiği gibi koyulaştırılmış görsel kopyaları |
| `java-text-layers` | `true` | Öncekilerin üzerine geri çizilen yazı ve görseller katman başına bir etiket alır (sandık başlıkları, aksiyon çubuğu, boss çubukları) |
| `nametag-background` | `auto` | Bedrock'un ad etiketi kutusu: `auto` (CustomNameplates'in ad etiketleri açıkken gizli), `hidden`, `bedrock` |
| `java-translations` | `true` | Bedrock oyuncuları için kaynak paketlerinin çevirilerini kullanır |

##### `world`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `bedrock-biome-matching` | `true` | Yeniden tanımlanmış 25 Bedrock biyomunda özel biyomlar için birebir renkler ve iklim, aksi hâlde en yakın vanilla biyom ve canlı biyom güncellemeleri |

##### `proxy`

| Anahtar | Varsayılan | Anlamı |
|---|---|---|
| `share-pack` | `true` | Dışa aktarılan paketi imzalı eklenti mesajlarıyla twilight-proxy'ye sunar |
| `secret` | `""` | twilight-proxy ile paylaşılan gizli anahtar (16+ karakter); boşsa Paper'ın Velocity gizli anahtarını veya BungeeGuard belirteçlerini kullanır |

`ui` ve `world` anahtarlarındaki değişiklikler yeni bir derleme gerektirir (`/twilight convert`); `proxy`
anahtarları ve `geyser.send-pack-to-bedrock` yeniden başlatmada etkinleşir.

#### twilight-proxy

Bedrock kaynak paketlerini bir kez, bağlanırken yükler. twilight-proxy bu yüzden her Bedrock oturumu için
oyuncunun katılmak üzere olduğu sunucunun paketini kaydeder (Geyser'ın `SessionLoadResourcePacksEvent`
olayıyla). Oyuncu daha sonra başka paketli bir sunucuya geçtiğinde istemci Geyser'a geri aktarılır, o paketi
yükler ve istediği sunucuya gönderilir. İstemci paketi önbellekte tutuyorsa yeniden bağlanma yaklaşık beş
saniye sürer; aksi hâlde Bedrock indirmek için bir kez sorar. Aynı paketi kullanan sunucular asla yeniden
bağlanmaya yol açmaz.

##### Yapılandırma

`plugins/twilight-proxy/config.yml`:

```yaml
packs:
  default: auto          # listelenmeyen sunucular
  server:
    lobby: auto                                  # o arka uçta Twilight'ın derlediği paket
    smp: https://example.com/packs/smp.mcpack    # doğrudan indirme bağlantısı (önbelleğe alınır, yenilenir)
    survival: survival.zip                       # plugins/twilight-proxy/packs/ içindeki dosya
    hub: none                                    # sunucuya özel paket yok
transfer-on-switch: true
transfer-address: ""     # boş = oyuncunun katıldığı adres
transfer-port: 0         # 0 = oyuncunun katıldığı port
initial-server: ""       # boş = proxy'nin ilk sunucusu
secret: ""               # boş = Velocity yönlendirme gizli anahtarı veya BungeeGuard belirteci
max-pack-size-mb: 256
download-timeout-seconds: 60
url-refresh-minutes: 60  # 0 = yalnızca açılışta ve yeniden yüklemede
```

| Anahtar | Anlamı |
|---|---|
| `packs.default` | Girdisi olmayan sunucular için kaynak |
| `packs.server.<ad>` | `auto`, `none`, `packs/` içinde bir dosya adı veya bir `.zip`/`.mcpack` dosyasına `http(s)` bağlantısı |
| `transfer-on-switch` | Sonraki sunucusu başka bir paket gerektiren Bedrock oyuncularını yeniden bağlar |
| `transfer-address`, `transfer-port` | Aktarılan oyuncuların yeniden bağlandığı yer (yük dengeleyici arkasında yararlı) |
| `initial-server` | Yeni bir oturumun paketini yüklediği sunucu |
| `secret` | `auto` paketler için paylaşılan gizli anahtar |
| `max-pack-size-mb` | Kabul edilen en büyük paket (1-2048) |
| `download-timeout-seconds` | Bir indirmenin zaman aşımı (5-600) |
| `url-refresh-minutes` | Bağlantıların ETag ile yeni sürüm için ne sıklıkla denetlendiği (0-10080) |

Her paket kullanılmadan önce denetlenir: boyut sınırının altında, kökünde `manifest.json` bulunan ve hiçbir
girdisi klasör dışına çıkamayan bir ZIP olmalıdır. Denetimi geçemeyen bir paket, başarısız bir indirme veya
eksik bir aktarım önceki paketi korur.

##### Kaynaklar

- **auto**: Arka uçtaki Twilight bir oyuncu katıldığında ve her derlemeden sonra dışa aktardığı paketi
  duyurur. Proxy kendi kopyası farklıysa paketi ister ve `cache/<sunucu>.mcpack` olarak saklar. 2,2 MiB'lik bir
  paket yaklaşık dört saniyede ulaşır.
- **dosya**: açılışta ve yeniden yüklemede `plugins/twilight-proxy/packs/` dizininden okunur.
- **bağlantı**: arka planda HTTP(S) üzerinden `cache/link-<karma>.mcpack` dosyasına indirilir; düz HTTP'ye
  yönlendirmeler izlenmez.

#### Dosyalar ve klasörler

##### Arka uç: `plugins/Twilight/`

| Yol | İçerik |
|---|---|
| `config.yml` | Ayarlar ([başvuru](#yapılandırma-başvurusu-twilight)) |
| `build/current/pack.zip` | Son derlenen Bedrock paketi |
| `build/current/custom_mappings/` | O derlemenin Geyser eşya ve blok eşlemeleri |
| `build/current/build-report.json` | Derlemenin sayıları, sorunları ve bildirimleri |
| `export/Twilight.mcpack` | Son başarılı paket, atomik olarak değiştirilir; proxy'ler ve diğer eklentiler için |
| `export/custom_mappings/twilight_*.json` | Dışa aktarılan pakete uyan Geyser eşlemeleri |
| `backups/geyser/<zaman>/` | `/twilight rollback` için Geyser anlık görüntüleri |
| `cache/vanilla/<sürüm>/` | Karma doğrulamalı Mojang istemci varlıkları |
| `logs/<işlem>-log-<zaman>.txt` | İşlem başına bir günlük (kaynaklar, parmak izi, sonuçlar) |
| `reports/content-report.json` | Son taramanın keşif raporu |
| `deployment.properties` | Twilight'ın Geyser klasöründe sahip olduğu dosyalar, karmalarıyla |

Geyser klasöründe Twilight yalnızca `packs/twilight.zip`, `custom_mappings/twilight_*.json` ve
`locales/overrides/` öğelerinin sahibidir; başka hiçbir şeye dokunulmaz.

##### Paket girdileri (`pack.zip` içinde)

| Yol | İçerik |
|---|---|
| `manifest.json` | Kararlı bir UUID'ye ve içerikten türetilen bir sürüme sahip paket başlığı |
| `attachables/`, `models/entity/`, `animations/`, `render_controllers/`, `textures/` | Dönüştürülmüş eşyalar ve modeller |
| `font/glyph_XX.png` | Özel font görselleri ve takma adları için glif sayfaları |
| `ui/chest_screen.json`, `ui/hud_screen.json`, `ui/ui_common.json` | Sandık yerleşimi ve yazı katmanı etiketleri |
| `biomes/<ad>.client_biome.json`, `fogs/twilight_<ad>.json` | Özel biyom görünümleri için yeniden tanımlanmış Bedrock biyomları |
| `materials/ui3D.material` | Gizli ad etiketi kutusu (`ui.nametag-background`) |
| `textures/ui/twilight_boss_bar/` | Bir paketin yeniden çizdiği boss çubuğu sprite'ları; HUD bunları Java gibi çizer |
| `texts/*.lang` | Bedrock arayüz anahtarları için paket çevirileri |
| `sounds/sound_definitions.json`, `sounds/` | Dönüştürülmüş sesler |
| `twilight/*.json` | Twilight'ın çalışma zamanı köprüleri için tablolar (yazı yerleşimi, biyom yuvaları, görüntü çeşitleri); Bedrock bunları yok sayar |

##### Proxy: `plugins/twilight-proxy/`

| Yol | İçerik |
|---|---|
| `config.yml` | Ayarlar ([başvuru](#twilight-proxy-4)) |
| `packs/` | `config.yml` içinde adı geçen paket dosyaları |
| `cache/<sunucu>.mcpack` | Arka uçlardaki Twilight'tan alınan paketler |
| `cache/link-<karma>.mcpack` (+ `.etag`) | İndirilen paketler |

#### Geliştirici API'si

##### Twilight (arka uç)

`com.siberanka.twilight.api.TwilightApi` Bukkit'in hizmet yöneticisine kaydedilir:

```java
TwilightApi twilight = Bukkit.getServicesManager().load(TwilightApi.class);
if (twilight != null && !twilight.isOperationRunning()) twilight.requestConvert();
```

| Yöntem | Açıklama |
|---|---|
| `boolean isOperationRunning()` | Bir tarama, derleme veya dağıtımın çalışıp çalışmadığı |
| `boolean requestScan()` | Bir tarama başlatır; başka bir işlem çalışırken false |
| `boolean requestConvert()` | Bir derleme başlatır (tarama, derleme, dışa aktarma, dağıtım) |
| `boolean requestDeploy()` | Son derlemeyi yeniden dağıtır |
| `Optional<ContentReport> lastContentReport()` | Son taramanın raporu |
| `Path dataDirectory()` | `plugins/Twilight`; dışa aktarılan paket `export/Twilight.mcpack` |

Olaylar (`com.siberanka.twilight.api.event` paketi, sunucu iş parçacığında tetiklenir):

| Olay | Veri |
|---|---|
| `TwilightScanCompleteEvent` | `report()`: kaynaklar, eşya tanımları, modeller, biyomlar |
| `TwilightBuildCompleteEvent` | `result()`: dönüştürülen eşyalar, glifler, sesler, sorunlar, paket SHA-256 |
| `TwilightDeployCompleteEvent` | `result()`: dağıtılan dosyalar, anlık görüntü, yeniden başlatma gerekip gerekmediği |
| `TwilightOperationFailedEvent` | `operation()`, `logFile()`, `failure()` |

##### twilight-proxy

`com.siberanka.twilight.proxy.api.TwilightProxyApi`, twilight-proxy etkinleştirildikten sonra kullanılabilir
(yumuşak bağımlılık olarak ekleyin):

```java
TwilightProxyApi packs = TwilightProxyApi.get();
packs.pack("survival").ifPresent(path -> { /* salt okunur .mcpack */ });
```

| Yöntem | Açıklama |
|---|---|
| `Optional<Path> pack(String server)` | Bir sunucunun denetlenmiş paketi |
| `Optional<String> packSha256(String server)` | Onun SHA-256 değeri (hex) |
| `boolean reload()` | Yapılandırmayı ve paketleri yeniden yükler |

#### Eklenti mesajı protokolü

Kanal `twilight:proxy`. Tüm değerler big-endian'dır; her mesaj bir HMAC-SHA256 etiketiyle biter.

| Alan | Boyut |
|---|---|
| Sihirli değer `TW` | 2 bayt |
| Sürüm (1) | 1 bayt |
| Tür: 1 duyuru, 2 istek, 3 parça | 1 bayt |
| Gövde | aşağıya bakın |
| Önceki her şey üzerinden HMAC-SHA256 | 32 bayt |

| Tür | Yön | Gövde |
|---|---|---|
| Duyuru | arka uç → proxy | zaman (8), paket SHA-256 (32), boyut (8) |
| İstek | proxy → arka uç | zaman (8), rastgele nonce (16), istenen SHA-256 (32) |
| Parça | arka uç → proxy | istek nonce'u (16), sıra (4), toplam (4), uzunluk (2), veri (en fazla 30000) |

Anahtar `HMAC-SHA256(secret, "twilight-proxy-pack-v1")` değeridir. Zamanlar alıcının saatine göre 120 saniye
içinde olmalıdır; 30128 bayttan büyük mesajlar okunmadan atılır.

#### Güvenlik modeli

- **Önce gizli anahtar.** Paylaşılan bir gizli anahtar olmadan twilight-proxy paket istemez ve Twilight
  kanalda hiçbir şey duyurmaz veya yanıtlamaz.
- **Sahtelenemez, istemcilerce okunamaz.** Proxy `twilight:proxy` üzerindeki her mesajı iki yönde de
  tüketir: istemciler asla paket verisi almaz ve bir arka uca mesaj gönderemez. Mesajlar bir HMAC etiketi,
  bir zaman damgası ve (isteklerde) Twilight'ın yalnızca bir kez kabul ettiği bir nonce taşır; bu yüzden
  doğrudan bir arka uca bağlanan bir istemci bir isteği sahteleyemez veya yeniden oynatamaz.
- **Sınırlı iş.** Twilight aynı anda bir aktarıma hizmet eder, tick başına en fazla iki parça gönderir, tam
  aktarımlar arasında 30 saniye bekler ve oyuncu ayrıldığında veya paket değiştiğinde durur. Proxy bir
  aktarımı yalnızca kendi nonce'u için, sırayla, duyurulan boyut ve SHA-256 ile kabul eder, aynı anda en
  fazla dördünü yürütür, boşta kalanları 30 saniye sonra bırakır ve kısmi dosyaları siler.
- **Denetlenen paketler.** Boyut sınırı, ZIP yapısı, kökteki `manifest.json` ve güvenli girdi adları Geyser
  bir paketi görmeden önce denetlenir. Yapılandırılmış dosya adları `packs/` dışına çıkamaz; bağlantılar
  kimlik bilgisi içermeyen http(s) olmalıdır.
- **Oyuncu başına sınırlı aktarım.** Bir Bedrock oyuncusu beş dakikada en fazla dört kez aktarılır; bekleyen
  bir hedef on dakika sonra sona erer.
- **Yazı yerleşimi sınırları.** 16384 karakterden uzun yazı veya 2^20 birimden geniş bir kaydırma
  yerleştirilmez (değişmeden gönderilir); böylece özel hazırlanmış yazı yerleşimi veya çıktısını sınırsız
  büyütemez.
- **Derlemeler.** Kaynak boyutları ve arşiv girdi sayıları sınırlıdır, sembolik bağlantılar reddedilir,
  vanilla indirmeleri karma ile doğrulanır ve dağıtım yalnızca Twilight'ın sahip olduğu dosyaları değiştirir.

#### Sorun giderme

| Belirti | Denetim |
|---|---|
| Bedrock oyuncuları paket almıyor | `/twilight status`; `plugins/Twilight/logs/`; `geyser.send-pack-to-bedrock` |
| "Restart the server to activate changed Geyser item mappings" | Geyser eşyaları açılışta kaydeder; bir kez yeniden başlatın |
| Bir derleme yayımlanmıyor | `build/current/build-report.json` → `problems`; katı derlemeler son iyi paketi korur |
| `auto` paketler proxy'ye hiç ulaşmıyor | `/twilightproxy` "auto packs off" gösteriyor: iki tarafta aynı `secret` değerini ayarlayın veya modern yönlendirme kullanın |
| Bedrock oyuncuları her sunucu geçişinde yeniden bağlanıyor | Sunucular farklı paketler kullandığında beklenir; aynı paketler asla yeniden bağlanmaz |
| Aktarılan oyuncular yanlış sunucuya düşüyor | `transfer-address`/`transfer-port` aynı proxy'ye ulaşmalı |
| Özel biyomlar vanilla gibi görünüyor | 25'ten fazla farklı görünüm veya `world.bedrock-biome-matching: false` |

#### Kaynaktan derleme

```text
./gradlew :twilight:build :twilight-proxy:build --offline --no-configuration-cache
```

Çıktılar: `twilight/build/libs/Twilight.jar` ve `twilight-proxy/build/libs/TwilightProxy.jar`. Eklenti
mesajı protokolü `protocol/` içindedir ve ikisine de derlenir. [LGPL-3.0-or-later](LICENSE.LESSER) ile
lisanslanmıştır.
