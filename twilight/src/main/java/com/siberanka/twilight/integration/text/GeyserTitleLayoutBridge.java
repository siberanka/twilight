/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.text;

import com.google.gson.JsonParser;
import com.siberanka.twilight.text.TextLayoutTable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Lays out chest titles for Bedrock players with Java font metrics before Geyser
 * translates them. The generated chest UI starts the title label
 * {@link TextLayoutTable#ORIGIN} units left of Java's origin, so every chest title is
 * rewritten (at least with leading spacers) while the served pack carries a layout.
 *
 * <p>Geyser fills its translator registry during start-up and may reload packs, so the
 * bridge attaches after Geyser's initialize and reload events and reads the table of
 * the pack Geyser then serves. Components are handled by {@link AdventureTitleLayout}.
 */
public final class GeyserTitleLayoutBridge implements AutoCloseable {
    /** Container types Geyser shows with Bedrock's chest screens, whose label the pack moves. */
    static final Set<ContainerType> CHEST_SCREENS = EnumSet.of(ContainerType.GENERIC_9X1, ContainerType.GENERIC_9X2,
            ContainerType.GENERIC_9X3, ContainerType.GENERIC_9X4, ContainerType.GENERIC_9X5,
            ContainerType.GENERIC_9X6, ContainerType.SHULKER_BOX);

    private final Path servedPack;
    private final Logger logger;
    private final EventRegistrar registrar;
    private final Wrapper wrapper = new Wrapper();
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private volatile TextLayoutTable table;
    private volatile PacketTranslator<? extends Packet> original;

    private GeyserTitleLayoutBridge(Object owner, Path servedPack, Logger logger) {
        this.servedPack = servedPack;
        this.logger = logger;
        this.registrar = EventRegistrar.of(owner);
    }

    /** Follows Geyser's lifecycle; titles pass through unchanged while the served pack has no layout table. */
    public static GeyserTitleLayoutBridge create(Object owner, Path servedPack, Logger logger) {
        GeyserTitleLayoutBridge bridge = new GeyserTitleLayoutBridge(owner, servedPack, logger);
        var events = GeyserApi.api().eventBus();
        events.subscribe(bridge.registrar, GeyserPostInitializeEvent.class, event -> bridge.attach());
        events.subscribe(bridge.registrar, GeyserPostReloadEvent.class, event -> bridge.attach());
        if (Registries.JAVA_PACKET_TRANSLATORS.get().get(ClientboundOpenScreenPacket.class) != null) bridge.attach();
        return bridge;
    }

    private synchronized void attach() {
        try {
            table = readTable(servedPack);
        } catch (IOException | RuntimeException failure) {
            table = null;
            logger.log(Level.SEVERE, "Could not read the text layout of the served Twilight pack", failure);
        }
        var translators = Registries.JAVA_PACKET_TRANSLATORS.get();
        PacketTranslator<? extends Packet> current = translators.get(ClientboundOpenScreenPacket.class);
        if (current == null) {
            logger.severe("Geyser has no open-screen translator; Java title layout is inactive.");
            return;
        }
        if (current != wrapper) {
            original = current;
            translators.put(ClientboundOpenScreenPacket.class, wrapper);
        }
        logger.info(table == null ? "Java title layout idle: the served pack has no layout table."
                : "Java title layout active for Bedrock chest screens (" + table.entryCount() + " font entries).");
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
        if (original != null) Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundOpenScreenPacket.class, wrapper, original);
    }

    @SuppressWarnings("unchecked")
    private void forward(GeyserSession session, ClientboundOpenScreenPacket packet) {
        ((PacketTranslator<ClientboundOpenScreenPacket>) original).translate(session, packet);
    }

    ClientboundOpenScreenPacket rewrite(ClientboundOpenScreenPacket packet, boolean pocket) {
        TextLayoutTable table = this.table;
        if (table == null || !CHEST_SCREENS.contains(packet.getType())) return packet;
        try {
            Method getTitle = packet.getClass().getMethod("getTitle");
            Object title = getTitle.invoke(packet);
            Object laidOut;
            try {
                laidOut = AdventureTitleLayout.layoutTitle(table, title, pocket);
            } catch (ReflectiveOperationException | RuntimeException layoutFailure) {
                if (pocket) throw layoutFailure;
                logOnce("Title layout failed; keeping Java text with the origin spacers only.", layoutFailure);
                laidOut = AdventureTitleLayout.originOnly(table, title);
            }
            Method withTitle = packet.getClass().getMethod("withTitle", getTitle.getReturnType());
            return (ClientboundOpenScreenPacket) withTitle.invoke(packet, laidOut);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logOnce("Java title layout is unavailable for this Geyser build; chest titles may be misplaced.", failure);
            return packet;
        }
    }

    private void logOnce(String message, Throwable failure) {
        if (failureLogged.compareAndSet(false, true)) logger.log(Level.WARNING, message, failure);
    }

    private final class Wrapper extends PacketTranslator<ClientboundOpenScreenPacket> {
        @Override public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
            // The touch layout keeps its centred native title; only glyph substitution applies there.
            var profile = session.getClientData() == null ? null : session.getClientData().getUiProfile();
            forward(session, rewrite(packet, profile != null && "POCKET".equals(profile.toString())));
        }

        @Override public boolean shouldExecuteInEventLoop() {
            return original.shouldExecuteInEventLoop();
        }
    }
}
