package com.siberanka.twilight.proxy;

import com.siberanka.twilight.protocol.PackChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Configuration, pack checks and the signed pack transfer between a backend and the proxy. */
class ProxyTest {
    private static final PackChannel.Key KEY = PackChannel.Key.derive("a-shared-secret-of-enough-length");
    @TempDir Path root;

    @Test
    void parsesTheDocumentedConfiguration() throws Exception {
        String defaults;
        try (var in = ProxyConfig.class.getResourceAsStream("/proxy-config.yml")) {
            defaults = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        ProxyConfig config = ProxyConfig.parse(defaults.replace("#   smp: https://example.com/packs/smp.mcpack", "    smp: https://example.com/packs/smp.mcpack")
                .replace("#   survival: survival.zip", "    survival: survival.zip"));
        assertInstanceOf(ProxyConfig.PackSource.Auto.class, config.source("lobby"));
        assertInstanceOf(ProxyConfig.PackSource.Url.class, config.source("SMP"));
        assertEquals(new ProxyConfig.PackSource.File("survival.zip"), config.source("survival"));
        assertInstanceOf(ProxyConfig.PackSource.Auto.class, config.source("unlisted"));
        assertTrue(config.transferOnSwitch());
        assertEquals(256L * 1024 * 1024, config.maxPackBytes());
    }

    @Test
    void refusesUnsafeOrMalformedSettings() {
        for (String value : List.of("../../secret.zip", "/etc/passwd", "C:\\x.zip", "ftp://host/x.mcpack", "file:///x", "https://user:pw@host/x")) {
            assertThrows(IllegalArgumentException.class, () -> ProxyConfig.parse("packs:\n  server:\n    a: \"" + value + "\"\n"), value);
        }
        assertThrows(IllegalArgumentException.class, () -> ProxyConfig.parse("packs:\n  server:\n    a: auto\n    a: none\n"));
        assertThrows(IllegalArgumentException.class, () -> ProxyConfig.parse("packs:\n  - list\n"));
        assertThrows(IllegalArgumentException.class, () -> ProxyConfig.parse("packs:\n\tserver: x\n"));
        assertThrows(IllegalArgumentException.class, () -> ProxyConfig.parse("packs:\n  default: auto\nmax-pack-size-mb: 99999\n"));
    }

    @Test
    void checksPackArchivesBeforeUse() throws Exception {
        Path good = zip("good.mcpack", "manifest.json", "{}");
        assertEquals(Files.size(good), PackFiles.inspect(good, 1 << 20).size());
        assertThrows(IOException.class, () -> PackFiles.inspect(zip("nomanifest.zip", "pack/manifest.json", "{}"), 1 << 20));
        assertThrows(IOException.class, () -> PackFiles.inspect(zip("escape.zip", "manifest.json", "{}", "../evil.txt", "x"), 1 << 20));
        assertThrows(IOException.class, () -> PackFiles.inspect(good, 10));
        Path notZip = root.resolve("text.mcpack");
        Files.writeString(notZip, "not a zip");
        assertThrows(IOException.class, () -> PackFiles.inspect(notZip, 1 << 20));
        assertEquals("lobby_1", PackFiles.safeName("Lobby 1"));
        assertEquals("_..", PackFiles.safeName(".."));
    }

    @Test
    void signedMessagesRejectTamperingWrongKeysAndOldTimestamps() {
        long now = System.currentTimeMillis();
        byte[] sha = new byte[32];
        byte[] announce = PackChannel.announce(KEY, now, sha, 1234);
        assertInstanceOf(PackChannel.Announce.class, PackChannel.read(announce, List.of(KEY), now));
        byte[] tampered = announce.clone();
        tampered[10] ^= 1;
        assertNull(PackChannel.read(tampered, List.of(KEY), now));
        assertNull(PackChannel.read(announce, List.of(PackChannel.Key.derive("another-secret-entirely")), now));
        assertNull(PackChannel.read(announce, List.of(KEY), now + PackChannel.MAX_AGE_MILLIS + 1));
        assertNull(PackChannel.read(new byte[PackChannel.MAX_MESSAGE + 1], List.of(KEY), now));
        assertNull(PackChannel.read(new byte[3], List.of(KEY), now));
        assertThrows(IllegalArgumentException.class, () -> PackChannel.Key.derive("short"));
    }

    @Test
    void receivesAnnouncedPacksOnlyWhenEveryChunkMatches() throws Exception {
        Path pack = zip("source.mcpack", "manifest.json", "{}", "big.bin", randomText(140_000));
        byte[] bytes = Files.readAllBytes(pack);
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(bytes);
        TestPlatform platform = new TestPlatform(root.resolve("proxy"));
        Files.createDirectories(platform.dataDirectory());
        Files.writeString(platform.dataDirectory().resolve("config.yml"), "packs:\n  default: auto\n");
        PackStore store = new PackStore(platform);
        ProxyConfig config = ProxyConfig.load(platform.dataDirectory().resolve("config.yml"));
        store.reload(config, List.of("lobby"));
        AutoTransfers transfers = new AutoTransfers(platform, store);
        long max = config.maxPackBytes();
        // Without a shared secret nothing is accepted.
        assertNull(transfers.onMessage("lobby", PackChannel.announce(KEY, System.currentTimeMillis(), sha, bytes.length), max));
        transfers.keys(List.of(KEY));

        byte[] request = transfers.onMessage("lobby", PackChannel.announce(KEY, System.currentTimeMillis(), sha, bytes.length), max);
        PackChannel.Request decoded = (PackChannel.Request) PackChannel.read(request, List.of(KEY), System.currentTimeMillis());
        assertArrayEquals(sha, decoded.sha256());
        // A second announce while the transfer runs asks for nothing.
        assertNull(transfers.onMessage("lobby", PackChannel.announce(KEY, System.currentTimeMillis(), sha, bytes.length), max));

        int total = (bytes.length + PackChannel.CHUNK_BYTES - 1) / PackChannel.CHUNK_BYTES;
        byte[] wrongNonce = new byte[16];
        transfers.onMessage("lobby", PackChannel.chunk(KEY, wrongNonce, 0, total, bytes, PackChannel.CHUNK_BYTES), max);
        for (int index = 0; index < total; index++) {
            int length = Math.min(PackChannel.CHUNK_BYTES, bytes.length - index * PackChannel.CHUNK_BYTES);
            byte[] data = Arrays.copyOfRange(bytes, index * PackChannel.CHUNK_BYTES, index * PackChannel.CHUNK_BYTES + length);
            assertNull(transfers.onMessage("lobby", PackChannel.chunk(KEY, decoded.nonce(), index, total, data, length), max));
        }
        platform.runAsync();
        assertArrayEquals(sha, store.pack("LOBBY").orElseThrow().sha256());
        // The same pack is not requested again.
        assertNull(transfers.onMessage("lobby", PackChannel.announce(KEY, System.currentTimeMillis(), sha, bytes.length), max));
        try (var files = Files.list(platform.dataDirectory().resolve("cache"))) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().endsWith(".part")));
        }
    }

    @Test
    void outOfOrderChunksAbortTheTransfer() throws Exception {
        TestPlatform platform = new TestPlatform(root.resolve("proxy2"));
        Files.createDirectories(platform.dataDirectory());
        Files.writeString(platform.dataDirectory().resolve("config.yml"), "packs:\n  default: auto\n");
        PackStore store = new PackStore(platform);
        ProxyConfig config = ProxyConfig.load(platform.dataDirectory().resolve("config.yml"));
        store.reload(config, List.of("lobby"));
        AutoTransfers transfers = new AutoTransfers(platform, store);
        transfers.keys(List.of(KEY));
        byte[] sha = new byte[32];
        byte[] request = transfers.onMessage("lobby", PackChannel.announce(KEY, System.currentTimeMillis(), sha, 70_000), config.maxPackBytes());
        PackChannel.Request decoded = (PackChannel.Request) PackChannel.read(request, List.of(KEY), System.currentTimeMillis());
        byte[] data = new byte[PackChannel.CHUNK_BYTES];
        transfers.onMessage("lobby", PackChannel.chunk(KEY, decoded.nonce(), 1, 3, data, data.length), config.maxPackBytes());
        try (var files = Files.list(platform.dataDirectory().resolve("cache"))) {
            assertEquals(0, files.count(), "the partial file is deleted");
        }
        assertTrue(store.pack("lobby").isEmpty());
    }

    @Test
    void findsTheVelocityForwardingSecret() throws Exception {
        Path proxy = Files.createDirectories(root.resolve("velocity"));
        Files.writeString(proxy.resolve("velocity.toml"), "player-info-forwarding-mode = \"modern\"\nforwarding-secret-file = \"forwarding.secret\"\n[servers]\nlobby = \"127.0.0.1:30066\"\n");
        Files.writeString(proxy.resolve("forwarding.secret"), "  abcdefghijklmnop1234  \n");
        assertEquals(List.of("abcdefghijklmnop1234"), ProxySecrets.discover(proxy, true, ""));
        Files.writeString(proxy.resolve("velocity.toml"), "player-info-forwarding-mode = \"legacy\"\n");
        assertTrue(ProxySecrets.discover(proxy, true, "").isEmpty());
        Files.writeString(proxy.resolve("velocity.toml"), "player-info-forwarding-mode = \"modern\"\nforwarding-secret-file = \"../outside.secret\"\n");
        Files.writeString(root.resolve("outside.secret"), "should-never-be-read-1234");
        assertTrue(ProxySecrets.discover(proxy, true, "").isEmpty(), "secret files outside the proxy are ignored");
    }

    private static String randomText(int length) {
        byte[] bytes = new byte[length / 2];
        new java.util.Random(7).nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private Path zip(String name, String... entries) throws IOException {
        Path file = root.resolve(name);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int index = 0; index < entries.length; index += 2) {
                zip.putNextEntry(new ZipEntry(entries[index]));
                zip.write(entries[index + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }

    private static final class TestPlatform implements Platform {
        private final Path data;
        private final List<Runnable> queued = new ArrayList<>();

        TestPlatform(Path data) { this.data = data; }

        void runAsync() {
            List<Runnable> tasks = new ArrayList<>(queued);
            queued.clear();
            tasks.forEach(Runnable::run);
        }

        @Override public Path dataDirectory() { return data; }
        @Override public Path proxyRoot() { return data.getParent(); }
        @Override public boolean velocity() { return true; }
        @Override public Collection<String> serverNames() { return List.of("lobby"); }
        @Override public String defaultServer() { return "lobby"; }
        @Override public void info(String message) { }
        @Override public void warn(String message, Throwable failure) { }
        @Override public void async(Runnable task) { queued.add(task); }
        @Override public void repeat(Runnable task, long periodSeconds) { }
    }
}
