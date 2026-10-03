package com.siberanka.twilight.compiler;

import com.google.gson.JsonParser;
import com.siberanka.twilight.config.TwilightConfig;
import com.siberanka.twilight.source.ContentSource;
import com.siberanka.twilight.text.TextLayoutTable;
import com.siberanka.twilight.text.TitleLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java/Bedrock title layout. Bedrock rules measured on 1.26.5203 (3 October 2026):
 * a glyph's first column with alpha above zero is drawn one unit after the pen, the
 * advance is the inked width plus one, alpha-1 pixels count as ink and a fully
 * transparent glyph advances four units.
 */
class TextLayoutTest {
    private static final int H_WIDTH = 6; // ordinary characters keep native widths on both clients
    @TempDir Path root;

    @Test
    void negativeHeightBitmapsBecomeJavaAdvancesAndRemappedGlyphsLandOnJavaPixels() throws Exception {
        // Real menu idiom: eight copies of a 1x1 bitmap with height -3 (advance -1 each), then a GUI image.
        image("pixel", 1, 1, 0, 1);
        image("menu", 176, 129, 0, 176);
        font("""
                {"providers":[
                  {"type":"bitmap","file":"demo:font/pixel.png","ascent":-2000,"height":-3,"chars":["\\u4e03"]},
                  {"type":"bitmap","file":"demo:font/menu.png","ascent":13,"height":129,"chars":["\\u1f03"]}
                ]}""");
        Compiled compiled = compile(config(true));
        assertTrue(compiled.problems().isEmpty(), compiled.problems().toString());
        TextLayoutTable table = compiled.table();
        assertEquals(-1f, table.lookup(null, 0x4E03).advance());
        TextLayoutTable.Entry menu = table.lookup(null, 0x1F03);
        assertTrue(menu.bedrock() >= 0xE200 && menu.bedrock() <= 0xF8FF, "private-use alias outside input-icon pages");
        assertEquals(0, menu.left());
        assertEquals(176, menu.width());
        assertEquals(177f, menu.advance());
        assertNotNull(compiled.files().get("font/glyph_%02X.png".formatted(menu.bedrock() >>> 8)));

        String title = "\u4E03".repeat(8) + "\u1F03";
        assertExact(table, title);
        assertEquals(-8, javaInk(table, title).getFirst()[0], "image starts at the GUI edge, eight units left of the title");
    }

    @Test
    void reproducesJavaSpacingTextAndGlyphBearingsWithInvisibleSpacers() throws Exception {
        image("icon", 8, 8, 0, 8);
        image("padded", 16, 16, 3, 13); // three transparent columns on the left
        font("""
                {"providers":[
                  {"type":"space","advances":{"\\ue001":-8,"\\ue002":5.0,"\\ue003":300}},
                  {"type":"bitmap","file":"demo:font/icon.png","ascent":7,"height":8,"chars":["\\ue010"]},
                  {"type":"bitmap","file":"demo:font/padded.png","ascent":12,"height":16,"chars":["\\ue011"]}
                ]}""");
        TextLayoutTable table = compile(config(true)).table();
        for (String title : List.of("\uE010", "\uE010H", "H \uE010 H", "\uE001\uE010\uE002\uE011", "\uE011\uE011H",
                "\uE001\uE001\uE010\uE003\uE010", "H\uE002H", "\uE010\uE010\uE010")) {
            assertExact(table, title);
        }
        // A glyph directly after ordinary text would need a one-unit backwards move: reported, not hidden.
        assertEquals(1, TitleLayout.layout(table, "H\uE010").approximations());
    }

