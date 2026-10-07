/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import com.siberanka.twilight.protocol.PackChannel;
import com.siberanka.twilight.proxy.api.TwilightProxyApi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-server Bedrock packs on a proxy, shared by the Velocity and BungeeCord entry points.
 *
 * <p>Bedrock loads resource packs once, when it connects. A Bedrock session therefore gets the pack
 * of the server it is about to join; when the player later moves to a server with another pack,
 * the client is transferred back to Geyser, loads that server's pack and is sent to the server it
 * asked for. Transfers are limited per player, so a server that keeps changing its pack or a
 * failing transfer never loops.
 */
public final class ProxyCore implements TwilightProxyApi {
    /** Long enough for a player to accept the pack download Bedrock asks for after the transfer. */
    private static final long PENDING_MILLIS = 600_000;
    private static final long TRANSFER_WINDOW_MILLIS = 300_000;
    private static final int TRANSFERS_PER_WINDOW = 4;
    private static final int MAX_TRACKED = 100_000;

    private final Platform platform;
    private final Object owner;
    private final PackStore store;
    private final AutoTransfers transfers;
    /** Bedrock session (XUID) -> SHA-256 hex of the per-server pack it loaded ("" for none). */
    private final Map<String, String> loaded = new ConcurrentHashMap<>();
    /** XUID -> the server a transferred player goes to when it comes back. */
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    /**
     * Java name -> XUID of a transferred player: when it rejoins, some proxies (BungeeCord) choose its
     * server before Geyser knows its Java UUID; the name then finds the pending entry, accepted only
     * while a Bedrock session with that XUID is connected.
     */
    private final Map<String, String> pendingNames = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> recentTransfers = new ConcurrentHashMap<>();
    private volatile ProxyConfig config;
    private volatile GeyserBridge geyser;
    private volatile com.siberanka.twilight.host.PackHost host;

    public ProxyCore(Platform platform, Object owner) {
        this.platform = platform;
        this.owner = owner;
        this.store = new PackStore(platform);
        this.transfers = new AutoTransfers(platform, store);
    }

    private record Pending(String server, long created, long expires) {}

    /** XUID -> when its last Bedrock session loaded packs (Geyser sees it before the Java login). */
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public void enable() throws IOException {
        if (!reload()) throw new IOException("twilight-proxy configuration is invalid; see the log");
        try {
            geyser = GeyserBridge.attach(owner, this);
        } catch (LinkageError absent) {
            geyser = null; // Geyser's API classes are not on this proxy
        }
        if (geyser == null) platform.info("Geyser is not installed on this proxy: packs are prepared but not sent.");
        else startHost();
        platform.repeat(this::sweep, 5);
        TwilightProxyApi.Holder.set(this);
    }

    /** The pack host Bedrock players download from ({@code pack-host.enabled}); changes take effect on restart. */
    private void startHost() {
        var settings = config.host();
        if (!settings.enabled()) return;
        try {
            host = com.siberanka.twilight.host.PackHost.start(settings, platform.dataDirectory().resolve("pack-host"), platform::info);
        } catch (IOException | RuntimeException failure) {
            platform.warn("pack-host could not start; Geyser sends the packs itself: " + failure.getMessage(), null);
        }
    }

    com.siberanka.twilight.host.PackHost host() {
        return host;
    }

    void warn(String message) {
        platform.warn(message, null);
    }

    public void disable() {
        TwilightProxyApi.Holder.set(null);
        if (host != null) host.close();
        if (geyser != null) geyser.close();
        transfers.close();
    }

    @Override public synchronized boolean reload() {
        try {
            ProxyConfig next = ProxyConfig.load(platform.dataDirectory().resolve("config.yml"));
            config = next;
            store.reload(next, platform.serverNames());
            List<PackChannel.Key> keys = new ArrayList<>();
            for (String secret : ProxySecrets.discover(platform.proxyRoot(), platform.velocity(), next.secret())) {
                try { keys.add(PackChannel.Key.derive(secret)); }
                catch (IllegalArgumentException tooShort) { platform.warn("Ignoring a shared secret shorter than 16 characters.", null); }
            }
            transfers.keys(keys);
            platform.info(keys.isEmpty()
                    ? "No secret is shared with the backends (secret, Velocity forwarding or BungeeGuard): 'auto' packs are off."
                    : "Packs from Twilight on the backends are on (" + keys.size() + " shared secret(s)).");
            if (next.urlRefreshMinutes() > 0) store.refreshLinks();
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            platform.warn("Could not load twilight-proxy's configuration: " + failure.getMessage(), null);
            return false;
        }
    }

    // --- Plugin messages from backends -------------------------------------------------------

    /** A message on {@link PackChannel#CHANNEL} that came from backend {@code server}; returns a reply or null. */
    public byte[] serverMessage(String server, byte[] message) {
        if (message == null || message.length > PackChannel.MAX_MESSAGE) return null;
        ProxyConfig current = config;
        return current == null ? null : transfers.onMessage(server, message, current.maxPackBytes());
    }

    // --- Bedrock sessions --------------------------------------------------------------------

