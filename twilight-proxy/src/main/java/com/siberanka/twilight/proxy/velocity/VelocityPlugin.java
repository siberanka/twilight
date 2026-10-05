/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy.velocity;

import com.google.inject.Inject;
import com.siberanka.twilight.protocol.PackChannel;
import com.siberanka.twilight.proxy.ProxyCore;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Velocity entry point (velocity-plugin.json). {@code @Subscribe(order)} is used instead of the newer
 * {@code priority} so the plugin also runs on Velocity releases before 3.3.
 */
@SuppressWarnings("deprecation")
public final class VelocityPlugin {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(PackChannel.CHANNEL);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private ProxyCore core;

    @Inject
    public VelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        ProxyCore created = new ProxyCore(new VelocityPlatform(this), this);
        try {
            created.enable();
        } catch (java.io.IOException failure) {
            logger.error("twilight-proxy is disabled: {}", failure.getMessage());
            return;
        }
        core = created;
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("twilightproxy").aliases("twproxy").build(),
                (SimpleCommand) invocation -> {
                    if (!invocation.source().hasPermission("twilight.proxy.admin")) return;
                    String[] args = invocation.arguments();
                    String result = args.length > 0 && args[0].equalsIgnoreCase("reload")
                            ? (created.reload() ? "twilight-proxy reloaded." : "Reload failed; see the console.")
                            : created.statusText();
                    invocation.source().sendMessage(Component.text(result));
                });
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) core.disable();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(PackChannel.CHANNEL)) return;
        // Consumed in both directions: clients never see it and cannot send it to a backend.
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection server) || core == null) return;
        byte[] reply = core.serverMessage(server.getServerInfo().getName(), event.getData());
        if (reply != null) server.sendPluginMessage(CHANNEL, reply);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        if (core == null) return;
        core.pending(event.getPlayer().getUniqueId(), event.getPlayer().getUsername())
                .flatMap(proxy::getServer)
                .ifPresent(event::setInitialServer);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPreConnect(ServerPreConnectEvent event) {
        if (core == null || !event.getResult().isAllowed()) return;
        RegisteredServer target = event.getResult().getServer().orElse(event.getOriginalServer());
        if (core.beforeConnect(event.getPlayer().getUniqueId(), event.getPlayer().getUsername(), target.getServerInfo().getName())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (core != null) core.disconnect(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
    }

    ProxyServer proxy() { return proxy; }

    Logger logger() { return logger; }

    Path dataDirectory() { return dataDirectory; }

    Collection<String> serverNames() {
        return proxy.getAllServers().stream().map(server -> server.getServerInfo().getName()).toList();
    }

    String defaultServer() {
        List<String> order = proxy.getConfiguration().getAttemptConnectionOrder();
        return order.isEmpty() ? "" : order.get(0);
    }

    void repeat(Runnable task, long seconds) {
        proxy.getScheduler().buildTask(this, task).repeat(seconds, TimeUnit.SECONDS).schedule();
    }

    void async(Runnable task) {
        proxy.getScheduler().buildTask(this, task).schedule();
    }
}
