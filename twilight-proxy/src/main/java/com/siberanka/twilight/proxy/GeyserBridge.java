/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import org.geysermc.event.PostOrder;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.util.Optional;
import java.util.UUID;

/**
 * The only class that touches Geyser's API, so twilight-proxy loads without Geyser. Packs are
 * registered per Bedrock session when it loads its resource packs (before it joins a server).
 */
final class GeyserBridge {
    private final EventRegistrar registrar;

    private GeyserBridge(Object owner) {
        this.registrar = EventRegistrar.of(owner);
    }

    /** Null when Geyser is not installed on this proxy. */
    @SuppressWarnings("deprecation") // register(ResourcePack) is the form every Geyser 2.x accepts
    static GeyserBridge attach(Object owner, ProxyCore core) {
        try {
            Class.forName("org.geysermc.geyser.api.GeyserApi");
            GeyserApi api = GeyserApi.api();
            if (api == null) return null;
            GeyserBridge bridge = new GeyserBridge(owner);
            api.eventBus().subscribe(bridge.registrar, SessionLoadResourcePacksEvent.class, event -> {
                GeyserConnection connection = event.connection();
                core.packFor(connection.xuid()).ifPresent(pack -> event.register(bridge.loaded(pack)));
            });
            // After every listener has added its packs, the pack host (when it runs) turns them all into links.
            api.eventBus().subscribe(bridge.registrar, SessionLoadResourcePacksEvent.class, event -> {
                var host = core.host();
                if (host == null) return;
                com.siberanka.twilight.host.SessionHosting.hostAll(host, event, (pack, reason) -> {
                    if (bridge.warned.add(pack + reason)) core.warn("pack-host: " + pack + " is sent by Geyser: " + reason);
                });
            }, PostOrder.LAST);
            return bridge;
        } catch (ClassNotFoundException | LinkageError | RuntimeException absent) {
            return null;
        }
    }

    /** Geyser's pack record of each pack file, reused while the file is unchanged (hashed once, not per session). */
    private final java.util.Map<java.nio.file.Path, Loaded> packs = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<String> warned = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record Loaded(long size, long modified, ResourcePack pack) {}

    private ResourcePack loaded(java.nio.file.Path file) {
        try {
            long size = java.nio.file.Files.size(file), modified = java.nio.file.Files.getLastModifiedTime(file).toMillis();
            Loaded known = packs.get(file);
            if (known != null && known.size() == size && known.modified() == modified) return known.pack();
            ResourcePack pack = ResourcePack.create(PackCodec.path(file));
            if (packs.size() > 256) packs.clear();
            packs.put(file, new Loaded(size, modified, pack));
            return pack;
        } catch (java.io.IOException unreadable) {
            return ResourcePack.create(PackCodec.path(file));
        }
    }

    boolean bedrock(UUID player, String name) {
        return connection(player, name) != null;
    }

    /**
     * The Bedrock session of a proxy player. While the first server connection is being chosen,
     * some proxies (BungeeCord) know the player before Geyser recorded its Java UUID, so the
     * session's Java name is matched as well.
     */
    Optional<String> xuid(UUID player, String name) {
        GeyserConnection connection = connection(player, name);
        return connection == null ? Optional.empty() : Optional.ofNullable(connection.xuid());
    }

    private static GeyserConnection connection(UUID player, String name) {
        GeyserApi api = GeyserApi.api();
        GeyserConnection connection = api.connectionByUuid(player);
        if (connection != null) return connection;
        for (GeyserConnection online : api.onlineConnections()) {
            if (player.equals(online.javaUuid()) || name != null && name.equalsIgnoreCase(online.javaUsername())) return online;
        }
        return null;
    }

    /** Sends the Bedrock client back to Geyser (configured address or the one it joined with). */
    boolean transfer(UUID player, String name, String address, int port) {
        GeyserConnection connection = connection(player, name);
        if (connection == null) return false;
        String host = address.isEmpty() ? connection.joinAddress() : address;
        int target = port > 0 ? port : connection.joinPort();
        return host != null && !host.isEmpty() && target > 0 && connection.transfer(host, target);
    }

    void close() {
        try { GeyserApi.api().eventBus().unregisterAll(registrar); } catch (RuntimeException | LinkageError ignored) { }
    }
}
