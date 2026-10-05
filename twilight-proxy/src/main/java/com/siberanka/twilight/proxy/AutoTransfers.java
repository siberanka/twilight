/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import com.siberanka.twilight.protocol.PackChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Receives the packs Twilight on the backends announces. Only signed messages count; a transfer
 * must match the nonce of this proxy's own request, arrive in order and match the announced size
 * and SHA-256, otherwise it is dropped and its partial file deleted. At most a few transfers run
 * at once and an idle one is abandoned, so a broken or hostile backend cannot fill memory or disk.
 */
final class AutoTransfers {
    private static final int MAX_ACTIVE = 4;
    private static final long IDLE_MILLIS = 30_000;

    private final Platform platform;
    private final PackStore store;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Active> active = new HashMap<>();
    private volatile List<PackChannel.Key> keys = List.of();

    AutoTransfers(Platform platform, PackStore store) {
        this.platform = platform;
        this.store = store;
    }

    void keys(List<PackChannel.Key> keys) {
        this.keys = List.copyOf(keys);
    }

    boolean enabled() {
        return !keys.isEmpty();
    }

    /**
     * A message from backend {@code server}; returns the request to send back to that server, or null.
     * Never throws for malformed input.
     */
    byte[] onMessage(String server, byte[] message, long maxBytes) {
        List<PackChannel.Key> current = keys;
        if (current.isEmpty() || !store.auto(server)) return null;
        Object decoded = PackChannel.read(message, current, System.currentTimeMillis());
        if (decoded instanceof PackChannel.Announce announce) return onAnnounce(server.toLowerCase(Locale.ROOT), announce, maxBytes, current);
        if (decoded instanceof PackChannel.Chunk chunk) onChunk(server.toLowerCase(Locale.ROOT), chunk);
        return null;
    }

    private synchronized byte[] onAnnounce(String server, PackChannel.Announce announce, long maxBytes,
                                           List<PackChannel.Key> current) {
        var have = store.pack(server);
        if (have.isPresent() && MessageDigest.isEqual(have.get().sha256(), announce.sha256())) return null;
        Active running = active.get(server);
        if (running != null && !running.idle()) return null;
        if (running != null) drop(server, running);
        if (announce.size() > maxBytes) {
            platform.warn("The Bedrock pack of " + server + " (" + announce.size() / 1048576 + " MiB) exceeds max-pack-size-mb.", null);
            return null;
        }
        if (active.size() >= MAX_ACTIVE) return null;
        try {
            byte[] nonce = new byte[16];
            random.nextBytes(nonce);
            int total = (int) ((announce.size() + PackChannel.CHUNK_BYTES - 1) / PackChannel.CHUNK_BYTES);
            Path file = store.temporaryFile(server);
            active.put(server, new Active(nonce, announce.sha256(), announce.size(), total, file,
                    FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)));
            return PackChannel.request(current.getFirst(), System.currentTimeMillis(), nonce, announce.sha256());
        } catch (IOException failure) {
            platform.warn("Could not prepare the Bedrock pack transfer of " + server, failure);
            return null;
        }
    }

    private synchronized void onChunk(String server, PackChannel.Chunk chunk) {
        Active transfer = active.get(server);
        if (transfer == null || !MessageDigest.isEqual(transfer.nonce, chunk.nonce())) return;
        if (chunk.index() != transfer.next || chunk.total() != transfer.total) {
            drop(server, transfer);
            return;
        }
        long expected = Math.min(PackChannel.CHUNK_BYTES, transfer.size - (long) transfer.next * PackChannel.CHUNK_BYTES);
        if (chunk.data().length != expected) {
            drop(server, transfer);
            return;
        }
        try {
            ByteBuffer data = ByteBuffer.wrap(chunk.data());
            while (data.hasRemaining()) transfer.channel.write(data);
            transfer.digest.update(chunk.data());
            transfer.next++;
            transfer.touched = System.currentTimeMillis();
            if (transfer.next < transfer.total) return;
            active.remove(server);
            transfer.channel.close();
            if (!MessageDigest.isEqual(transfer.digest.digest(), transfer.sha256)) throw new IOException("checksum mismatch");
            Path file = transfer.file;
            platform.async(() -> {
                try {
                    store.acceptAuto(server, file);
                } catch (IOException failure) {
                    platform.warn("Refused the Bedrock pack of " + server + ": " + failure.getMessage(), null);
                    delete(file);
                }
            });
        } catch (IOException failure) {
            platform.warn("The Bedrock pack transfer of " + server + " failed: " + failure.getMessage(), null);
            drop(server, transfer);
        }
    }

    /** Abandons transfers that stopped (player left, backend restarted). */
    synchronized void sweep() {
        for (Iterator<Map.Entry<String, Active>> iterator = active.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Active> entry = iterator.next();
            if (entry.getValue().idle()) {
                iterator.remove();
                close(entry.getValue());
            }
        }
    }

    synchronized void close() {
        active.values().forEach(this::close);
        active.clear();
    }

    private void drop(String server, Active transfer) {
        active.remove(server, transfer);
        close(transfer);
    }

    private void close(Active transfer) {
        try { transfer.channel.close(); } catch (IOException ignored) { }
        delete(transfer.file);
    }

    private static void delete(Path file) {
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }

    private static final class Active {
        final byte[] nonce;
        final byte[] sha256;
        final long size;
        final int total;
        final Path file;
        final FileChannel channel;
        final MessageDigest digest;
        int next;
        long touched = System.currentTimeMillis();

        Active(byte[] nonce, byte[] sha256, long size, int total, Path file, FileChannel channel) {
            this.nonce = nonce;
            this.sha256 = sha256;
            this.size = size;
            this.total = total;
            this.file = file;
            this.channel = channel;
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }

        boolean idle() {
            return System.currentTimeMillis() - touched > IDLE_MILLIS;
        }
    }
}
