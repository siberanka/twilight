/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bedrock pack of every backend server, from its configured source: Twilight on that server
 * (cached in {@code cache/}), a file in {@code packs/}, or a download link (cached, refreshed with
 * the server's ETag). Every pack is checked ({@link PackFiles#inspect}) before it is used; a failed
 * download or transfer keeps the previous pack.
 */
final class PackStore {
    private final Platform platform;
    private final Path packs;
    private final Path cache;
    private final Map<String, PackFiles.Pack> current = new ConcurrentHashMap<>();
    private final Set<String> downloading = ConcurrentHashMap.newKeySet();
    private final Set<String> known = ConcurrentHashMap.newKeySet();
    private volatile ProxyConfig config;
    private volatile HttpClient http;

    PackStore(Platform platform) {
        this.platform = platform;
        this.packs = platform.dataDirectory().resolve("packs");
        this.cache = platform.dataDirectory().resolve("cache");
    }

    /** Applies a configuration: loads files and caches now, downloads links in the background. */
    void reload(ProxyConfig config, Iterable<String> servers) throws IOException {
        this.config = config;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(Math.min(30, config.downloadTimeoutSeconds()))).build();
        Files.createDirectories(packs);
        Files.createDirectories(cache);
        current.clear();
        known.clear();
        for (String server : servers) known.add(server.toLowerCase(Locale.ROOT));
        known.addAll(config.servers().keySet());
        for (String server : known) load(server);
    }

    Optional<PackFiles.Pack> pack(String server) {
        return Optional.ofNullable(current.get(server.toLowerCase(Locale.ROOT)));
    }

    boolean auto(String server) {
        return config.source(server) instanceof ProxyConfig.PackSource.Auto;
    }

    /** Re-downloads every linked pack whose content changed (ETag), in the background. */
    void refreshLinks() {
        ProxyConfig snapshot = config;
        for (var entry : snapshot.servers().entrySet()) {
            if (entry.getValue() instanceof ProxyConfig.PackSource.Url url) download(entry.getKey(), url.uri());
        }
        if (snapshot.defaultSource() instanceof ProxyConfig.PackSource.Url url) download("*default", url.uri());
    }

    /** A pack Twilight on {@code server} sent; replaces the cached one when it checks out. */
    void acceptAuto(String server, Path received) throws IOException {
        PackFiles.Pack checked = PackFiles.inspect(received, config.maxPackBytes());
        Path target = cache.resolve(PackFiles.safeName(server) + ".mcpack");
        PackFiles.moveInto(received, target);
        current.put(server.toLowerCase(Locale.ROOT), new PackFiles.Pack(target, checked.sha256(), checked.size()));
        platform.info("Received the Bedrock pack of " + server + " from Twilight (" + checked.size() / 1024 + " KiB, "
                + checked.hex().substring(0, 12) + ").");
    }

    Path temporaryFile(String server) throws IOException {
        Files.createDirectories(cache);
        return Files.createTempFile(cache, PackFiles.safeName(server) + "-", ".part");
    }

    private void load(String server) {
        ProxyConfig.PackSource source = config.source(server);
        try {
            if (source instanceof ProxyConfig.PackSource.Auto) {
                Path cached = cache.resolve(PackFiles.safeName(server) + ".mcpack");
                if (Files.isRegularFile(cached)) current.put(server, PackFiles.inspect(cached, config.maxPackBytes()));
            } else if (source instanceof ProxyConfig.PackSource.File file) {
                Path path = packs.resolve(file.name()).normalize();
                if (!path.startsWith(packs)) throw new IOException("pack file outside packs/");
                current.put(server, PackFiles.inspect(path, config.maxPackBytes()));
            } else if (source instanceof ProxyConfig.PackSource.Url url) {
                Path cached = linkCache(url.uri());
                if (Files.isRegularFile(cached)) current.put(server, PackFiles.inspect(cached, config.maxPackBytes()));
                download(server, url.uri());
            }
        } catch (IOException failure) {
            platform.warn("The Bedrock pack of " + server + " is unavailable: " + failure.getMessage(), null);
        }
    }

    private Path linkCache(URI uri) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(uri.toString().getBytes(StandardCharsets.UTF_8));
            return cache.resolve("link-" + HexFormat.of().formatHex(hash, 0, 12) + ".mcpack");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void download(String server, URI uri) {
        String key = uri.toString();
        if (!downloading.add(key)) return;
        ProxyConfig snapshot = config;
        HttpClient client = http;
        platform.async(() -> {
            Path target = linkCache(uri);
            Path etagFile = target.resolveSibling(target.getFileName() + ".etag");
            Path partial = null;
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder(uri).GET()
                        .timeout(Duration.ofSeconds(snapshot.downloadTimeoutSeconds()))
                        .header("User-Agent", "twilight-proxy");
                if (Files.isRegularFile(target) && Files.isRegularFile(etagFile) && Files.size(etagFile) < 512) {
                    request.header("If-None-Match", Files.readString(etagFile).strip());
                }
                HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    if (response.statusCode() == 304) return;
                    if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
                    String scheme = response.uri().getScheme();
                    if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) throw new IOException("redirected off http(s)");
                    long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                    if (declared > snapshot.maxPackBytes()) throw new IOException("the pack is larger than max-pack-size-mb");
                    partial = temporaryFile(server);
                    long copied = 0;
                    byte[] buffer = new byte[65_536];
                    try (OutputStream out = Files.newOutputStream(partial)) {
                        for (int read; (read = body.read(buffer)) >= 0;) {
                            copied += read;
                            if (copied > snapshot.maxPackBytes()) throw new IOException("the pack is larger than max-pack-size-mb");
                            out.write(buffer, 0, read);
                        }
                    }
                    PackFiles.Pack checked = PackFiles.inspect(partial, snapshot.maxPackBytes());
                    PackFiles.moveInto(partial, target);
                    partial = null;
                    String etag = response.headers().firstValue("ETag").orElse("");
                    if (!etag.isEmpty() && etag.length() < 256) Files.writeString(etagFile, etag);
                    else Files.deleteIfExists(etagFile);
                    for (String name : known) {
                        if (snapshot.source(name) instanceof ProxyConfig.PackSource.Url other && other.uri().equals(uri)) {
                            current.put(name, new PackFiles.Pack(target, checked.sha256(), checked.size()));
                        }
                    }
                    platform.info("Downloaded the Bedrock pack " + uri.getHost() + uri.getPath() + " (" + checked.size() / 1024 + " KiB).");
                }
            } catch (IOException | RuntimeException failure) {
                platform.warn("Could not download the Bedrock pack " + uri.getHost() + uri.getPath() + ": " + failure.getMessage(), null);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                if (partial != null) try { Files.deleteIfExists(partial); } catch (IOException ignored) { }
                downloading.remove(key);
            }
        });
    }
}
