/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy.velocity;

import com.siberanka.twilight.proxy.Platform;

import java.nio.file.Path;
import java.util.Collection;

final class VelocityPlatform implements Platform {
    private final VelocityPlugin plugin;

    VelocityPlatform(VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public Path dataDirectory() { return plugin.dataDirectory(); }

    @Override public Path proxyRoot() { return Path.of("").toAbsolutePath(); }

    @Override public boolean velocity() { return true; }

    @Override public Collection<String> serverNames() { return plugin.serverNames(); }

    @Override public String defaultServer() { return plugin.defaultServer(); }

    @Override public void info(String message) { plugin.logger().info(message); }

    @Override public void warn(String message, Throwable failure) {
        if (failure == null) plugin.logger().warn(message);
        else plugin.logger().warn(message, failure);
    }

    @Override public void async(Runnable task) { plugin.async(task); }

    @Override public void repeat(Runnable task, long periodSeconds) { plugin.repeat(task, periodSeconds); }

    @Override public void later(Runnable task, long millis) { plugin.later(task, millis); }

    @Override public java.util.Optional<java.net.InetAddress> address(java.util.UUID player) {
        return plugin.proxy().getPlayer(player).map(found -> found.getRemoteAddress().getAddress());
    }

    @Override public java.util.Optional<String> currentServer(java.util.UUID player) {
        return plugin.proxy().getPlayer(player).flatMap(found -> found.getCurrentServer())
                .map(connection -> connection.getServerInfo().getName());
    }

    @Override public String version() { return plugin.version(); }

    @Override public java.util.Optional<java.util.Set<String>> pluginFolders() {
        return java.util.Optional.of(plugin.proxy().getPluginManager().getPlugins().stream()
                .map(container -> container.getDescription().getId()).collect(java.util.stream.Collectors.toSet()));
    }

    @Override public java.util.Optional<String> geyserPlugin() {
        return plugin.proxy().getPluginManager().getPlugin("geyser").map(container -> container.getDescription().getName().orElse("Geyser")
                + " " + container.getDescription().getVersion().orElse(""));
    }

    @Override public void tellAdmins(String text, String url) {
        plugin.proxy().getAllPlayers().stream().filter(VelocityPlugin::admin).forEach(player -> VelocityPlugin.tell(player, text, url));
    }
}
