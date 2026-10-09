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
 * asked for. Login plugins keep the last word: when one sends the reconnected player to a login or
 * limbo server first, the player goes on to its server at its next server change. A session's first
 * server never causes a reconnect, transfers are limited per player and every reconnect has a
 * deadline that grows with the pack, so nothing loops or waits forever.
 */
public final class ProxyCore implements TwilightProxyApi {
    private static final long TRANSFER_WINDOW_MILLIS = 300_000;
    private static final int TRANSFERS_PER_WINDOW = 4;
    private static final int MAX_TRACKED = 100_000;
    /** Packs at least this large get a pack-host hint when Geyser has to send them. */
    private static final long LARGE_PACK_BYTES = 32L * 1_048_576;
    private static final long GEYSER_BYTES_PER_SECOND = 1_250_000;
    /** The shortest time between a transfer and the client's next login seen in tests (cached pack). */
    private static final long RECONNECT_LOGIN_MILLIS = 4_000;
    /** A client that just joined its first server ignores a transfer until it is in game. */
    private static final long FIRST_SERVER_RECONNECT_DELAY_MILLIS = 5_000;

    private final Platform platform;
    private final Object owner;
    private final PackStore store;
    private final AutoTransfers transfers;
    /** Bedrock session (XUID) -> SHA-256 hex of the per-server pack it loaded ("" for none). */
    private final Map<String, String> loaded = new ConcurrentHashMap<>();
    /** Reconnects in progress, from the transfer until the player reaches its server. */
    private final Switches switches = new Switches();
    /** Proxy players already connected to a backend server; a session's first server is checked once. */
    private final java.util.Set<UUID> seated = ConcurrentHashMap.newKeySet();
    /** Servers whose large pack already got the pack-host hint. */
    private final java.util.Set<String> hinted = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Long>> recentTransfers = new ConcurrentHashMap<>();
    private volatile ProxyConfig config;
    private volatile GeyserBridge geyser;
    private volatile com.siberanka.twilight.host.PackHost host;
    private volatile com.siberanka.twilight.update.UpdateCheck updates;

    /** Players told about new versions; holders of {@link #ADMIN_PERMISSION} are told as well. */
    public static final String UPDATE_PERMISSION = "twilight.proxy.update";
    public static final String ADMIN_PERMISSION = "twilight.proxy.admin";

    public ProxyCore(Platform platform, Object owner) {
        this.platform = platform;
        this.owner = owner;
        this.store = new PackStore(platform);
        this.transfers = new AutoTransfers(platform, store);
        store.onChange(this::prewarm);
    }

    /**
     * Hashes a new pack for Geyser and copies it for the pack host now, so the first Bedrock session that
     * needs it does not wait for a large pack to be read during its login.
     */
    private void prewarm(PackFiles.Pack pack) {
        long started = System.nanoTime();
        try {
            GeyserBridge bridge = geyser;
            if (bridge != null) bridge.prepare(pack.path());
            var running = host;
            if (running != null) running.snapshot(pack.path(), pack.sha256());
        } catch (IOException | RuntimeException | LinkageError failure) {
            platform.warn("Could not prepare a Bedrock pack for Geyser: " + failure.getMessage(), null);
            return;
        }
        long millis = (System.nanoTime() - started) / 1_000_000;
        if (millis >= 1000) platform.info("Prepared a " + mib(pack.size()) + " pack for Bedrock sessions in " + millis / 1000 + " s.");
    }

    public void enable() throws IOException {
        if (!reload()) throw new IOException("twilight-proxy configuration is invalid; see the log");
        try {
            geyser = GeyserBridge.attach(owner, this);
        } catch (LinkageError absent) {
            geyser = null; // Geyser's API classes are not on this proxy
        }
        if (geyser == null) platform.info("Geyser is not installed on this proxy: packs are prepared but not sent.");
        else {
            startHost();
            platform.async(() -> platform.serverNames().forEach(server -> store.pack(server).ifPresent(this::prewarm)));
        }
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

    /**
     * A transferred Bedrock client logs in again a few seconds after it left (four or more, measured). A proxy
     * that refuses logins from the same address within a longer time turns the pack reconnect into a refusal.
     */
    public void checkLoginLimit(String setting, long millis) {
        if (geyser != null && millis > RECONNECT_LOGIN_MILLIS && config != null && config.transferOnSwitch()) {
            platform.warn(setting + " is " + millis + " ms: Bedrock players who reconnect for a server's pack log in again"
                    + " about " + RECONNECT_LOGIN_MILLIS / 1000 + " s after leaving and may be refused. Keep it at "
                    + RECONNECT_LOGIN_MILLIS + " ms or less.", null);
        }
    }

    public void disable() {
        TwilightProxyApi.Holder.set(null);
        var check = updates;
        if (check != null) check.close();
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
            configureUpdates(next);
            return true;
        } catch (IOException | IllegalArgumentException failure) {
            platform.warn("Could not load twilight-proxy's configuration: " + failure.getMessage(), null);
            return false;
        }
    }