    @Test
    void readsSignedTrueTypeAdvancesLikeJava() throws Exception {
        // Negative-space fonts store negative advances as unsigned 16-bit values; STB reads them signed.
        byte[] ttf = trueType(new int[]{0xF801, 0xF808, 0xF821}, new int[]{-10, -80, 10}, 100, 85, -15);
        var metrics = TrueTypeAdvances.read(ttf, 10f);
        assertEquals(-1f, metrics.get(0xF801).advance(), 1e-6);
        assertEquals(-8f, metrics.get(0xF808).advance(), 1e-6);
        assertEquals(1f, metrics.get(0xF821).advance(), 1e-6);
        assertFalse(metrics.get(0xF808).visible());

        Path fontFile = root.resolve("assets/minecraft/font/negative_spaces.ttf");
        Files.createDirectories(fontFile.getParent());
        Files.write(fontFile, ttf);
        image("menu", 176, 83, 0, 176);
        font("""
                {"providers":[
                  {"type":"ttf","file":"minecraft:negative_spaces.ttf","shift":[0,0],"size":10,"oversample":1},
                  {"type":"bitmap","file":"demo:font/menu.png","ascent":14,"height":83,"chars":["\\ue050"]}
                ]}""");
        TextLayoutTable table = compile(config(true)).table();
        assertEquals(-8f, table.lookup(null, 0xF808).advance(), 1e-6);
        assertExact(table, "\uF808\uE050");
        assertExact(table, "\uF801\uF808\uE050\uF821H");
    }

    @Test
    void ignoresTrueTypeFontsJavaCannotLoad() throws Exception {
        // Java 26.2 (FreeType) ignored a real negative-space font without a maxp table; the
        // characters fell through to fallback glyphs. Twilight must not invent negative advances.
        byte[] broken = trueType(new int[]{0xF808}, new int[]{-80}, 100, 85, -15, false);
        assertThrows(java.io.IOException.class, () -> TrueTypeAdvances.read(broken, 10f));
        Path fontFile = root.resolve("assets/minecraft/font/broken.ttf");
        Files.createDirectories(fontFile.getParent());
        Files.write(fontFile, broken);
        image("icon", 8, 8, 0, 8);
        font("""
                {"providers":[
                  {"type":"ttf","file":"minecraft:broken.ttf","size":10},
                  {"type":"bitmap","file":"demo:font/icon.png","ascent":7,"height":8,"chars":["\ue010"]}
                ]}""");
        Compiled compiled = compile(diagnostic());
        assertNull(compiled.table().lookup(null, 0xF808));
        assertTrue(compiled.problems().stream().anyMatch(problem -> problem.contains("TrueType fonts could not be read")));
    }

    @Test
    void aliasesNamedFontCollisionsAndKeepsDirectPrivateUseGlyphs() throws Exception {
        image("a", 8, 8, 0, 8);
        image("b", 8, 8, 0, 4);
        write("assets/minecraft/font/default.json", """
                {"providers":[{"type":"bitmap","file":"demo:font/a.png","ascent":7,"height":8,"chars":["\\ue121"]}]}""");
        write("assets/demo/font/other.json", """
                {"providers":[{"type":"bitmap","file":"demo:font/b.png","ascent":7,"height":8,"chars":["\\ue121"]}]}""");
        Compiled compiled = compile(config(true));
        assertTrue(compiled.problems().isEmpty(), compiled.problems().toString());
        assertEquals(0xE121, compiled.table().lookup(null, 0xE121).bedrock());
        TextLayoutTable.Entry other = compiled.table().lookup("demo:other", 0xE121);
        assertNotEquals(0xE121, other.bedrock());
        assertEquals(4, other.width());
        assertEquals(5f, other.advance());
        String out = TitleLayout.layout(compiled.table(),
                List.of(new TitleLayout.Segment("\uE121", "demo:other"))).texts().getFirst();
        assertTrue(out.codePoints().anyMatch(cp -> cp == other.bedrock()));
        assertTrue(out.codePoints().noneMatch(cp -> cp == 0xE121));
    }

    @Test
    void spacerPageIsInvisibleAndDecompositionNeverNeedsAOneUnitSpacer() throws Exception {
        image("icon", 8, 8, 0, 8);
        font("""
                {"providers":[{"type":"bitmap","file":"demo:font/icon.png","ascent":7,"height":8,"chars":["\\ue010"]}]}""");
        Compiled compiled = compile(config(true));
        TextLayoutTable table = compiled.table();
        int spacerPage = table.spacer(2) >>> 8;
        BufferedImage page = ImageIO.read(new java.io.ByteArrayInputStream(
                compiled.files().get("font/glyph_%02X.png".formatted(spacerPage))));
        for (int y = 0; y < page.getHeight(); y++) for (int x = 0; x < page.getWidth(); x++) {
            assertTrue((page.getRGB(x, y) >>> 24) <= 1, "spacer pixels must stay invisible");
        }
        // Every gap from the minimum spacer to well beyond the origin is reproduced exactly.
        for (int advance = 2 - TextLayoutTable.ORIGIN; advance < 700; advance++) {
            TextLayoutTable shifted = new TextLayoutTable(Map.of(TextLayoutTable.DEFAULT_FONT,
                    Map.of(0xE001, TextLayoutTable.Entry.advanceOnly(advance))),
                    table.spacer(2), table.maximumSpacer() - 1);
            assertExact(shifted, "\uE001H");
        }
    }

