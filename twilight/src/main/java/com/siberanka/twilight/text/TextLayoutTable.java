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
 * the pen. {@link TextLayout} uses these numbers to reproduce Java positions with
 * invisible spacer glyphs.
 */
public final class TextLayoutTable {
    /** Pack path of the serialized table. */
    public static final String PATH = "twilight/text-layout.json";
    /** With Twilight's chest UI, the Bedrock title label starts this many GUI units left of Java's title origin. */
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
    private final int containerOrigin;
    private final Map<Integer, Float> textAdvances;

    public TextLayoutTable(Map<String, Map<Integer, Entry>> fonts, int spacerFirst, int spacerCount) {
        this(fonts, spacerFirst, spacerCount, ORIGIN);
    }

    public TextLayoutTable(Map<String, Map<Integer, Entry>> fonts, int spacerFirst, int spacerCount,
                           int containerOrigin) {
        this(fonts, spacerFirst, spacerCount, containerOrigin, Map.of());
    }

    /**
     * @param containerOrigin {@link #ORIGIN} when the pack moves the chest title label, otherwise 0
     * @param textAdvances Java advances of ordinary default-font characters (Java's own font sheets),
     *                     used only to centre lines with Java's rounding; empty when unknown
     */
    public TextLayoutTable(Map<String, Map<Integer, Entry>> fonts, int spacerFirst, int spacerCount,
                           int containerOrigin, Map<Integer, Float> textAdvances) {
        if (containerOrigin != 0 && containerOrigin != ORIGIN) throw new IllegalArgumentException("origin " + containerOrigin);
        Map<String, Map<Integer, Entry>> copy = new TreeMap<>();
        fonts.forEach((font, entries) -> copy.put(font, Collections.unmodifiableMap(new TreeMap<>(entries))));
        this.fonts = Collections.unmodifiableMap(copy);
        this.spacerFirst = spacerFirst;
        this.spacerCount = spacerCount;
        this.containerOrigin = containerOrigin;
        this.textAdvances = Collections.unmodifiableMap(new TreeMap<>(textAdvances));
    }

    /** Same metrics for a pack whose chest title label does (or does not) start left of Java's origin. */
    public TextLayoutTable withContainerOrigin(int origin) {
        return new TextLayoutTable(fonts, spacerFirst, spacerCount, origin, textAdvances);
    }

    /** Java advance of an ordinary default-font character, or NaN when unknown. */
    public float textAdvance(int codePoint) {
        if (codePoint == ' ') return 4;
        Float advance = textAdvances.get(codePoint);
        return advance == null ? Float.NaN : advance;
    }

    /** Units the Bedrock chest title label starts left of Java's title origin. */
    public int containerOrigin() { return containerOrigin; }

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
        root.addProperty("origin", containerOrigin);
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
        if (!textAdvances.isEmpty()) {
            JsonObject text = new JsonObject();
            textAdvances.forEach((codePoint, advance) ->
                    text.addProperty(Integer.toHexString(codePoint).toUpperCase(java.util.Locale.ROOT), advance));
            root.add("text_advances", text);
        }
        return root;
    }

    public static TextLayoutTable fromJson(JsonObject root) {
        int origin = root.get("origin").getAsInt();
        if (root.get("version").getAsInt() != 1 || (origin != ORIGIN && origin != 0)) {
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
        Map<Integer, Float> text = new LinkedHashMap<>();
        if (root.has("text_advances")) {
            for (var entry : root.getAsJsonObject("text_advances").entrySet()) {
                text.put(Integer.parseInt(entry.getKey(), 16), entry.getValue().getAsFloat());
            }
        }
        return new TextLayoutTable(fonts, root.get("spacer_first").getAsInt(), root.get("spacer_count").getAsInt(),
                origin, text);
    }

}