    /** The pack a new Bedrock session loads: that of the server it is going to join. */
    Optional<Path> packFor(String xuid) {
        if (xuid == null || xuid.isEmpty()) return Optional.empty();
        String server = Optional.ofNullable(pending.get(xuid)).filter(p -> p.expires > now()).map(Pending::server)
                .orElseGet(this::initialServer);
        Optional<PackFiles.Pack> pack = server.isEmpty() ? Optional.empty() : store.pack(server);
        if (loaded.size() < MAX_TRACKED) loaded.put(xuid, pack.map(PackFiles.Pack::hex).orElse(""));
        if (sessions.size() < MAX_TRACKED) sessions.put(xuid, now());
        return pack.map(PackFiles.Pack::path);
    }

    /** Where a transferred player rejoins, once; empty for everyone else. */
    public Optional<String> pending(UUID player, String name) {
        GeyserBridge bridge = geyser;
        if (bridge == null) return Optional.empty();
        Optional<String> xuid = bridge.xuid(player, name);
        if (xuid.isEmpty() && name != null) {
            String remembered = pendingNames.get(name.toLowerCase(Locale.ROOT));
            Pending waiting = remembered == null ? null : pending.get(remembered);
            // Accepted only when that Bedrock account reconnected after the transfer.
            if (waiting != null && sessions.getOrDefault(remembered, 0L) >= waiting.created) xuid = Optional.of(remembered);
        }
        if (xuid.isEmpty()) return Optional.empty();
        if (name != null) pendingNames.remove(name.toLowerCase(Locale.ROOT));
        Pending entry = pending.remove(xuid.get());
        if (entry == null || entry.expires <= now()) return Optional.empty();
        platform.info("Sending " + name + " to " + entry.server + " with its Bedrock pack.");
        return Optional.of(entry.server);
    }

    /**
     * Called before {@code player} connects to {@code server}. True when the Bedrock client was
     * transferred to load that server's pack: the caller cancels the connection.
     */
    public boolean beforeConnect(UUID player, String name, String server) {
        GeyserBridge bridge = geyser;
        ProxyConfig current = config;
        if (bridge == null || current == null || !current.transferOnSwitch() || !bridge.bedrock(player, name)) return false;
        Optional<String> xuid = bridge.xuid(player, name);
        if (xuid.isEmpty()) return false;
        String have = loaded.get(xuid.get());
        String want = wanted(server);
        if (have == null || want == null || want.equals(have)) return false;
        Deque<Long> recent = recentTransfers.computeIfAbsent(xuid.get(), ignored -> new ArrayDeque<>());
        synchronized (recent) {
            long now = now();
            while (!recent.isEmpty() && now - recent.peekFirst() > TRANSFER_WINDOW_MILLIS) recent.pollFirst();
            if (recent.size() >= TRANSFERS_PER_WINDOW) return false;
            recent.addLast(now);
        }
        pending.put(xuid.get(), new Pending(server.toLowerCase(Locale.ROOT), now(), now() + PENDING_MILLIS));
        if (name != null && pendingNames.size() < MAX_TRACKED) pendingNames.put(name.toLowerCase(Locale.ROOT), xuid.get());
        if (bridge.transfer(player, name, current.transferAddress(), current.transferPort())) {
            platform.info("Reconnecting " + name + " to load the Bedrock pack of " + server + ".");
            return true;
        }
        pending.remove(xuid.get());
        return false;
    }

    public void disconnect(UUID player, String name) {
        GeyserBridge bridge = geyser;
        if (bridge == null) return;
        // A transferred player keeps its pending entry; its new session records its pack again.
        bridge.xuid(player, name).ifPresent(xuid -> {
            if (!pending.containsKey(xuid)) loaded.remove(xuid);
        });
    }

    /** SHA-256 hex of the pack Bedrock players need on {@code server}; null when unknown (keep theirs). */
    private String wanted(String server) {
        ProxyConfig current = config;
        if (current.source(server) instanceof ProxyConfig.PackSource.None) return "";
        return store.pack(server).map(PackFiles.Pack::hex).orElse(null);
    }

    private String initialServer() {
        ProxyConfig current = config;
        String configured = current == null ? "" : current.initialServer();
        return configured.isEmpty() ? platform.defaultServer().toLowerCase(Locale.ROOT) : configured;
    }

    private void sweep() {
        transfers.sweep();
        long now = now();
        pending.values().removeIf(entry -> entry.expires <= now);
        pendingNames.values().removeIf(xuid -> !pending.containsKey(xuid));
        sessions.values().removeIf(time -> now - time > PENDING_MILLIS);
        recentTransfers.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().isEmpty() || now - entry.getValue().peekLast() > TRANSFER_WINDOW_MILLIS;
            }
        });
        ProxyConfig current = config;
        if (current != null && current.urlRefreshMinutes() > 0 && now - lastRefresh > current.urlRefreshMinutes() * 60_000L) {
            lastRefresh = now;
            store.refreshLinks();
        }
    }

    private volatile long lastRefresh = System.currentTimeMillis();

    public String statusText() {
        StringBuilder out = new StringBuilder("twilight-proxy: ")
                .append(transfers.enabled() ? "auto packs on" : "auto packs off (no shared secret)")
                .append(", Geyser ").append(geyser == null ? "absent" : "present");
        for (String server : platform.serverNames()) {
            out.append("\n  ").append(server).append(": ").append(store.pack(server)
                    .map(pack -> pack.size() / 1024 + " KiB " + pack.hex().substring(0, 12)).orElse("no pack"));
        }
        return out.toString();
    }

    @Override public Optional<Path> pack(String server) {
        return store.pack(server).map(PackFiles.Pack::path);
    }

    @Override public Optional<String> packSha256(String server) {
        return store.pack(server).map(PackFiles.Pack::hex);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