    @Test
    void combinesFontDefinitionsFromEveryPackLikeJava() throws Exception {
        // Servers stack plugin packs that each ship minecraft:default; Java merges their providers,
        // giving the higher-priority pack precedence for shared characters.
        Path lower = root.resolve("lower"), upper = root.resolve("upper");
        writeIn(lower, "assets/minecraft/font/default.json", """
                {"providers":[{"type":"bitmap","file":"demo:font/a.png","ascent":7,"height":8,"chars":["\ue000","\ue001"]}]}""");
        writeIn(upper, "assets/minecraft/font/default.json", """
                {"providers":[{"type":"bitmap","file":"demo:font/b.png","ascent":7,"height":8,"chars":["\ue000"]},
                              {"type":"space","advances":{"\ue002":-8}}]}""");
        imageIn(lower, "a", 8, 16, 0, 8);
        imageIn(upper, "b", 8, 8, 0, 3);
        var result = new BedrockPackCompiler(root.resolve("merge-data"), config(true)).build(List.of(
                new ContentSource("lower", ContentSource.Kind.RESOURCE_PACK, lower, 1),
                new ContentSource("upper", ContentSource.Kind.RESOURCE_PACK, upper, 2)), List.of());
        assertTrue(result.problems().isEmpty(), result.problems().toString());
        try (ZipFile zip = new ZipFile(result.outputDirectory().resolve("pack.zip").toFile())) {
            TextLayoutTable table = TextLayoutTable.fromJson(JsonParser.parseString(new String(
                    zip.getInputStream(zip.getEntry(TextLayoutTable.PATH)).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            assertEquals(3, table.lookup(null, 0xE000).width(), "the upper pack's glyph wins");
            assertEquals(8, table.lookup(null, 0xE001).width(), "the lower pack still contributes its other glyphs");
            assertEquals(-8f, table.lookup(null, 0xE002).advance());
        }
    }

    @Test
    void withoutTheLayoutNothingChanges() throws Exception {
        image("icon", 8, 8, 0, 8);
        font("""
                {"providers":[{"type":"bitmap","file":"demo:font/icon.png","ascent":7,"height":8,"chars":["\\ue010"]}]}""");
        Compiled compiled = compile(config(false));
        assertNull(compiled.table());
        assertFalse(compiled.files().containsKey(TextLayoutTable.PATH));
        assertEquals(1, compiled.files().keySet().stream().filter(name -> name.startsWith("font/")).count());
    }

    // --- Java and Bedrock rendering models -------------------------------------------------

    /** Asserts every visible glyph's first inked column is at the same unit on both clients. */
    private static void assertExact(TextLayoutTable table, String title) {
        TitleLayout.Result layout = TitleLayout.layout(table, title);
        String bedrock = layout.texts().getFirst();
        assertEquals(0, layout.approximations(), escape(title));
        assertSameInk(javaInk(table, title), bedrockInk(table, bedrock), "title " + escape(title));
    }

    /** Java: column zero at the pen, so ink starts at pen + left; ordinary characters are 'H'. */
    static List<int[]> javaInk(TextLayoutTable table, String title) {
        List<int[]> ink = new ArrayList<>();
        double pen = 0;
        for (int codePoint : title.codePoints().toArray()) {
            TextLayoutTable.Entry entry = table.lookup(null, codePoint);
            if (entry == null && codePoint == ' ') { pen += 4; continue; }
            if (entry == null) { ink.add(new int[]{(int) Math.round(pen), 'H'}); pen += H_WIDTH; continue; }
            if (entry.visible()) ink.add(new int[]{(int) Math.round(pen) + entry.left(), entry.bedrock()});
            pen += entry.advance();
        }
        return ink;
    }

    /** Bedrock, relative to Java's title origin: ink one unit after the pen, advance = width + 1. */
    static List<int[]> bedrockInk(TextLayoutTable table, String bedrock) {
        Map<Integer, Integer> spacers = new HashMap<>();
        for (int advance = table.minimumSpacer(); advance <= table.maximumSpacer(); advance++) {
            spacers.put(table.spacer(advance), advance);
        }
        Map<Integer, TextLayoutTable.Entry> glyphs = new HashMap<>();
        table.fonts().values().forEach(entries -> entries.values().forEach(entry -> {
            if (!entry.visible()) return;
            glyphs.put(entry.bedrock(), entry);
            if (entry.wide() >= 0) glyphs.put(entry.wide(), new TextLayoutTable.Entry(entry.bedrock(), -1,
                    entry.left(), entry.width() + 1, entry.advance()));
        }));
        List<int[]> ink = new ArrayList<>();
        int pen = -TextLayoutTable.ORIGIN;
        for (int codePoint : bedrock.codePoints().toArray()) {
            if (spacers.containsKey(codePoint)) { pen += spacers.get(codePoint); continue; }
            TextLayoutTable.Entry glyph = glyphs.get(codePoint);
            if (glyph != null) { ink.add(new int[]{pen + 1, glyph.bedrock()}); pen += glyph.width() + 1; continue; }
            if (codePoint == ' ') { pen += 4; continue; }
            ink.add(new int[]{pen, 'H'});
            pen += H_WIDTH;
        }
        return ink;
    }

    private static void assertSameInk(List<int[]> expected, List<int[]> actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected.size(), actual.size(), message);
        for (int index = 0; index < expected.size(); index++) {
            assertArrayEquals(expected.get(index), actual.get(index), message + " element " + index);
        }
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        value.codePoints().forEach(cp -> out.append(cp < 128 ? Character.toString(cp) : "\\u%04X".formatted(cp)));
        return out.toString();
    }

    // --- fixtures ---------------------------------------------------------------------------

    private record Compiled(TextLayoutTable table, Map<String, byte[]> files, List<String> problems) {}

    private Compiled compile(TwilightConfig config) throws Exception {
        ContentSource source = new ContentSource("test", ContentSource.Kind.RESOURCE_PACK, root, 1);
        var result = new BedrockPackCompiler(root.resolveSibling(root.getFileName() + "-data"), config)
                .build(List.of(source), List.of());
        Map<String, byte[]> files = new HashMap<>();
        try (ZipFile zip = new ZipFile(result.outputDirectory().resolve("pack.zip").toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                files.put(entry.getName(), zip.getInputStream(entry).readAllBytes());
            }
        }
        byte[] layout = files.get(TextLayoutTable.PATH);
        TextLayoutTable table = layout == null ? null : TextLayoutTable.fromJson(
                JsonParser.parseString(new String(layout, StandardCharsets.UTF_8)).getAsJsonObject());
        return new Compiled(table, files, result.problems());
    }

    private static TwilightConfig diagnostic() {
        return new TwilightConfig(false, false, false, false, 40, 100, 10_000_000, 1000,
                false, false, List.of(), "auto", false, false, 3, true, true);
    }

    private static TwilightConfig config(boolean textLayout) {
        return new TwilightConfig(false, true, false, false, 40, 100, 10_000_000, 1000,
                false, false, List.of(), "auto", false, false, 3, true, textLayout);
    }

    private void font(String json) throws Exception {
        write("assets/minecraft/font/default.json", json);
    }

    private void write(String path, String content) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static void writeIn(Path base, String path, String content) throws Exception {
        Path file = base.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /** Opaque magenta columns [inkFrom, inkTo) of an image whose height also sets the cell (columns = width / 8). */
    private static void imageIn(Path base, String name, int width, int height, int inkFrom, int inkTo) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (x % 8 >= inkFrom && x % 8 < inkTo) image.setRGB(x, y, 0xFFFF00FF);
        }
        Path file = base.resolve("assets/demo/textures/font/" + name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "PNG", file.toFile());
    }

    /** Opaque magenta columns [inkFrom, inkTo) of an otherwise transparent image. */
    private void image(String name, int width, int height, int inkFrom, int inkTo) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = inkFrom; x < inkTo; x++) image.setRGB(x, y, 0xFFFF00FF);
        Path file = root.resolve("assets/demo/textures/font/" + name + ".png");
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "PNG", file.toFile());
    }

    /** Minimal TrueType file: head, hhea, hmtx, cmap (format 4) and an empty loca/glyf. */
    static byte[] trueType(int[] codePoints, int[] advances, int unitsPerEm, int ascent, int descent) {
        return trueType(codePoints, advances, unitsPerEm, ascent, descent, true);
    }

    static byte[] trueType(int[] codePoints, int[] advances, int unitsPerEm, int ascent, int descent, boolean withMaxp) {
        int glyphs = codePoints.length + 1;
        ByteBuffer head = ByteBuffer.allocate(54);
        head.putShort(18, (short) unitsPerEm);
        head.putShort(50, (short) 0);
        ByteBuffer hhea = ByteBuffer.allocate(36);
        hhea.putShort(4, (short) ascent);
        hhea.putShort(6, (short) descent);
        hhea.putShort(34, (short) glyphs);
        ByteBuffer hmtx = ByteBuffer.allocate(4 * glyphs);
        for (int index = 0; index < codePoints.length; index++) hmtx.putShort(4 * (index + 1), (short) advances[index]);
        // Every glyph carries a 1x1 font-unit token outline, like real negative-space fonts.
        ByteBuffer loca = ByteBuffer.allocate(2 * (glyphs + 1));
        for (int index = 1; index <= glyphs; index++) loca.putShort(2 * index, (short) (index * 9));
        ByteBuffer glyf = ByteBuffer.allocate(18 * glyphs);
        for (int index = 0; index < glyphs; index++) glyf.putShort(18 * index, (short) 1).putShort(18 * index + 6, (short) 1)
                .putShort(18 * index + 8, (short) 1);
        int segments = codePoints.length + 1;
        ByteBuffer cmap = ByteBuffer.allocate(4 + 8 + 14 + 8 * segments + 2);
        cmap.putShort(0, (short) 0).putShort(2, (short) 1);
        cmap.putShort(4, (short) 3).putShort(6, (short) 1).putInt(8, 12);
        int sub = 12;
        cmap.putShort(sub, (short) 4).putShort(sub + 6, (short) (2 * segments));
        int ends = sub + 14, starts = ends + 2 * segments + 2, deltas = starts + 2 * segments, ranges = deltas + 2 * segments;
        for (int index = 0; index < codePoints.length; index++) {
            cmap.putShort(ends + 2 * index, (short) codePoints[index]);
            cmap.putShort(starts + 2 * index, (short) codePoints[index]);
            cmap.putShort(deltas + 2 * index, (short) ((index + 1 - codePoints[index]) & 0xFFFF));
        }
        cmap.putShort(ends + 2 * codePoints.length, (short) 0xFFFF);
        cmap.putShort(starts + 2 * codePoints.length, (short) 0xFFFF);
        cmap.putShort(deltas + 2 * codePoints.length, (short) 1);
        ByteBuffer maxp = ByteBuffer.allocate(6).putInt(0, 0x00005000).putShort(4, (short) glyphs);
        ByteBuffer[] tables = withMaxp ? new ByteBuffer[]{cmap, head, hhea, hmtx, loca, glyf, maxp}
                : new ByteBuffer[]{cmap, head, hhea, hmtx, loca, glyf};
        String[] tags = {"cmap", "head", "hhea", "hmtx", "loca", "glyf", "maxp"};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer directory = ByteBuffer.allocate(12 + 16 * tables.length);
        directory.putInt(0x00010000).putShort((short) tables.length);
        int offset = 12 + 16 * tables.length;
        for (int index = 0; index < tables.length; index++) {
            int record = 12 + 16 * index;
            directory.position(record);
            directory.put(tags[index].getBytes(StandardCharsets.US_ASCII)).putInt(0).putInt(offset).putInt(tables[index].capacity());
            offset += tables[index].capacity();
        }
        out.writeBytes(directory.array());
        for (ByteBuffer table : tables) out.writeBytes(table.array());
        return out.toByteArray();
    }
}