    /**
     * {@code update-check}: looks for newer releases on GitHub (GitLab when GitHub cannot be reached), writes
     * them to the console and tells players with the update permission.
     */
    private void configureUpdates(ProxyConfig next) {
        var check = updates;
        if (!next.updateCheck() && check != null) {
            check.close();
            updates = null;
        } else if (next.updateCheck() && check == null) {
            updates = com.siberanka.twilight.update.UpdateCheck.start("twilight-proxy", platform.version(), platform::info,
                    release -> {
                        ProxyConfig current = config;
                        if (current != null && current.updateNotify()) platform.tellAdmins(updateText(release), release.page());
                    }).orElse(null);
        }
    }

    /** A newer release to tell a joining player with the update permission about, when notices are on. */
    public Optional<com.siberanka.twilight.update.UpdateCheck.Release> updateNotice() {
        var check = updates;
        ProxyConfig current = config;
        if (check == null || current == null || !current.updateNotify()) return Optional.empty();
        return check.latest();
    }

    /** The text before the release link in a player notice. */
    public String updateText(com.siberanka.twilight.update.UpdateCheck.Release release) {
        return "[twilight-proxy] Version " + release.version() + " is available (this proxy runs " + platform.version() + "): ";
    }

    // --- Plugin messages from backends -------------------------------------------------------

    /** A message on {@link PackChannel#CHANNEL} that came from backend {@code server}; returns a reply or null. */
    public byte[] serverMessage(String server, byte[] message) {
        if (message == null || message.length > PackChannel.MAX_MESSAGE) return null;
        ProxyConfig current = config;
        return current == null ? null : transfers.onMessage(server, message, current.maxPackBytes());
    }

    // --- Bedrock sessions --------------------------------------------------------------------

    /** What the proxy does with a server connection of a Bedrock player. */
    public sealed interface Route {
        /** Let it happen. */
        record Allow() implements Route {}

        /** Cancel it: the client was transferred to load the server's pack. */
        record Transferred() implements Route {}

        /** Cancel it and connect the player to {@code server} instead (a reconnect waiting for its destination). */
        record Redirect(String server) implements Route {}
    }

    private static final Route ALLOW = new Route.Allow();

    /** The pack a new Bedrock session loads: that of the server it is going to join. */
    Optional<Path> packFor(String xuid) {
        if (xuid == null || xuid.isEmpty()) return Optional.empty();
        long now = now();
        Optional<Switches.Switch> reconnect = switches.get(xuid, now);
        String server = reconnect.map(entry -> entry.to).orElseGet(this::initialServer);
        Optional<PackFiles.Pack> pack = server.isEmpty() ? Optional.empty() : store.pack(server);
        if (loaded.size() < MAX_TRACKED) loaded.put(xuid, pack.map(PackFiles.Pack::hex).orElse(""));
        switches.reconnected(xuid, now).ifPresent(entry -> platform.info(entry.name + " reconnected after "
                + entry.seconds(now) + " s; loading the pack of " + entry.to
                + pack.map(found -> " (" + mib(found.size()) + ")").orElse("") + "."));
        return pack.map(PackFiles.Pack::path);
    }

    /**
     * The first server of a session. {@code chosen} is the server picked after every other plugin ran,
     * {@code original} the one the proxy picked; returns the server to use instead, if any.
     */
    public Optional<String> initial(UUID player, String name, String chosen, String original) {
        GeyserBridge bridge = geyser;
        if (bridge == null) return Optional.empty();
        long now = now();
        Optional<String> xuid = xuidOf(bridge, player, name, now);
        if (xuid.isEmpty()) return Optional.empty();
        Switches.Initial decision = switches.initial(xuid.get(), chosen, original, now);
        if (decision instanceof Switches.Initial.Route route) {
            Switches.Switch entry = route.entry();
            platform.info(name + " is back " + entry.seconds(now) + " s after the transfer"
                    + (entry.reconnected > 0 ? " (pack and login " + Math.max(0, (now - entry.reconnected + 500) / 1000) + " s)" : "")
                    + "; sending it to " + entry.to + ".");
            return Optional.of(entry.to);
        }
        if (decision instanceof Switches.Initial.Defer defer) {
            platform.info(name + " was sent to " + defer.heldOn() + " by another plugin (login or limbo server); it goes on to "
                    + defer.entry().to + " when it next changes servers.");
        }
        return Optional.empty();
    }

