/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * The plugin-message protocol between Twilight on a backend server and twilight-proxy.
 *
 * <p>A backend announces its exported Bedrock pack (SHA-256 and size) to the proxy; the proxy
 * requests a pack it does not have, and the backend streams it in chunks over the same player
 * connection. Every message carries an HMAC-SHA256 over its content with a key derived from a
 * secret both sides already share (an explicit setting, Velocity's forwarding secret or a
 * BungeeGuard token), plus a timestamp; requests carry a random nonce. A client can neither read
 * the messages (the proxy consumes them) nor forge or replay them. Messages are small and fixed in
 * shape; anything else is rejected before it is parsed further.
 */
public final class PackChannel {
    /** Plugin-message channel, lowercase namespace:path as modern servers require. */
    public static final String CHANNEL = "twilight:proxy";
    public static final byte VERSION = 1;
    public static final byte ANNOUNCE = 1;
    public static final byte REQUEST = 2;
    public static final byte CHUNK = 3;
    /** Pack bytes per chunk message; well below every platform's plugin-message limit. */
    public static final int CHUNK_BYTES = 30_000;
    /** No valid message is larger; bigger payloads are dropped unread. */
    public static final int MAX_MESSAGE = CHUNK_BYTES + 128;
    /** Accepted clock difference between proxy and backend. */
    public static final long MAX_AGE_MILLIS = 120_000;
    private static final int MAC_BYTES = 32;
    private static final byte[] MAGIC = {'T', 'W'};
    private static final byte[] KEY_LABEL = "twilight-proxy-pack-v1".getBytes(StandardCharsets.UTF_8);

    private PackChannel() {}

    /** A key derived from one shared secret; secrets shorter than 16 characters are refused. */
    public static final class Key {
        private final byte[] key;

        private Key(byte[] key) {
            this.key = key;
        }

        public static Key derive(String secret) {
            if (secret == null || secret.strip().length() < 16) throw new IllegalArgumentException("secret too short");
            return new Key(hmac(secret.strip().getBytes(StandardCharsets.UTF_8), KEY_LABEL));
        }

        byte[] mac(byte[] data, int length) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(key, "HmacSHA256"));
                mac.update(data, 0, length);
                return mac.doFinal();
            } catch (GeneralSecurityException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    public record Announce(long time, byte[] sha256, long size) {}

    public record Request(long time, byte[] nonce, byte[] sha256) {}

    public record Chunk(byte[] nonce, int index, int total, byte[] data) {}

    public static byte[] announce(Key key, long time, byte[] sha256, long size) {
        return sign(key, ANNOUNCE, out -> {
            out.writeLong(time);
            out.write(check(sha256, 32));
            out.writeLong(size);
        });
    }

    public static byte[] request(Key key, long time, byte[] nonce, byte[] sha256) {
        return sign(key, REQUEST, out -> {
            out.writeLong(time);
            out.write(check(nonce, 16));
            out.write(check(sha256, 32));
        });
    }

    public static byte[] chunk(Key key, byte[] nonce, int index, int total, byte[] data, int length) {
        if (length < 0 || length > CHUNK_BYTES) throw new IllegalArgumentException("chunk size");
        return sign(key, CHUNK, out -> {
            out.write(check(nonce, 16));
            out.writeInt(index);
            out.writeInt(total);
            out.writeShort(length);
            out.write(data, 0, length);
        });
    }

    /**
     * Verifies and decodes one message with any of the keys (a backend may accept several BungeeGuard
     * tokens); returns null for anything malformed, unsigned, of another version or too old.
     */
    public static Object read(byte[] message, List<Key> keys, long now) {
        if (message == null || message.length < MAGIC.length + 2 + MAC_BYTES || message.length > MAX_MESSAGE) return null;
        if (message[0] != MAGIC[0] || message[1] != MAGIC[1] || message[2] != VERSION) return null;
        int body = message.length - MAC_BYTES;
        byte[] tag = Arrays.copyOfRange(message, body, message.length);
        boolean valid = false;
        for (Key key : keys) valid |= MessageDigest.isEqual(key.mac(message, body), tag);
        if (!valid) return null;
        ByteBuffer in = ByteBuffer.wrap(message, 4, body - 4);
        try {
            return switch (message[3]) {
                case ANNOUNCE -> {
                    if (in.remaining() != 8 + 32 + 8) yield null;
                    long time = in.getLong();
                    byte[] sha = new byte[32];
                    in.get(sha);
                    long size = in.getLong();
                    yield fresh(time, now) && size > 0 ? new Announce(time, sha, size) : null;
                }
                case REQUEST -> {
                    if (in.remaining() != 8 + 16 + 32) yield null;
                    long time = in.getLong();
                    byte[] nonce = new byte[16];
                    in.get(nonce);
                    byte[] sha = new byte[32];
                    in.get(sha);
                    yield fresh(time, now) ? new Request(time, nonce, sha) : null;
                }
                case CHUNK -> {
                    if (in.remaining() < 16 + 4 + 4 + 2) yield null;
                    byte[] nonce = new byte[16];
                    in.get(nonce);
                    int index = in.getInt(), total = in.getInt(), length = Short.toUnsignedInt(in.getShort());
                    if (index < 0 || total <= 0 || index >= total || length > CHUNK_BYTES || in.remaining() != length) yield null;
                    byte[] data = new byte[length];
                    in.get(data);
                    yield new Chunk(nonce, index, total, data);
                }
                default -> null;
            };
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static boolean fresh(long time, long now) {
        return Math.abs(now - time) <= MAX_AGE_MILLIS;
    }

    @FunctionalInterface
    private interface Body {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] sign(Key key, byte type, Body body) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
            DataOutputStream out = new DataOutputStream(bytes);
            out.write(MAGIC);
            out.writeByte(VERSION);
            out.writeByte(type);
            body.write(out);
            byte[] content = bytes.toByteArray();
            out.write(key.mac(content, content.length));
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static byte[] check(byte[] value, int length) {
        if (value == null || value.length != length) throw new IllegalArgumentException("field length");
        return value;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
