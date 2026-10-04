/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.world;

import com.google.gson.JsonParser;
import com.siberanka.twilight.world.BiomeMatcher;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Gives custom Java biomes the closest vanilla Bedrock biome instead of Geyser's fallback.
 *
 * <p>When a Java server sends its biome registry, the bridge looks at every biome Geyser
 * cannot map (datapack and plugin biomes) and registers the vanilla biome whose grass,
 * foliage, water, fog and precipitation are closest (see {@link BiomeMatcher}). The registry
 * entries carry the biome's own data, so runtime biomes such as RealisticSeasons' seasonal
 * ones are matched with their current colours. Mappings are global and never replace an
 * existing Geyser mapping.
 */
public final class GeyserBiomeBridge implements AutoCloseable {
    private static final String BIOME_REGISTRY = "minecraft:worldgen/biome";

    private final Path servedPack;
    private final Logger logger;
    private final EventRegistrar registrar;
    private final Wrapper wrapper = new Wrapper();
    private final AtomicBoolean failureLogged = new AtomicBoolean();
    private volatile BiomeMatcher matcher;
    private volatile PacketTranslator<? extends Packet> original;

    private GeyserBiomeBridge(Object owner, Path servedPack, Logger logger) {
        this.servedPack = servedPack;
        this.logger = logger;
        this.registrar = EventRegistrar.of(owner);
    }

    public static GeyserBiomeBridge create(Object owner, Path servedPack, Logger logger) {
        GeyserBiomeBridge bridge = new GeyserBiomeBridge(owner, servedPack, logger);
        var events = GeyserApi.api().eventBus();
        events.subscribe(bridge.registrar, GeyserPostInitializeEvent.class, event -> bridge.attach());
        events.subscribe(bridge.registrar, GeyserPostReloadEvent.class, event -> bridge.attach());
        if (Registries.JAVA_PACKET_TRANSLATORS.get().get(ClientboundRegistryDataPacket.class) != null) bridge.attach();
        return bridge;
    }

    private synchronized void attach() {
        try {
            matcher = readTable(servedPack);
        } catch (IOException | RuntimeException failure) {
            matcher = null;
            logger.log(Level.WARNING, "Could not read the vanilla biome table of the served Twilight pack", failure);
        }
        var translators = Registries.JAVA_PACKET_TRANSLATORS.get();
        PacketTranslator<? extends Packet> current = translators.get(ClientboundRegistryDataPacket.class);
        if (current == null) {
            logger.warning("Geyser has no registry-data translator; custom biomes keep Geyser's fallback.");
            return;
        }
        if (current != wrapper) {
            original = current;
            translators.put(ClientboundRegistryDataPacket.class, wrapper);
        }
        logger.info(matcher == null ? "Biome matching idle: the served pack has no vanilla biome table."
                : "Custom biomes are shown on Bedrock as their closest vanilla biome (" + matcher.vanilla().size()
                + " vanilla appearances).");
    }

    static BiomeMatcher readTable(Path pack) throws IOException {
        if (!Files.isRegularFile(pack)) return null;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry(BiomeMatcher.PATH);
            if (entry == null) return null;
            if (entry.getSize() > 4L * 1024 * 1024) throw new IOException("Biome table exceeds size limit");
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                return BiomeMatcher.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
    }

    @Override public synchronized void close() {
        GeyserApi.api().eventBus().unregisterAll(registrar);
        if (original != null) Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundRegistryDataPacket.class, wrapper, original);
    }

    /** Registers mappings for the unmapped biomes of a registry packet; returns custom key -> vanilla key. */
    @SuppressWarnings("unchecked")
    static Map<String, String> register(Object packet, BiomeMatcher matcher, Map<String, Integer> identifiers)
            throws ReflectiveOperationException {
        Map<String, String> added = new java.util.LinkedHashMap<>();
        if (!BIOME_REGISTRY.equals(keyString(packet.getClass().getMethod("getRegistry").invoke(packet)))) return added;
        List<String> candidates = new ArrayList<>();
        for (String key : matcher.vanilla().keySet()) if (identifiers.containsKey(key)) candidates.add(key);
        java.util.Set<String> usable = java.util.Set.copyOf(candidates);
        for (Object entry : (List<?>) packet.getClass().getMethod("getEntries").invoke(packet)) {
            String key = keyString(entry.getClass().getMethod("getId").invoke(entry));
            Object data = entry.getClass().getMethod("getData").invoke(entry);
            if (key == null || identifiers.containsKey(key) || !(data instanceof Map<?, ?> nbt)) continue;
            String closest = matcher.closest(matcher.appearance((Map<String, ?>) nbt), usable);
            if (closest == null) continue;
            identifiers.put(key, identifiers.get(closest));
            added.put(key, closest);
        }
        return added;
    }

    private static String keyString(Object key) throws ReflectiveOperationException {
        if (key == null) return null;
        Method asString = key.getClass().getMethod("asString");
        asString.setAccessible(true);
        return (String) asString.invoke(key);
    }

    private final class Wrapper extends PacketTranslator<ClientboundRegistryDataPacket> {
        @SuppressWarnings("unchecked")
        @Override public void translate(GeyserSession session, ClientboundRegistryDataPacket packet) {
            BiomeMatcher current = matcher;
            if (current != null) {
                try {
                    // Geyser's map is a (relocated) fastutil Object2IntMap, which is also a java.util.Map.
                    // Reflection avoids a compile-time cast to the unrelocated fastutil type.
                    Object registry = Registries.BIOME_IDENTIFIERS;
                    Map<String, Integer> identifiers = (Map<String, Integer>) registry.getClass().getMethod("get").invoke(registry);
                    Map<String, String> added;
                    synchronized (identifiers) {
                        added = register(packet, current, identifiers);
                    }
                    if (!added.isEmpty()) {
                        logger.info("Bedrock biome mapping: " + added.size() + " custom biome(s), e.g. "
                                + added.entrySet().iterator().next());
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                    if (failureLogged.compareAndSet(false, true)) {
                        logger.log(Level.WARNING, "Custom biome matching failed; such biomes keep Geyser's fallback.", failure);
                    }
                }
            }
            ((PacketTranslator<ClientboundRegistryDataPacket>) original).translate(session, packet);
        }

        @Override public boolean shouldExecuteInEventLoop() {
            return original.shouldExecuteInEventLoop();
        }
    }
}
