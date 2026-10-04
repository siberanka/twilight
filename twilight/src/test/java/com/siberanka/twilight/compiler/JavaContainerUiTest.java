package com.siberanka.twilight.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.siberanka.twilight.config.TwilightConfig;
import com.siberanka.twilight.source.ContentSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class JavaContainerUiTest {
    // Java AbstractContainerScreen: title (8, 6), first chest slot frame (7, 17).
    private static final int[] JAVA_TITLE = {8, 6};
    private static final int[] JAVA_SLOT_FRAME = {7, 17};

    @TempDir Path root;

    @Test
    void placesTheFirstTextRowWhereJavaDrawsTheTitleForBothChestSizes() {
        for (int[] grid : new int[][]{JavaContainerUi.SMALL_CHEST_GRID, JavaContainerUi.LARGE_CHEST_GRID}) {
            JsonArray offset = JavaContainerUi.titleOffset(grid);
            int textX = offset.get(0).getAsInt();
            int textTop = offset.get(1).getAsInt() + JavaContainerUi.BEDROCK_LABEL_TEXT_TOP;
            assertEquals(JAVA_TITLE[0] - JAVA_SLOT_FRAME[0], textX - grid[0]);
            assertEquals(JAVA_TITLE[1] - JAVA_SLOT_FRAME[1], textTop - grid[1]);
        }
        JsonObject screen = JavaContainerUi.chestScreen();
        assertEquals("[8,-3]", screen.getAsJsonObject("small_chest_panel_top_half")
                .get("$twilight_java_title_offset").toString());
        assertEquals("[8,-2]", screen.getAsJsonObject("large_chest_panel_top_half")
                .get("$twilight_java_title_offset").toString());
    }

    @Test
    void keepsJavaPlayerInventoryDistanceFromEveryChestGrid() {
        // Java: chest frames every 18 units, player inventory frame 13 units after the last row.
        // Bedrock (measured, vanilla offsets): 11 units for 1-6 rows; the bottom half is bottom-anchored.
        JsonObject screen = JavaContainerUi.chestScreen();
        assertEquals("[0,10]", screen.getAsJsonObject("small_chest_panel_top_half").get("offset").toString());
        assertEquals("[0,9]", screen.getAsJsonObject("large_chest_panel_top_half").get("offset").toString());
        for (int[][] panel : new int[][][]{{JavaContainerUi.SMALL_CHEST_PANEL, {0, 10}},
                {JavaContainerUi.LARGE_CHEST_PANEL, {0, 9}}}) {
            int raised = panel[0][1] - JavaContainerUi.panelOffset(panel[0]).get(1).getAsInt();
            assertEquals(JavaContainerUi.JAVA_INVENTORY_GAP, JavaContainerUi.BEDROCK_INVENTORY_GAP + raised);
        }
    }

    @Test
    void givesOnlyChestScreensJavaHotbarSpacing() {
        JsonObject common = JavaContainerUi.commonScreen();
        assertEquals("common", common.get("namespace").getAsString());
        JsonObject hotbar = common.getAsJsonObject("hotbar_grid_template");
        assertEquals("[0,-5]", hotbar.get("$twilight_hotbar_offset|default").toString(), "other screens stay vanilla");
        assertEquals("$twilight_hotbar_offset", hotbar.get("offset").getAsString());
        assertEquals(2, hotbar.size(), "grid size, template and bindings stay vanilla");
        JsonObject chest = JavaContainerUi.chestScreen();
        for (String panel : List.of("small_chest_panel", "large_chest_panel")) {
            assertEquals("[0,-4]", chest.getAsJsonObject(panel).get("$twilight_hotbar_offset").toString());
            assertFalse(chest.getAsJsonObject(panel).has("controls"), "controls stay vanilla");
        }
    }

    @Test
    void drawsTheChestInventoryLabelWhereAndAboveWhereJavaDoes() {
        JsonObject common = JavaContainerUi.commonScreen();
        JsonArray modifications = common.getAsJsonObject("inventory_panel_bottom_half_with_label")
                .getAsJsonArray("modifications");
        assertEquals(2, modifications.size());
        JsonObject remove = modifications.get(0).getAsJsonObject();
        assertEquals("remove", remove.get("operation").getAsString());
        assertEquals("inventory_label", remove.get("control_name").getAsString());
        JsonObject insert = modifications.get(1).getAsJsonObject();
        assertEquals("insert_back", insert.get("operation").getAsString());
        JsonObject label = insert.getAsJsonArray("value").get(0).getAsJsonObject()
                .getAsJsonObject("inventory_label@common.section_heading_label");
        // Defaults reproduce vanilla for furnaces, hoppers and every other user of the shared bottom half.
        assertEquals("[7,3]", label.get("$twilight_inventory_label_offset|default").toString());
        assertEquals(2, label.get("$twilight_inventory_label_layer|default").getAsInt());
        assertEquals("container.inventory", label.get("text").getAsString());
        assertEquals("top_left", label.get("anchor_from").getAsString());

        JsonObject chest = JavaContainerUi.chestScreen();
        for (String panel : List.of("small_chest_panel", "large_chest_panel")) {
            JsonObject screen = chest.getAsJsonObject(panel);
            // Java: one unit right of and ten above the player inventory frame; text starts 1 below origin.
            assertEquals("[8,2]", screen.get("$twilight_inventory_label_offset").toString());
            int layer = screen.get("$twilight_inventory_label_layer").getAsInt();
            assertTrue(layer > JavaContainerUi.TITLE_LAYER, "Java draws the inventory label after the title");
            assertTrue(layer < 8, "items stay above the label");
        }
    }

    @Test
    void movesTheLabelOriginForTheTitleLayoutOnly() {
        JsonObject laidOut = JavaContainerUi.chestScreen(true);
        assertEquals("[-248,-3]", laidOut.getAsJsonObject("small_chest_panel_top_half")
                .get("$twilight_java_title_offset").toString());
        assertEquals("[-248,-2]", laidOut.getAsJsonObject("large_chest_panel_top_half")
                .get("$twilight_java_title_offset").toString());
        assertEquals(JavaContainerUi.chestScreen(), JavaContainerUi.chestScreen(false));
    }

    @Test
    void neverWrapsAndLayersTheTitleBetweenSlotFramesAndItems() {
        JsonObject screen = JavaContainerUi.chestScreen();
        assertEquals("chest", screen.get("namespace").getAsString());
        JsonObject label = screen.getAsJsonObject("chest_label");
        assertEquals("[\"default\",\"default\"]", label.get("size").toString());
        assertEquals("$twilight_java_title_offset", label.get("offset").getAsString());
        assertEquals("[8,-2]", label.get("$twilight_java_title_offset|default").toString());
        int layer = label.get("layer").getAsInt();
        assertTrue(layer > 3, "slot frames must stay below the title image");
        assertTrue(layer < 5, "selection, hover and items must stay above the title image");
        for (var channel : label.getAsJsonArray("color")) assertEquals(0x40, Math.round(channel.getAsDouble() * 255));
        // A partial definition must not replace vanilla behavior such as text binding or localization.
        for (String preserved : List.of("text", "type", "localize", "enable_profanity_filter", "anchor_from", "controls")) {
            assertFalse(label.has(preserved), preserved);
            assertFalse(screen.getAsJsonObject("small_chest_panel_top_half").has(preserved), preserved);
        }
    }

    @Test
    void compilerEmitsTheAdapterOnlyWhenEnabled() throws Exception {
        Path source = root.resolve("source");
        Path font = source.resolve("assets/minecraft/font/default.json");
        Files.createDirectories(font.getParent());
        Files.writeString(font, """
                {"providers":[{"type":"bitmap","file":"demo:font/menu.png","height":83,"ascent":14,"chars":["\\uEC03"]}]}
                """);
        Path texture = source.resolve("assets/demo/textures/font/menu.png");
        Files.createDirectories(texture.getParent());
        BufferedImage image = new BufferedImage(176, 83, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF402010);
        ImageIO.write(image, "PNG", texture.toFile());
        ContentSource pack = new ContentSource("test", ContentSource.Kind.RESOURCE_PACK, source, 1);

        Path enabled = new BedrockPackCompiler(root.resolve("enabled"), config(true)).build(List.of(pack), List.of()).outputDirectory();
        try (ZipFile zip = new ZipFile(enabled.resolve("pack.zip").toFile())) {
            var entry = zip.getEntry(JavaContainerUi.PATH);
            assertNotNull(entry);
            JsonObject parsed = JsonParser.parseString(new String(zip.getInputStream(entry).readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            // A custom font enables the title layout, which moves the label origin left and adds layer labels.
            assertEquals(JavaContainerUi.chestScreen(true, true), parsed);
            assertNotNull(zip.getEntry(JavaHudUi.PATH), "layered action bar and boss bar labels");
            assertNotNull(zip.getEntry("twilight/text-layout.json"));
            assertNotNull(zip.getEntry(JavaContainerUi.COMMON_PATH));
            assertNotNull(zip.getEntry("font/glyph_EC.png"));
        }
        Path disabled = new BedrockPackCompiler(root.resolve("disabled"), config(false)).build(List.of(pack), List.of()).outputDirectory();
        try (ZipFile zip = new ZipFile(disabled.resolve("pack.zip").toFile())) {
            assertNull(zip.getEntry(JavaContainerUi.PATH));
            assertNull(zip.getEntry(JavaContainerUi.COMMON_PATH));
            assertNotNull(zip.getEntry("font/glyph_EC.png"));
        }
    }

    private static TwilightConfig config(boolean titles) {
        return new TwilightConfig(false, true, false, false, 40, 100, 10_000_000, 1000,
                false, false, List.of(), "auto", false, false, 3, titles);
    }
}
