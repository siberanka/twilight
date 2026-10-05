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
}
