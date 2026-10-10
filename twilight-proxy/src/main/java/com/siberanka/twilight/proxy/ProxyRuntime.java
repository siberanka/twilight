/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import com.siberanka.twilight.geyser.CustomBlocks;
import com.siberanka.twilight.geyser.LoadingGuard;
import com.siberanka.twilight.integration.text.GeyserLanguageBridge;
import com.siberanka.twilight.integration.text.GeyserTextLayoutBridge;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Twilight's runtime on the proxy's Geyser, the part a backend's own Geyser would otherwise run: the Java text
 * layout (custom font glyphs, menu titles, aliases of ordinary characters and named fonts, spacing), the packs'
 * translations, and keeping clients alive while they load large packs. Each Bedrock session gets the layout of
 * the pack it loaded for its server. Only this class and the bridges it starts touch Geyser's internals.
 */
final class ProxyRuntime implements AutoCloseable {
    private static final int MAX_CACHED = 64;

    private final GeyserTextLayoutBridge text;
    private final GeyserLanguageBridge language;
    private final LoadingGuard guard;

    private ProxyRuntime(GeyserTextLayoutBridge text, GeyserLanguageBridge language, LoadingGuard guard) {
        this.text = text;
        this.language = language;
        this.guard = guard;
    }

    /**
     * Starts what {@code config} enables. {@code packOf} gives the pack file a Bedrock session (by XUID) loaded,
     * {@code packs} every server's current pack.
     */
    static ProxyRuntime start(Object owner, ProxyConfig config, Platform platform, Function<String, Optional<Path>> packOf,
                              Supplier<Collection<Path>> packs) {
        return start(owner, config, platform, packOf, packs, null);
    }

    /** @param registered custom blocks already subscribed in the proxy's load phase, or null */
    static ProxyRuntime start(Object owner, ProxyConfig config, Platform platform, Function<String, Optional<Path>> packOf,
                              Supplier<Collection<Path>> packs, CustomBlocks registered) {
        Logger logger = logger(platform);
        GeyserTextLayoutBridge text = null;
        GeyserLanguageBridge language = null;
        LoadingGuard guard = null;
        try {
            if (config.textLayout()) text = GeyserTextLayoutBridge.create(owner, new SessionLayouts(packOf), logger, true);
        } catch (RuntimeException | LinkageError failure) {
            platform.warn("Java text layout is unavailable for this Geyser build; custom font glyphs keep Bedrock's layout.", failure);
        }
        try {
            if (config.translations()) language = GeyserLanguageBridge.create(owner, packs, logger);
        } catch (RuntimeException | LinkageError failure) {
            platform.warn("Resource-pack translations are unavailable for this Geyser build.", failure);
        }
        try {
            guard = LoadingGuard.start(owner, config.loadingProtectionSeconds(), platform::info);
        } catch (RuntimeException | LinkageError failure) {
            platform.warn("Loading protection is unavailable for this Geyser build.", failure);
        }
        ProxyRuntime runtime = new ProxyRuntime(text, language, guard);
        try {
            if (registered != null) runtime.blocks = registered;
            else if (config.customBlocks()) runtime.blocks = CustomBlocks.start(owner, packs, platform::info, platform::warn);
        } catch (RuntimeException | LinkageError failure) {
            platform.warn("Custom blocks are unavailable for this Geyser build; they keep the vanilla look on Bedrock.", failure);
        }
        return runtime;
    }

    private volatile CustomBlocks blocks;

    /**
     * The servers' packs changed: their translations are read again.
     *
     * @return a description of the custom blocks when they differ from what Geyser registered at start (Geyser
     *         registers blocks only then), otherwise null
     */
    String packsChanged() {
        if (language != null) language.refresh();
        CustomBlocks custom = blocks;
        CustomBlocks.Merged registered = custom == null ? null : custom.registered();
        if (registered == null) return null;
        CustomBlocks.Merged now = custom.current();
        return now.hash().equals(registered.hash()) ? null : now.blocks().size() + " custom block(s)";
    }

    @Override
    public void close() {
        for (AutoCloseable part : new AutoCloseable[]{text, language, guard, blocks}) {
            if (part == null) continue;
            try {
                part.close();
            } catch (Exception ignored) {
                // Geyser is shutting down as well.
            }
        }
    }

    /** The layout of the pack each session loaded, read once per pack version (versions never change). */
    private static final class SessionLayouts implements GeyserTextLayoutBridge.LayoutSource {
        private static final GeyserTextLayoutBridge.Layout NONE = new GeyserTextLayoutBridge.Layout(null, java.util.Set.of());
        private final Function<String, Optional<Path>> packOf;
        private final Map<Path, GeyserTextLayoutBridge.Layout> layouts = new ConcurrentHashMap<>();

        SessionLayouts(Function<String, Optional<Path>> packOf) {
            this.packOf = packOf;
        }

        @Override public void refresh() {
            layouts.clear();
        }

        @Override public GeyserTextLayoutBridge.Layout layout(GeyserSession session) {
            String xuid = session.xuid();
            if (xuid == null) return null;
            Optional<Path> pack = packOf.apply(xuid);
            if (pack.isEmpty()) return null;
            GeyserTextLayoutBridge.Layout layout = layouts.get(pack.get());
            if (layout == null) {
                try {
                    layout = GeyserTextLayoutBridge.readLayout(pack.get());
                } catch (IOException | RuntimeException unreadable) {
                    layout = null;
                }
                if (layouts.size() >= MAX_CACHED) layouts.clear();
                // A pack without a layout table (a server without custom fonts) is remembered as NONE.
                if (layout == null) layout = NONE;
                layouts.put(pack.get(), layout);
            }
            return layout == NONE || layout.table() == null ? null : layout;
        }

        @Override public String describe() {
            return "the layout of each server's pack";
        }
    }

    /** The bridges log through java.util.logging; this forwards to the proxy's log. */
    private static Logger logger(Platform platform) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                String message = record.getMessage();
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) platform.warn(message, record.getThrown());
                else platform.info(message);
            }

            @Override public void flush() {
            }

            @Override public void close() {
            }
        });
        return logger;
    }
}