    /**
     * Called before {@code player} connects to {@code server} with every other plugin's checks passed.
     * {@code first} is true for the first server of a session.
     */
    public Route beforeConnect(UUID player, String name, String server, boolean first) {
        GeyserBridge bridge = geyser;
        ProxyConfig current = config;
        if (bridge == null || current == null) return ALLOW;
        long now = now();
        Optional<String> xuid = xuidOf(bridge, player, name, now);
        if (xuid.isEmpty()) return ALLOW;
        Switches.Connect step = switches.connect(xuid.get(), server, first || current.loginServer(server), now);
        if (step instanceof Switches.Connect.Redirect redirect) {
            platform.info("Sending " + name + " on to " + redirect.entry().to + " instead of " + server
                    + " (the server it reconnected for, " + redirect.entry().seconds(now) + " s ago).");
            return new Route.Redirect(redirect.entry().to);
        }
        // The pack of a session was chosen for the server the proxy expected; when a plugin sends the player
        // somewhere else first (a login or limbo server), reconnecting there would only loop.
        if (first || !current.transferOnSwitch() || current.loginServer(server)) return ALLOW;
        if (!needsReconnect(xuid.get(), server)) return ALLOW;
        return reconnect(bridge, current, player, name, xuid.get(), server, now) ? new Route.Transferred() : ALLOW;
    }

    /** True when the session's loaded pack is not the one {@code server} needs (and that one is known). */
    private boolean needsReconnect(String xuid, String server) {
        String have = loaded.get(xuid);
        String want = wanted(server);
        return have != null && want != null && !want.equals(have);
    }

    /** Transfers the client to load the pack of {@code server}; false when it was not (limits, no address). */
    private boolean reconnect(GeyserBridge bridge, ProxyConfig current, UUID player, String name, String xuidValue,
                              String server, long now) {
        Optional<String> xuid = Optional.of(xuidValue);
        Deque<Long> recent = recentTransfers.computeIfAbsent(xuid.get(), ignored -> new ArrayDeque<>());
        synchronized (recent) {
            while (!recent.isEmpty() && now - recent.peekFirst() > TRANSFER_WINDOW_MILLIS) recent.pollFirst();
            if (recent.size() >= TRANSFERS_PER_WINDOW) {
                platform.warn(name + " was reconnected " + TRANSFERS_PER_WINDOW + " times in "
                        + TRANSFER_WINDOW_MILLIS / 60_000 + " minutes; it joins " + server + " with the pack it has.", null);
                return false;
            }
            recent.addLast(now);
        }
        long bytes = store.pack(server).map(PackFiles.Pack::size).orElse(0L);
        long deadline = current.transferDeadlineMillis(bytes);
        String from = platform.currentServer(player).orElse("");
        java.net.InetAddress client = platform.address(player).orElse(null);
        java.net.InetAddress playing = bridge.address(player, name, client).orElse(null);
        Optional<String> address = bridge.transfer(player, name, client, current.transferAddress(), current.transferPort());
        if (address.isEmpty()) return false;
        switches.start(new Switches.Switch(xuid.get(), name, from, server.toLowerCase(Locale.ROOT), bytes, now,
                now + deadline, address.get(), playing == null ? null : com.siberanka.twilight.host.PackHost.normalise(playing)));
        platform.info("Reconnecting " + name + " to load the Bedrock pack of " + server + " (" + mib(bytes)
                + "); waiting up to " + Math.max(1, deadline / 60_000) + " min for it to come back.");
        if (bytes >= LARGE_PACK_BYTES && host == null && hinted.add(server.toLowerCase(Locale.ROOT))) {
            platform.info("The pack of " + server + " is " + mib(bytes) + ": Geyser sends at most about 1.2 MiB/s, so it takes "
                    + Math.max(1, bytes / GEYSER_BYTES_PER_SECOND) + " s or longer. Enable pack-host to let Bedrock download it over HTTP.");
        }
        return true;
    }

