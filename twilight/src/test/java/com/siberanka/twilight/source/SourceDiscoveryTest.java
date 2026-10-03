package com.siberanka.twilight.source;

import com.siberanka.twilight.config.TwilightConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceDiscoveryTest {
    @TempDir Path root;

    @Test
    void disablingAutomaticDiscoveryStillHonorsExplicitSources() throws Exception {
        Path automatic = Files.createDirectories(root.resolve("plugins/ItemsAdder/output"));
        Files.writeString(automatic.resolve("pack.mcmeta"), "{}");
        Path datapack = Files.createDirectories(root.resolve("world/datapacks/example"));
        Files.writeString(datapack.resolve("pack.mcmeta"), "{}");
        Path explicit = Files.createDirectories(root.resolve("explicit"));
        Files.writeString(explicit.resolve("pack.mcmeta"), "{}");

        TwilightConfig config = new TwilightConfig(false, true, false, true, 40, 100, 10_000_000, 1000,
                true, false, List.of(explicit), "auto", false, false, 3);
        List<ContentSource> sources = new SourceDiscovery(root, config)
                .discover(Set.of(root.resolve("world")));

        assertEquals(List.of(explicit.toRealPath()), sources.stream().map(ContentSource::path).toList());
    }

    @Test
    void providerAuthoredAssetsOverrideCachesAndGeneratedPackIsOnlyFallback() throws Exception {
        Path provider = Files.createDirectories(root.resolve("plugins/ItemsAdder"));
        Path generated = Files.createDirectories(provider.resolve("output"));
        Files.write(generated.resolve("generated.zip"), emptyZip());
        Path cache = Files.createDirectories(provider.resolve("storage/cache/resource_pack/assets/demo"));
        Files.writeString(cache.resolve("cached.json"), "{}");
        Files.createDirectories(cache.resolve("models/item"));
        Files.writeString(cache.resolve("models/item/shared.json"), "{\"layer\":\"cache\"}");
        Path data = Files.createDirectories(provider.resolve("data/resource_pack/assets/demo"));
        Files.writeString(data.resolve("data.json"), "{}");
        Files.createDirectories(data.resolve("models/item"));
        Files.writeString(data.resolve("models/item/shared.json"), "{\"layer\":\"data\"}");
        Path contents = Files.createDirectories(provider.resolve("contents/example/resourcepack/assets/demo"));
        Files.writeString(contents.resolve("authored.json"), "{}");
        Files.createDirectories(contents.resolve("models/item"));
        Files.writeString(contents.resolve("models/item/shared.json"), "{\"layer\":\"contents\"}");
        Files.createDirectories(provider.resolve("contents/example/configs"));
        Files.writeString(provider.resolve("contents/example/configs/items.yml"), "info: {}\n");

        TwilightConfig config = new TwilightConfig(false, true, true, true, 40, 100, 10_000_000, 1000,
                true, true, List.of(), "auto", false, false, 3);
        List<ContentSource> sources = new SourceDiscovery(root, config).discover(Set.of());

        ContentSource generatedSource = source(sources, "generated.zip");
        ContentSource cachedSource = source(sources, "resource_pack", "storage");
        ContentSource dataSource = source(sources, "resource_pack", "data");
        ContentSource authoredSource = source(sources, "resourcepack", "contents");
        assertTrue(generatedSource.priority() < cachedSource.priority());
        assertTrue(cachedSource.priority() < dataSource.priority());
        assertTrue(dataSource.priority() < authoredSource.priority());
        assertTrue(sources.stream().anyMatch(source -> source.kind() == ContentSource.Kind.PROVIDER_DATA &&
                source.path().getFileName().toString().equalsIgnoreCase("contents")));
        try (ResourceIndex resources = ResourceIndex.build(sources, config)) {
            assertEquals("{\"layer\":\"contents\"}", resources.find("assets/demo/models/item/shared.json")
                    .orElseThrow().readUtf8());
            assertTrue(resources.find("provider-data/itemsadder/contents/example/configs/items.yml").isPresent());
        }
    }

    @Test
    void ignoresVanillaAssetCopiesTemporaryBuildsAndStaleNestedPacks() throws Exception {
        // Real ItemsAdder folders: storage/cache/vanilla_assets/<version> holds the vanilla client assets,
        // storage/cache/tmp a leftover build folder, data/resource_pack/pack.zip an old self-hosted pack.
        Path provider = Files.createDirectories(root.resolve("plugins/ItemsAdder"));
        Files.createDirectories(provider.resolve("output"));
        Files.write(provider.resolve("output/generated.zip"), emptyZip());
        for (String stale : List.of("storage/cache/vanilla_assets/26.2", "storage/cache/tmp/resource_pack")) {
            Path pack = Files.createDirectories(provider.resolve(stale).resolve("assets/minecraft/font"));
            Files.writeString(pack.resolve("default.json"), "{\"providers\":[]}");
            Files.writeString(provider.resolve(stale).resolve("pack.mcmeta"), "{}");
        }
        Path working = Files.createDirectories(provider.resolve("data/resource_pack/assets/demo"));
        Files.writeString(working.getParent().getParent().resolve("pack.mcmeta"), "{}");
        Files.write(working.getParent().getParent().resolve("pack.zip"), emptyZip());

        TwilightConfig config = new TwilightConfig(false, true, true, true, 40, 100, 10_000_000, 1000,
                true, true, List.of(), "auto", false, false, 3);
        List<ContentSource> sources = new SourceDiscovery(root, config).discover(Set.of());
        Path real = root.toRealPath();
        assertTrue(sources.stream().noneMatch(source -> real.relativize(source.path()).toString().contains("vanilla_assets")), sources.toString());
        assertTrue(sources.stream().noneMatch(source -> real.relativize(source.path()).toString().contains("tmp")), sources.toString());
        assertTrue(sources.stream().noneMatch(source -> source.path().getFileName().toString().equals("pack.zip")), sources.toString());
        assertTrue(sources.stream().anyMatch(source -> source.path().getFileName().toString().equals("generated.zip")));
        assertTrue(sources.stream().anyMatch(source -> source.path().endsWith(Path.of("data", "resource_pack"))));
    }

    @Test
    void usesTheNewestRenamedItemsAdderPackWhenGeneratedZipIsAbsent() throws Exception {
        Path output = Files.createDirectories(root.resolve("plugins/ItemsAdder/output"));
        Files.write(output.resolve("server-pack-1.18.zip"), emptyZip());
        Files.write(output.resolve("generated_1.21.x.zip"), emptyZip());
        Files.setLastModifiedTime(output.resolve("server-pack-1.18.zip"), java.nio.file.attribute.FileTime.fromMillis(1_000_000L));
        Files.setLastModifiedTime(output.resolve("generated_1.21.x.zip"), java.nio.file.attribute.FileTime.fromMillis(2_000_000L));
        TwilightConfig config = new TwilightConfig(false, true, true, true, 40, 100, 10_000_000, 1000,
                true, true, List.of(), "auto", false, false, 3);
        List<ContentSource> sources = new SourceDiscovery(root, config).discover(Set.of());
        assertEquals(List.of("generated_1.21.x.zip"), sources.stream().map(source -> source.path().getFileName().toString())
                .filter(name -> name.endsWith(".zip")).toList());

        Files.write(output.resolve("generated.zip"), emptyZip());
        List<ContentSource> standard = new SourceDiscovery(root, config).discover(Set.of());
        assertEquals(List.of("generated.zip"), standard.stream().map(source -> source.path().getFileName().toString())
                .filter(name -> name.endsWith(".zip")).toList());
    }

    private static ContentSource source(List<ContentSource> sources, String fileName, String... ancestor) {
        return sources.stream().filter(source -> source.path().getFileName().toString().equalsIgnoreCase(fileName))
                .filter(source -> ancestor.length == 0 || java.util.stream.StreamSupport.stream(source.path().spliterator(), false)
                        .anyMatch(part -> part.toString().equalsIgnoreCase(ancestor[0])))
                .findFirst().orElseThrow();
    }

    private static byte[] emptyZip() throws Exception {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) { }
        return bytes.toByteArray();
    }
}
