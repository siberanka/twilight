package com.siberanka.twilight.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.siberanka.twilight.text.LayerEncoding;
import com.siberanka.twilight.text.LayeredTextLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Generated chest and HUD labels for layered text (expression forms measured live on Bedrock). */
class LayerLabelsTest {
    @Test
    void blockExpressionsCutTheTextByBytes() {
        LayerLabels.Source title = LayerLabels.Source.variable("$container_title", LayerLabels.CHEST_EMPTY);
        assertEquals("(('%.768s' * $twilight_text) + ('%.0s' * #gamepad_helper_visible))",
                LayerLabels.blockExpression(title, 0, 768));
        assertEquals("(('%.768s' * ($twilight_text - ('%.1536s' * $twilight_text))) + ('%.0s' * #gamepad_helper_visible))",
                LayerLabels.blockExpression(title, 2, 768));
        JsonObject boss = new JsonObject();
        boss.addProperty("binding_name", "#bossName");
        assertEquals("('%.96s' * #bossName)", LayerLabels.blockExpression(LayerLabels.Source.binding(boss, "#bossName"), 0, 96),
                "a binding property needs no helper property");
    }

    @Test
    void chestTitleGetsOneLabelPerLayerAndHidesTheVanillaLabel() {
        JsonObject screen = JavaContainerUi.chestScreen(true, true);
        JsonObject vanilla = screen.getAsJsonObject("chest_label");
        assertEquals("$container_title", vanilla.get("$twilight_text").getAsString());
        assertEquals(LayerLabels.LAYER_PROPERTY, vanilla.get("text").getAsString(), "the vanilla label shows block 0");
        assertTrue(vanilla.getAsJsonArray("bindings").toString().contains("%.768s"));
        JsonObject panel = screen.getAsJsonObject("twilight_title_layers");
        assertEquals("$twilight_java_title_offset", panel.get("offset").getAsString());
        JsonArray labels = panel.getAsJsonArray("controls");
        assertEquals(LayeredTextLayout.MAX_LAYERS - 1, labels.size());
        for (int index = 0; index < labels.size(); index++) {
            int layer = LayeredTextLayout.MAX_LAYERS - 1 - index; // drawn bottom layer first
            JsonObject label = labels.get(index).getAsJsonObject().getAsJsonObject("twilight_title_layer_" + layer);
            assertEquals(LayerLabels.LAYER_PROPERTY, label.get("text").getAsString());
            assertTrue(label.getAsJsonArray("bindings").toString().contains("%." + layer * 768 + "s"));
        }
        for (String half : new String[]{"small_chest_panel_top_half", "large_chest_panel_top_half"}) {
            assertTrue(screen.getAsJsonObject(half).getAsJsonArray("modifications").toString()
                    .contains("twilight_title_layers@chest.twilight_title_layers"), half);
            assertTrue(screen.getAsJsonObject(half).getAsJsonArray("modifications").toString().contains("insert_front"),
                    "lower layers are drawn before the vanilla title label");
        }
        assertNull(JavaContainerUi.chestScreen(true, false).get("twilight_title_layers"), "layers can be disabled");
    }

    @Test
    void hudLabelsKeepVanillaValuesAndClearTheActionBarBackdrop() {
        JsonObject hud = JavaHudUi.hudScreen();
        JsonObject actionbar = hud.getAsJsonObject("hud_actionbar_text");
        assertEquals(JavaHudUi.CLEAR_TEXTURE, actionbar.get("texture").getAsString());
        JsonArray modifications = actionbar.getAsJsonArray("modifications");
        assertEquals("remove", modifications.get(0).getAsJsonObject().get("operation").getAsString());
        JsonArray inserted = modifications.get(1).getAsJsonObject().getAsJsonArray("value");
        assertEquals(LayeredTextLayout.MAX_LAYERS, inserted.size());
        JsonObject vanilla = inserted.get(inserted.size() - 1).getAsJsonObject().getAsJsonObject("actionbar_message");
        assertEquals("$actionbar_text", vanilla.get("$twilight_text").getAsString());
        assertEquals(31, vanilla.get("layer").getAsInt());
        JsonObject boss = hud.getAsJsonObject("boss_name_panel").getAsJsonArray("modifications").get(1).getAsJsonObject()
                .getAsJsonArray("value").get(0).getAsJsonObject().getAsJsonObject("twilight_boss_name_layer_3");
        assertTrue(boss.getAsJsonArray("bindings").toString().contains("\"binding_collection_name\":\"boss_bars\""));
        assertEquals("top_middle", boss.get("anchor_from").getAsString());
    }
}