    /**
     * {@code player} is now connected to {@code server}. A reconnect ends here when that is its server; on any
     * other server (a login server, or where a plugin sent the player) it waits for the next server change.
     */
    public void connected(UUID player, String name, String server) {
        GeyserBridge bridge = geyser;
        ProxyConfig current = config;
        if (bridge == null || current == null || server == null) return;
        long now = now();
        Optional<String> xuid = xuidOf(bridge, player, name, now);
        if (xuid.isEmpty()) return;
        boolean first = seated.size() < MAX_TRACKED && seated.add(player);
        Switches.Connected state = switches.connected(xuid.get(), server, now);
        // A fresh session whose first server is not the one its pack was chosen for (a proxy that reconnects
        // players to their last server, forced hosts): reconnect once for this server's pack. Cancelling the
        // first connection instead is not possible on every proxy, so this happens once the player is on it.
        if (first && state instanceof Switches.Connected.None && current.transferOnSwitch() && !current.loginServer(server)
                && needsReconnect(xuid.get(), server)) {
            platform.info(name + " joined " + server + ", not the server its pack was chosen for; reconnecting it once for "
                    + server + "'s pack (set initial-server to the server players join first to avoid this).");
            // Bedrock ignores a transfer while it is still loading the world: wait until it is in game.
            platform.later(() -> {
                if (!platform.currentServer(player).map(server::equalsIgnoreCase).orElse(false)) return;
                if (!needsReconnect(xuid.get(), server)) return;
                reconnect(bridge, current, player, name, xuid.get(), server, now());
            }, FIRST_SERVER_RECONNECT_DELAY_MILLIS);
            return;
        }
        if (state instanceof Switches.Connected.Arrived arrived) {
            Switches.Switch entry = arrived.entry();
            platform.info(name + " reached " + entry.to + " " + entry.seconds(now) + " s after the transfer"
                    + (entry.reconnected > 0 ? " (reconnect " + Math.max(0, (entry.reconnected - entry.started + 500) / 1000)
                    + " s, pack and login " + Math.max(0, (now - entry.reconnected + 500) / 1000) + " s)" : "") + ".");
        } else if (state instanceof Switches.Connected.Held held && held.first()) {
            platform.info(name + " joined " + server + " first (a login or limbo server, or another plugin's choice); it goes on to "
                    + held.entry().to + " when it next changes servers.");
        }
    }

    /** The connection of a {@link Route.Redirect} failed; the player goes where it asked to instead. */
    public void redirectFailed(String name, String server, String asked) {
        platform.info(name + " could not be sent on to " + server + "; it goes to " + asked + " as it asked.");
    }

    /**
     * Another plugin refused the first connection of a reconnected player to the server it reconnected for
     * (a login plugin that blocks every other server). Returns true when the caller should connect the player
     * to the server the proxy picked first; the reconnect then waits for the player's next server change.
     */
    public boolean refusedFirst(UUID player, String name, String server) {
        GeyserBridge bridge = geyser;
        if (bridge == null) return false;
        long now = now();
        Optional<String> xuid = xuidOf(bridge, player, name, now);
        Optional<Switches.Switch> entry = xuid.flatMap(found -> switches.refused(found, server, now));
        entry.ifPresent(found -> platform.info("Another plugin refused " + server + " as " + name
                + "'s first server (login required?); it joins the proxy's first server and goes on to " + server
                + " when it next changes servers."));
        return entry.isPresent();
    }

    public void disconnect(UUID player, String name) {
        seated.remove(player);
        GeyserBridge bridge = geyser;
        if (bridge == null) return;
        // A transferred player keeps its switch; its new session records its pack again.
        bridge.xuid(player, name, platform.address(player).orElse(null)).ifPresent(xuid -> {
            if (switches.get(xuid, now()).isEmpty()) loaded.remove(xuid);
        });
    }

    /**
     * The Bedrock session (XUID) of a proxy player: through Geyser, or for a transferred player whose Java UUID
     * Geyser does not know yet, by its name from the address its client plays from.
     */
    private Optional<String> xuidOf(GeyserBridge bridge, UUID player, String name, long now) {
        java.net.InetAddress address = platform.address(player).map(com.siberanka.twilight.host.PackHost::normalise).orElse(null);
        return bridge.xuid(player, name, address).or(() -> switches.xuidByName(name, address, now));
    }

    private static String mib(long bytes) {
        return bytes >= 10 * 1_048_576 ? bytes / 1_048_576 + " MiB" : String.format(Locale.ROOT, "%.1f MiB", bytes / 1_048_576.0);
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
        ProxyConfig settings = config;
        if (settings != null) store.sweepVersions(settings.transferDeadlineMillis(settings.maxPackBytes()) + 600_000);
        for (Switches.Note note : switches.sweep(now)) {
            if (note.warning()) platform.warn(note.text(), null);
            else platform.info(note.text());
        }
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
        var check = updates;
        out.append("\n  ").append(check == null ? "update check disabled" : check.status());
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
