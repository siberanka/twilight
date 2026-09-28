/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.display;

import com.google.gson.JsonParser;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.GeyserEntityType;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/** Isolated adapter for the pinned Geyser 2.11.3 item-display metadata schema. */
public final class GeyserDisplayBridge implements AutoCloseable {
    static final Identifier ID = Identifier.of("twilight:item_display");
    final Map<String, Integer> variants;
    final GeyserFloatEntityProperty[] from = new GeyserFloatEntityProperty[14];
    final GeyserFloatEntityProperty[] to = new GeyserFloatEntityProperty[14];
    GeyserFloatEntityProperty seconds, delay;
    GeyserIntEntityProperty revision, appearance;
    private final EventRegistrar registrar;
    private VanillaEntityType<TwilightItemDisplay> definition;

    public GeyserDisplayBridge(Object owner, Path deployedPack, Logger logger) throws IOException {
        variants = readIndex(deployedPack);
        registrar = EventRegistrar.of(owner);
        if (variants.isEmpty()) return;
        GeyserApi.api().eventBus().subscribe(registrar, GeyserDefineEntitiesEvent.class, event -> {
            if (Registries.JAVA_ENTITY_TYPES.get(GeyserEntityType.ofVanilla(EntityType.ITEM_DISPLAY)) != null) {
                logger.severe("Item-display bridge cannot start: another translator already owns item_display.");
                return;
            }
            CustomEntityDefinition custom = CustomEntityDefinition.of(ID);
            event.register(custom);
            var builder = VanillaEntityType.<TwilightItemDisplay>builder(spawn -> new TwilightItemDisplay(spawn, this))
                    .type(EntityType.ITEM_DISPLAY).bedrockDefinition((BedrockEntityDefinition) custom).heightAndWidth(0);
            // Java DisplayRenderer submits its mesh even with the base entity's
            // invisible flag set. Model providers use that flag on visible bones.
            // Living-entity flags (invisibility, swimming, fire, etc.) must not
            // be forwarded to Bedrock's custom display mesh.
            builder.addTranslator(null);
            // Base entity metadata 1..7 does not affect the display mesh.
            for (int i = 1; i < 8; i++) builder.addTranslator(null);
            builder.addTranslator(MetadataTypes.INT, (entity, data) -> entity.delay(data.getValue()));
            builder.addTranslator(MetadataTypes.INT, (entity, data) -> entity.duration(data.getValue()));
            builder.addTranslator(null); // Position interpolation is handled by Geyser movement packets.
            builder.addTranslator(MetadataTypes.VECTOR3, (entity, data) -> entity.vector(0, data.getValue()));
            builder.addTranslator(MetadataTypes.VECTOR3, (entity, data) -> entity.vector(3, data.getValue()));
            builder.addTranslator(MetadataTypes.QUATERNION, (entity, data) -> entity.rotation(6, data.getValue()));
            builder.addTranslator(MetadataTypes.QUATERNION, (entity, data) -> entity.rotation(10, data.getValue()));
            builder.addTranslator(null); // Billboard constraints.
            builder.addTranslator(null); // Brightness override.
            builder.addTranslator(MetadataTypes.FLOAT, (entity, data) -> entity.viewRange(data.getValue()));
            for (int i = 18; i <= 22; i++) builder.addTranslator(null);
            builder.addTranslator(MetadataTypes.ITEM_STACK, (entity, data) -> entity.item(data.getValue()));
            builder.addTranslator(MetadataTypes.BYTE, (entity, data) -> entity.context(data.getValue()));
            definition = builder.build();
            logger.info("Registered live item-display bridge for " + variants.size() + " converted models.");
        });
        GeyserApi.api().eventBus().subscribe(registrar, GeyserDefineEntityPropertiesEvent.class, event -> {
            if (definition == null) return;
            DisplayPose identity = DisplayPose.identity();
            for (int i = 0; i < 14; i++) {
                from[i] = event.registerFloatProperty(ID, Identifier.of("twilight:a" + i), -1_000_000, 1_000_000, identity.value(i));
                to[i] = event.registerFloatProperty(ID, Identifier.of("twilight:b" + i), -1_000_000, 1_000_000, identity.value(i));
            }
            seconds = event.registerFloatProperty(ID, Identifier.of("twilight:seconds"), 0, 3600, 0f);
            revision = event.registerIntegerProperty(ID, Identifier.of("twilight:revision"), 0, 1_000_000, 0);
            appearance = event.registerIntegerProperty(ID, Identifier.of("twilight:appearance"), -9, variants.size() * 9 - 1, -9);
            delay = event.registerFloatProperty(ID, Identifier.of("twilight:delay"), -3600, 3600, 0f);
        });
    }

    private static Map<String, Integer> readIndex(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return Map.of();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entry = zip.getEntry("twilight/display-index.json");
            if (entry == null) return Map.of();
            if (entry.getSize() > 8 * 1024 * 1024) throw new IOException("Display index exceeds size limit");
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                var object = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Integer> result = new LinkedHashMap<>();
                object.entrySet().forEach(value -> result.put(value.getKey(), value.getValue().getAsInt()));
                for (int i = 0; i < result.size(); i++) if (!result.containsValue(i)) throw new IOException("Invalid display variant index");
                return Map.copyOf(result);
            }
        }
    }

    @Override public void close() {
        GeyserApi.api().eventBus().unregisterAll(registrar);
        if (definition != null) {
            Registries.JAVA_ENTITY_TYPES.get().remove(definition.type(), definition);
            Registries.JAVA_ENTITY_IDENTIFIERS.get().remove("minecraft:item_display", definition);
        }
    }
}
