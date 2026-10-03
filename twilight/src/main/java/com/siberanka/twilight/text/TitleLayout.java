/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites a container title so Bedrock draws every character where Java does.
 *
 * <p>The Bedrock label starts {@link TextLayoutTable#ORIGIN} units left of Java's
 * title origin. The layout tracks {@code delta}, Bedrock's pen minus Java's pen
 * shifted by the origin. Ordinary characters are assumed to have equal widths on
 * both clients and need {@code delta == 0}. A bitmap glyph needs
 * {@code delta == left - 1}, because Bedrock draws its first inked column one unit
 * after the pen while Java draws column zero at the pen. Advance-only characters
 * (spaces, negative shifts) move only Java's pen. Gaps are filled with invisible
 * spacers. Bedrock cannot advance by exactly one unit, so a one-unit gap after a
 * glyph uses that glyph's wide variant (one invisible trailing column). Remaining
 * backwards or one-unit moves are counted as approximations and left unchanged.
 */
public final class TitleLayout {
    /** Title text in render order; {@code text == null} marks a component Twilight cannot measure. */
    public record Segment(String text, String font) {}

    /**
     * @param texts replacement text per segment; for an opaque segment, the spacers that must precede it
     */
    public record Result(List<String> texts, int customCharacters, int approximations) {}

    private final TextLayoutTable table;
    private final List<StringBuilder> outputs = new ArrayList<>();
    private double delta = -TextLayoutTable.ORIGIN;
    private int approximations;
    private int customCharacters;
    // The most recent output is a glyph that may still be widened by one unit.
    private StringBuilder lastGlyphOutput;
    private int lastGlyphIndex = -1;
    private int lastGlyphWide = -1;

    private TitleLayout(TextLayoutTable table) {
        this.table = table;
    }

    public static Result layout(TextLayoutTable table, List<Segment> segments) {
        TitleLayout layout = new TitleLayout(table);
        for (Segment segment : segments) {
            StringBuilder out = new StringBuilder();
            layout.outputs.add(out);
            if (segment.text() == null) layout.align(out, 0);
            else layout.text(out, segment.text(), segment.font());
        }
        List<String> texts = layout.outputs.stream().map(StringBuilder::toString).toList();
        return new Result(texts, layout.customCharacters, layout.approximations);
    }

    /**
     * Glyph substitution without positioning, for Bedrock's touch (pocket) layout whose title
     * label is centred and unrelated to Java's geometry: visible glyphs use their Bedrock code
     * points and Java-only spacing characters are dropped.
     */
    public static Result substitute(TextLayoutTable table, List<Segment> segments) {
        List<String> texts = new ArrayList<>();
        int custom = 0;
        for (Segment segment : segments) {
            if (segment.text() == null) { texts.add(""); continue; }
            StringBuilder out = new StringBuilder();
            for (int codePoint : segment.text().codePoints().toArray()) {
                TextLayoutTable.Entry entry = table.lookup(segment.font(), codePoint);
                if (entry == null) out.appendCodePoint(codePoint);
                else {
                    custom++;
                    if (entry.visible()) out.appendCodePoint(entry.bedrock());
                }
            }
            texts.add(out.toString());
        }
        return new Result(texts, custom, 0);
    }

    /** Single default-font text convenience. */
    public static Result layout(TextLayoutTable table, String text) {
        return layout(table, List.of(new Segment(text, null)));
    }

    private void text(StringBuilder out, String content, String font) {
        content.codePoints().forEach(codePoint -> {
            TextLayoutTable.Entry entry = table.lookup(font, codePoint);
            if (entry == null && codePoint == ' ' && (font == null || TextLayoutTable.DEFAULT_FONT.equals(font))) {
                entry = TextLayoutTable.Entry.advanceOnly(4); // Java's vanilla space provider
            }
            if (entry == null) {
                align(out, 0);
                out.appendCodePoint(codePoint);
                forgetGlyph();
            } else if (!entry.visible()) {
                delta -= entry.advance();
                if (codePoint != ' ') customCharacters++;
            } else {
                customCharacters++;
                align(out, entry.left() - 1);
                lastGlyphOutput = out;
                lastGlyphIndex = out.length();
                lastGlyphWide = entry.wide();
                out.appendCodePoint(entry.bedrock());
                delta += entry.width() + 1 - entry.advance();
            }
        });
    }

    private void forgetGlyph() {
        lastGlyphOutput = null;
        lastGlyphWide = -1;
    }

    private void align(StringBuilder out, double required) {
        int gap = (int) Math.round(required - delta);
        if (gap == 1 && lastGlyphOutput != null && lastGlyphWide >= 0) {
            int length = Character.charCount(lastGlyphOutput.codePointAt(lastGlyphIndex));
            lastGlyphOutput.replace(lastGlyphIndex, lastGlyphIndex + length, Character.toString(lastGlyphWide));
            delta += 1;
            gap = 0;
        }
        if (gap == 0) return;
        forgetGlyph();
        if (gap < table.minimumSpacer()) {
            approximations++;
            return;
        }
        delta += gap;
        int maximum = table.maximumSpacer();
        while (gap > maximum) {
            int step = gap - maximum >= table.minimumSpacer() ? maximum : maximum - table.minimumSpacer();
            out.appendCodePoint(table.spacer(step));
            gap -= step;
        }
        out.appendCodePoint(table.spacer(gap));
    }
}
