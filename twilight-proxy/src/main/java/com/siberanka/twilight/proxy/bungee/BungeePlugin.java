/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy.bungee;

import com.siberanka.twilight.protocol.PackChannel;
import com.siberanka.twilight.proxy.Platform;
import com.siberanka.twilight.proxy.ProxyCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** BungeeCord (and Waterfall) entry point (bungee.yml). */
public final class BungeePlugin extends Plugin implements Listener {
    private ProxyCore core;

    @Override
    public void onEnable() {
        getProxy().registerChannel(PackChannel.CHANNEL);
        ProxyCore created = new ProxyCore(new BungeePlatform(), this);
        try {
            created.enable();
        } catch (java.io.IOException failure) {
            getLogger().severe("twilight-proxy is disabled: " + failure.getMessage());
            return;
        }
        core = created;
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new Command("twilightproxy", "twilight.proxy.admin", "twproxy") {
            @Override public void execute(CommandSender sender, String[] args) {
                String result = args.length > 0 && args[0].equalsIgnoreCase("reload")
                        ? (created.reload() ? "twilight-proxy reloaded." : "Reload failed; see the console.")
                        : created.statusText();
                sender.sendMessage(new TextComponent(result));
            }
        });
    }

    @Override
    public void onDisable() {
        if (core != null) core.disable();
        getProxy().unregisterChannel(PackChannel.CHANNEL);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPluginMessage(PluginMessageEvent event) {
        if (!PackChannel.CHANNEL.equals(event.getTag())) return;
        // Consumed in both directions: clients never see it and cannot send it to a backend.
        event.setCancelled(true);
        if (core == null || !(event.getSender() instanceof Server server) || !(event.getReceiver() instanceof ProxiedPlayer)) return;
        byte[] reply = core.serverMessage(server.getInfo().getName(), event.getData());
        if (reply != null) server.sendData(PackChannel.CHANNEL, reply);
    }

    /** The first server BungeeCord picked for each joining player, before other plugins changed it. */
    private final Map<UUID, String> originals = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConnectFirst(ServerConnectEvent event) {
        if (core == null || !joining(event) || originals.size() > 100_000) return;
        originals.put(event.getPlayer().getUniqueId(), event.getTarget().getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        boolean first = joining(event);
        String original = first ? originals.remove(player.getUniqueId()) : null;
        if (core == null || event.isCancelled()) return;
        if (first) {
            core.initial(player.getUniqueId(), player.getName(), event.getTarget().getName(), original)
                    .map(name -> getProxy().getServerInfo(name))
                    .ifPresent(event::setTarget);
        }
        ServerInfo target = event.getTarget();
        ProxyCore.Route route = core.beforeConnect(player.getUniqueId(), player.getName(), target.getName(), first);
        if (route instanceof ProxyCore.Route.Transferred) {
            event.setCancelled(true);
        } else if (route instanceof ProxyCore.Route.Redirect redirect && !first) {
            ServerInfo destination = getProxy().getServerInfo(redirect.server());
            if (destination == null) return;
            // A new request, so every plugin checks the destination as well.
            event.setCancelled(true);
            getProxy().getScheduler().schedule(this, () -> player.connect(destination, (connected, error) -> {
                if (!Boolean.TRUE.equals(connected)) {
                    core.redirectFailed(player.getName(), redirect.server(), target.getName());
                    player.connect(target);
                }
            }), 100, TimeUnit.MILLISECONDS);
        }
    }

    /** First connection after joining the proxy (Reason exists on BungeeCord 1.13 and later). */
    private static boolean joining(ServerConnectEvent event) {
        try {
            return event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY;
        } catch (NoSuchMethodError | NoClassDefFoundError older) {
            return event.getPlayer().getServer() == null;
        }
    }

    @EventHandler
    public void onConnected(ServerConnectedEvent event) {
        if (core != null) core.connected(event.getPlayer().getUniqueId(), event.getPlayer().getName(),
                event.getServer().getInfo().getName());
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        originals.remove(event.getPlayer().getUniqueId());
        if (core != null) core.disconnect(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    private final class BungeePlatform implements Platform {
        @Override public Path dataDirectory() { return getDataFolder().toPath(); }

        @Override public Path proxyRoot() { return Path.of("").toAbsolutePath(); }

        @Override public boolean velocity() { return false; }

        @Override public Collection<String> serverNames() { return List.copyOf(getProxy().getServers().keySet()); }

        @Override public String defaultServer() {
            for (ListenerInfo listener : getProxy().getConfig().getListeners()) {
                List<String> priorities = listener.getServerPriority();
                if (priorities != null && !priorities.isEmpty()) return priorities.get(0);
            }
            return getProxy().getServers().values().stream().map(ServerInfo::getName).findFirst().orElse("");
        }

        @Override public void info(String message) { getLogger().info(message); }

        @Override public void warn(String message, Throwable failure) { getLogger().log(Level.WARNING, message, failure); }

        @Override public void async(Runnable task) { getProxy().getScheduler().runAsync(BungeePlugin.this, task); }

        @Override public void repeat(Runnable task, long periodSeconds) {
            getProxy().getScheduler().schedule(BungeePlugin.this, task, periodSeconds, periodSeconds, TimeUnit.SECONDS);
        }

        @Override public void later(Runnable task, long millis) {
            getProxy().getScheduler().schedule(BungeePlugin.this, task, millis, TimeUnit.MILLISECONDS);
        }

        @Override public Optional<java.net.InetAddress> address(UUID player) {
            ProxiedPlayer online = getProxy().getPlayer(player);
            if (online == null || !(online.getSocketAddress() instanceof java.net.InetSocketAddress socket)) return Optional.empty();
            return Optional.ofNullable(socket.getAddress());
        }

        @Override public Optional<String> currentServer(UUID player) {
            ProxiedPlayer online = getProxy().getPlayer(player);
            return online == null || online.getServer() == null ? Optional.empty() : Optional.of(online.getServer().getInfo().getName());
        }
    }
}
