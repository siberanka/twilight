/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.text;

import com.google.gson.JsonParser;
import com.siberanka.twilight.text.TextLayout;
import com.siberanka.twilight.text.TextLayoutTable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Lays out Java text for Bedrock players with Java font metrics before Geyser translates it.
 *
 * <p>Chest titles always pass through the layout: the generated chest UI starts the title
 * label {@link TextLayoutTable#containerOrigin()} units left of Java's origin. The other
 * surfaces (chat, action bar, titles, boss bars, scoreboards, entity names and text
 * displays, other container titles) are rewritten only when they contain characters the
 * table knows, so custom-font glyphs, Java-remapped characters and space shifts look like
 * Java everywhere; other packets reach Geyser unchanged.
 *
 * <p>Geyser fills its translator registry during start-up and may reload packs, so the
 * bridge attaches after Geyser's initialize and reload events and reads the table of the
 * pack Geyser then serves. Packet fields are read and replaced by name, because Geyser
 * relocates Adventure on some platforms; components are handled by {@link AdventureTextLayout}.
 */
public final class GeyserTextLayoutBridge implements AutoCloseable {
    private final Path servedPack;
    private final Logger logger;
    private final EventRegistrar registrar;
    private final Map<Class<? extends Packet>, Surface<?>> surfaces = new LinkedHashMap<>();
    private final Set<String> loggedFailures = ConcurrentHashMap.newKeySet();
    private volatile TextLayoutTable table;

    private GeyserTextLayoutBridge(Object owner, Path servedPack, Logger logger, boolean allSurfaces) {
        this.servedPack = servedPack;
        this.logger = logger;
        this.registrar = EventRegistrar.of(owner);
        surface(ClientboundOpenScreenPacket.class, this::openScreen);
        if (!allSurfaces) return;
        surface(ClientboundSystemChatPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                packet.isOverlay() ? TextLayout.Mode.CENTERED : TextLayout.Mode.LEFT, "Content"));
        surface(ClientboundPlayerChatPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.LEFT, "Content", "UnsignedContent", "Name", "TargetName"));
        surface(ClientboundDisguisedChatPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.LEFT, "Message", "Name", "TargetName"));
        surface(ClientboundSetActionBarTextPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.CENTERED, "Text"));
        surface(ClientboundSetTitleTextPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.CENTERED, "Text"));
        surface(ClientboundSetSubtitleTextPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.CENTERED, "Text"));
        surface(ClientboundBossEventPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.CENTERED, "Title"));
        surface(ClientboundSetObjectivePacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.CENTERED, "DisplayName"));
        surface(ClientboundSetPlayerTeamPacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.LEFT, "DisplayName", "PlayerPrefix", "PlayerSuffix"));
        surface(ClientboundSetScorePacket.class, (session, packet, table) -> TextSurfaces.rewrite(packet, table,
                TextLayout.Mode.LEFT, "Display"));
        surface(ClientboundSetEntityDataPacket.class, (session, packet, table) -> TextSurfaces.entityData(packet, table));
    }

    /**
     * Follows Geyser's lifecycle; text passes through unchanged while the served pack has no layout table.
     *
     * @param allSurfaces also lay out chat, titles, boss bars, scoreboards and entity names, not only containers
     */
    public static GeyserTextLayoutBridge create(Object owner, Path servedPack, Logger logger, boolean allSurfaces) {
        GeyserTextLayoutBridge bridge = new GeyserTextLayoutBridge(owner, servedPack, logger, allSurfaces);
        var events = GeyserApi.api().eventBus();
        events.subscribe(bridge.registrar, GeyserPostInitializeEvent.class, event -> bridge.attach());
        events.subscribe(bridge.registrar, GeyserPostReloadEvent.class, event -> bridge.attach());
        if (Registries.JAVA_PACKET_TRANSLATORS.get().get(ClientboundOpenScreenPacket.class) != null) bridge.attach();
        return bridge;
    }

    private <P extends Packet> void surface(Class<P> type, Rewrite<P> rewrite) {
        surfaces.put(type, new Surface<>(type, rewrite));
    }

    private synchronized void attach() {
        try {
            table = readTable(servedPack);
        } catch (IOException | RuntimeException failure) {
            table = null;
            logger.log(Level.SEVERE, "Could not read the text layout of the served Twilight pack", failure);
        }
        var translators = Registries.JAVA_PACKET_TRANSLATORS.get();
        List<String> missing = new ArrayList<>();
        for (Surface<?> surface : surfaces.values()) {
            PacketTranslator<? extends Packet> current = translators.get(surface.type);
            if (current == null) {
                missing.add(surface.type.getSimpleName());
                continue;
            }
            if (current != surface) {
                surface.original = current;
                translators.put(surface.type, surface);
            }
        }
        if (!missing.isEmpty()) logger.warning("Geyser has no translator for " + missing + "; their text keeps Bedrock's layout.");
        logger.info(table == null ? "Java text layout idle: the served pack has no layout table."
                : "Java text layout active for " + (surfaces.size() - missing.size()) + " Bedrock packet types ("
                + table.entryCount() + " font entries).");
    }

    static TextLayoutTable readTable(Path pack) throws IOException {
        if (!Files.isRegularFile(pack)) return null;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry(TextLayoutTable.PATH);
            if (entry == null) return null;
            if (entry.getSize() > 64L * 1024 * 1024) throw new IOException("Text layout table exceeds size limit");
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                return TextLayoutTable.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
    }

    @Override public synchronized void close() {
        GeyserApi.api().eventBus().unregisterAll(registrar);
        var translators = Registries.JAVA_PACKET_TRANSLATORS.get();
        for (Surface<?> surface : surfaces.values()) {
            if (surface.original != null) translators.replace(surface.type, surface, surface.original);
        }
    }

    private ClientboundOpenScreenPacket openScreen(GeyserSession session, ClientboundOpenScreenPacket packet,
                                                   TextLayoutTable table) throws ReflectiveOperationException {
        // The touch layout keeps its centred native title; only glyph substitution applies there.
        var profile = session.getClientData() == null ? null : session.getClientData().getUiProfile();
        boolean pocket = profile != null && "POCKET".equals(profile.toString());
        return TextSurfaces.openScreen(packet, table, pocket);
    }

    private void logOnce(Class<?> type, Throwable failure) {
        if (loggedFailures.add(type.getName())) {
            logger.log(Level.WARNING, "Java text layout failed for " + type.getSimpleName()
                    + "; such text is shown unchanged. Further failures of this kind are not logged.", failure);
        }
    }

    @FunctionalInterface
    interface Rewrite<P> {
        P apply(GeyserSession session, P packet, TextLayoutTable table) throws ReflectiveOperationException;
    }

    private final class Surface<P extends Packet> extends PacketTranslator<P> {
        private final Class<P> type;
        private final Rewrite<P> rewrite;
        private volatile PacketTranslator<? extends Packet> original;

        private Surface(Class<P> type, Rewrite<P> rewrite) {
            this.type = type;
            this.rewrite = rewrite;
        }

        @SuppressWarnings("unchecked")
        @Override public void translate(GeyserSession session, P packet) {
            P forwarded = packet;
            TextLayoutTable current = table;
            if (current != null) {
                try {
                    forwarded = rewrite.apply(session, packet, current);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    logOnce(type, failure);
                }
            }
            ((PacketTranslator<P>) original).translate(session, forwarded);
        }

        @Override public boolean shouldExecuteInEventLoop() {
            return original.shouldExecuteInEventLoop();
        }
    }
}
