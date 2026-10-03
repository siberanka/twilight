/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Java font metrics and their Bedrock representation, generated with the pack.
 *
 * <p>An entry is either a visible bitmap glyph, drawn on Bedrock by {@code bedrock},
 * or an advance-only character such as a space provider, a negative-height bitmap
 * or an empty TrueType glyph. Bedrock draws a glyph's first inked column one unit
 * after the pen and advances by its inked width plus one; Java draws column zero at
 * the pen. {@link TitleLayout} uses these numbers to reproduce Java positions with
 * invisible spacer glyphs.
 */
public final class TextLayoutTable {
    /** Pack path of the serialized table. */
    public static final String PATH = "twilight/text-layout.json";
    /** The Bedrock title label starts this many GUI units left of Java's title origin. */
    public static final int ORIGIN = 256;
    public static final String DEFAULT_FONT = "minecraft:default";

    /**
     * @param bedrock Bedrock code point, or -1 for an advance-only character
     * @param wide variant with one invisible trailing column (advance + 1), or -1
     * @param left first inked column in the Bedrock cell (Java draws it at pen + left)
     * @param width inked width on Bedrock
     * @param advance Java advance in GUI units
     */
    public record Entry(int bedrock, int wide, int left, int width, float advance) {
        public static Entry advanceOnly(float advance) { return new Entry(-1, -1, 0, 0, advance); }

        public boolean visible() { return bedrock >= 0; }
    }

    private final Map<String, Map<Integer, Entry>> fonts;
    private final int spacerFirst;
    private final int spacerCount;

    public TextLayoutTable(Map<String, Map<Integer, Entry>> fonts, int spacerFirst, int spacerCount) {
        Map<String, Map<Integer, Entry>> copy = new TreeMap<>();
        fonts.forEach((font, entries) -> copy.put(font, Collections.unmodifiableMap(new TreeMap<>(entries))));
        this.fonts = Collections.unmodifiableMap(copy);
        this.spacerFirst = spacerFirst;
        this.spacerCount = spacerCount;
    }

    public Entry lookup(String font, int codePoint) {
        Map<Integer, Entry> entries = fonts.get(font == null ? DEFAULT_FONT : font);
        return entries == null ? null : entries.get(codePoint);
    }

    public boolean hasFont(String font) { return fonts.containsKey(font); }
    public Map<String, Map<Integer, Entry>> fonts() { return fonts; }
    /** Smallest spacer advance; a spacer with inked width w advances w + 1. */
    public int minimumSpacer() { return 2; }
    public int maximumSpacer() { return spacerCount + 1; }

    /** Code point of an invisible spacer with exactly this Bedrock advance. */
    public int spacer(int advance) {
        if (advance < minimumSpacer() || advance > maximumSpacer()) throw new IllegalArgumentException("spacer " + advance);
        return spacerFirst + advance - minimumSpacer();
    }

    public int entryCount() { return fonts.values().stream().mapToInt(Map::size).sum(); }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("origin", ORIGIN);
        root.addProperty("spacer_first", spacerFirst);
        root.addProperty("spacer_count", spacerCount);
        JsonObject fontsJson = new JsonObject();
        fonts.forEach((font, entries) -> {
            JsonObject entriesJson = new JsonObject();
            entries.forEach((codePoint, entry) -> {
                JsonArray value = new JsonArray();
                if (entry.visible()) {
                    value.add(entry.bedrock());
                    value.add(entry.wide());
                    value.add(entry.left());
                    value.add(entry.width());
                }
                value.add(entry.advance());
                entriesJson.add(Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT), value);
            });
            fontsJson.add(font, entriesJson);
        });
        root.add("fonts", fontsJson);
        return root;
    }

    public static TextLayoutTable fromJson(JsonObject root) {
        if (root.get("version").getAsInt() != 1 || root.get("origin").getAsInt() != ORIGIN) {
            throw new IllegalArgumentException("Unsupported text layout table");
        }
        Map<String, Map<Integer, Entry>> fonts = new LinkedHashMap<>();
        for (var font : root.getAsJsonObject("fonts").entrySet()) {
            Map<Integer, Entry> entries = new LinkedHashMap<>();
            for (var entry : font.getValue().getAsJsonObject().entrySet()) {
                JsonArray value = entry.getValue().getAsJsonArray();
                int codePoint = Integer.parseInt(entry.getKey(), 16);
                if (value.size() == 5) {
                    entries.put(codePoint, new Entry(value.get(0).getAsInt(), value.get(1).getAsInt(),
                            value.get(2).getAsInt(), value.get(3).getAsInt(), value.get(4).getAsFloat()));
                } else {
                    entries.put(codePoint, Entry.advanceOnly(value.get(0).getAsFloat()));
                }
            }
            fonts.put(font.getKey(), entries);
        }
        return new TextLayoutTable(fonts, root.get("spacer_first").getAsInt(), root.get("spacer_count").getAsInt());
    }

}
