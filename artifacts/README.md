# Twilight 1.0.0-pre.15 prerelease build

> Türkçe: [aşağıda](#türkçe)

[Download Twilight.jar](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256) (backend servers)

[Download TwilightProxy.jar](TwilightProxy.jar?raw=true) | [SHA-256](TwilightProxy.jar.sha256) (Velocity and BungeeCord proxies, optional)

Built locally by siberanka using Java 25.0.2 and Gradle 9.6.0, with
`:twilight:build :twilight-proxy:build --offline --no-daemon --no-configuration-cache`.
234 tests across 41 suites passed. No hosted CI was used.

The corresponding source is in this commit under `twilight/`, `twilight-proxy/` and `protocol/`, with build files
at the repository root. Licensed under [LGPL-3.0-or-later](../LICENSE.LESSER);
the accompanying [GPL text](../LICENSE) is also provided.

Read the [release notes](../RELEASE_NOTES.md), the [wiki](../WIKI.md) and the
[stacked images, nameplates and exact biomes](../docs/LAYERS_BIOMES_2026-10-04.md)
report before deployment. This prerelease fixes conversions on Paper 1.21 servers with Java 17+, makes
twilight-proxy find a Geyser that loads after it, delivers every backend's custom items to the proxy's
Geyser and finds login servers on its own ([field report](../docs/FIELD_REPORT_2026-10-09.md)); restart
Geyser once after updating. 1.0.0-pre.14 added an update check: both plugins look for a newer release on
GitHub (GitLab when GitHub cannot be reached) and tell the console and players with the update permission;
`update-check.enabled: false` turns it off ([wiki](../WIKI.md#updates)). 1.0.0-pre.13 made twilight-proxy's
pack reconnects work with LeaderOS Auth Plus, BungeeGuard, Floodgate and Velocity modern forwarding
([login plugin test](../docs/PROXY_AUTH_2026-10-09.md)); 1.0.0-pre.12 covered other login plugins, protections and large packs
([reconnect test](../docs/PROXY_RECONNECT_2026-10-08.md)).
1.0.0-pre.11 added the pack host: Bedrock players download the packs over HTTP from the server that
runs Geyser, through links that only work for their own session ([wiki](../WIKI.md#pack-hosting)).
1.0.0-pre.10 draws boss bars whose sprites a pack redraws with those
sprites, shows long boss bar names whole and fits CustomNameplates' default boss bar into Bedrock's
name limit ([boss bar and model review](../docs/BOSSBARS_MODELS_2026-10-05.md)). Since 1.0.0-pre.9,
releases include twilight-proxy, per-server Bedrock packs for
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

---

## Türkçe

### Twilight 1.0.0-pre.15 ön sürüm derlemesi

[Twilight.jar indir](Twilight.jar?raw=true) | [SHA-256](Twilight.jar.sha256) (arka uç sunucuları)

[TwilightProxy.jar indir](TwilightProxy.jar?raw=true) | [SHA-256](TwilightProxy.jar.sha256) (Velocity ve BungeeCord proxy'leri, isteğe bağlı)

siberanka tarafından Java 25.0.2 ve Gradle 9.6.0 ile yerel olarak
`:twilight:build :twilight-proxy:build --offline --no-daemon --no-configuration-cache`
komutuyla derlendi. 41 paketteki 234 test geçti. Barındırılan CI kullanılmadı.

İlgili kaynak kod bu commit içinde `twilight/`, `twilight-proxy/` ve `protocol/` altında, derleme
dosyaları depo kökündedir. [LGPL-3.0-or-later](../LICENSE.LESSER) ile lisanslanmıştır;
eşlik eden [GPL metni](../LICENSE) de sağlanır.

Dağıtımdan önce [sürüm notlarını](../RELEASE_NOTES.md), [wiki'yi](../WIKI.md) ve
[üst üste görseller, isim plakaları ve birebir biyomlar](../docs/LAYERS_BIOMES_2026-10-04.md)
raporunu okuyun. Bu ön sürüm Java 17+ ile çalışan Paper 1.21 sunucularında dönüştürmeleri düzeltir,
twilight-proxy'nin kendisinden sonra yüklenen bir Geyser'ı bulmasını sağlar, her arka ucun özel eşyalarını
proxy'deki Geyser'a ulaştırır ve giriş sunucularını kendisi bulur
([saha raporu](../docs/FIELD_REPORT_2026-10-09.md)); güncellemeden sonra Geyser'ı bir kez yeniden başlatın.
1.0.0-pre.14 bir güncelleme denetimi ekledi: iki eklenti de GitHub'da (GitHub'a ulaşılamadığında
GitLab'da) daha yeni bir sürüm arar ve konsola ve güncelleme iznine sahip oyunculara bildirir;
`update-check.enabled: false` kapatır ([wiki](../WIKI.md#güncellemeler)). 1.0.0-pre.13 twilight-proxy'nin paket
yeniden bağlanmalarını LeaderOS Auth Plus, BungeeGuard, Floodgate ve Velocity modern yönlendirmesiyle çalışır
hâle getirdi ([giriş eklentisi testi](../docs/PROXY_AUTH_2026-10-09.md)); 1.0.0-pre.12 diğer giriş eklentilerini, korumaları
ve büyük paketleri kapsadı ([yeniden bağlanma testi](../docs/PROXY_RECONNECT_2026-10-08.md)).
1.0.0-pre.11 paket sunucusunu ekledi: Bedrock oyuncuları paketleri Geyser'ı çalıştıran sunucudan, yalnızca
kendi oturumları için çalışan bağlantılarla HTTP üzerinden indirir ([wiki](../WIKI.md#paket-sunucusu)).
1.0.0-pre.10, sprite'larını bir paketin yeniden çizdiği boss çubuklarını
bu sprite'larla çizer, uzun boss çubuğu adlarını tamamen gösterir ve CustomNameplates'in varsayılan boss çubuğunu Bedrock'un ad
sınırına sığdırır ([boss çubuğu ve model incelemesi](../docs/BOSSBARS_MODELS_2026-10-05.md)). 1.0.0-pre.9'dan
beri sürümler, Geyser'ı Velocity veya BungeeCord proxy'si üzerinde çalıştıran ağlar için sunucuya özel Bedrock
paketleri sunan twilight-proxy'yi içerir
([proxy testi](../docs/PROXY_2026-10-05.md)). Java'nın önceki öğelerin üzerine geri çizdiği yazı ve
görselleri (üst üste menü görselleri, CustomNameplates arka planları) katman başına bir Bedrock
etiketiyle çizer, özel ve RealisticSeasons biyomlarını birebir renkleriyle gösterir, isim
plakalarına Java'nın isim kurallarını uygular, her sağlayıcının kaynağını ve Bedrock paket
gönderimini `config.yml` içinde seçtirir ve her derlemeyi `plugins/Twilight/export` altına
aktarır. Yedi üretim sunucusunun tam derlemeleri varsayılan ayarlarla katı yayımlamayı geçer.

[Arayüz kampanyası](../docs/UI_CAMPAIGN_2026-10-04.md) (104 gerçek menü), her Bedrock yazı
yüzeyindeki Java yazı yerleşimi ([yazı yüzeyi incelemesi](../docs/TEXT_SURFACES_2026-10-04.md)),
sandık başlığı yerleşimi ([yazı yerleşimi incelemesi](../docs/TEXT_LAYOUT_2026-10-03.md)) ve
konteyner yerleşimi ([konteyner yerleşimi incelemesi](../docs/CONTAINER_LAYOUT_2026-10-03.md))
üzerine kuruludur; canlı görüntü köprüsünü, [bulut çapası düzeltmelerini](../docs/CLOUD_ANCHORS_2026-09-29.md)
ve [Java uyumlu bulut binme yüksekliğini](../docs/DISPLAY_SEATS_2026-09-29.md) içerir.

Tam animasyon, birinci şahıs, arayüz ve varlık eşdeğerliği henüz tamamlanmadı. Eşya adları ve
açıklamaları ham font karakterlerini korur, moblara binen yazı gösterimleri yaklaşık 0,4 blok
aşağıda durur, hex renkli yazılar Bedrock'un 28 rengini kullanır, boşluksuz olarak yazının hemen
ardından gelen bir glif bir birim farklı durur ve yüksek çözünürlüklü glif örneklemesi farklıdır.

[Genişletilmiş içerik denetimleri](../docs/BROAD_CONTENT_2026-09-29.md),
[yeni örnek regresyonu](../docs/COMPOSITE_MODELS_2026-09-30.md) ve
[font ölçüleri regresyonu](../docs/FONT_METRICS_2026-10-01.md) önceki eşya, glif ve menü
sonuçlarını belgeler.
