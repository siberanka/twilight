/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.siberanka.twilight.text.LayerEncoding;
import com.siberanka.twilight.text.TextLayoutTable;

/**
 * Bedrock HUD labels that show Java's layered action bar and boss bar text.
 *
 * <p>Plugins such as CustomNameplates draw text on a background image by moving the pen back
 * with negative spaces; one Bedrock label cannot. The action bar and every boss bar name get
 * extra labels at the vanilla label's position, one per further {@link LayerLabels layer}; the
 * vanilla label shows the first. Java draws no backdrop behind the action bar, so its Bedrock background is
 * cleared as well. The definitions are merged with the vanilla {@code hud} namespace; vanilla
 * labels are re-inserted with the same values plus a visibility binding.
 */
final class JavaHudUi {
    static final String PATH = "ui/hud_screen.json";
    /** Transparent texture for cleared backgrounds. */
    static final String CLEAR_TEXTURE = "textures/ui/twilight_clear";

    private JavaHudUi() {}

    static JsonObject hudScreen() {
        JsonObject root = new JsonObject();
        root.addProperty("namespace", "hud");

        JsonObject actionbarLabel = new JsonObject();
        actionbarLabel.addProperty("type", "label");
        actionbarLabel.addProperty("anchor_from", "center");
        actionbarLabel.addProperty("anchor_to", "center");
        actionbarLabel.addProperty("color", "$tool_tip_text");
        actionbarLabel.addProperty("enable_profanity_filter", true);
        actionbarLabel.addProperty("layer", 31);
        actionbarLabel.addProperty("text", "$actionbar_text");
        actionbarLabel.addProperty("shadow", true);
        actionbarLabel.addProperty("localize", false);
        actionbarLabel.addProperty("alpha", "@hud.anim_actionbar_text_alpha_out");
        JsonObject actionbar = new JsonObject();
        actionbar.addProperty("texture", CLEAR_TEXTURE);
        actionbar.add("modifications", relabel("actionbar_message", "twilight_actionbar", actionbarLabel,
                LayerLabels.Source.variable("$actionbar_text", LayerLabels.HUD_EMPTY),
                LayerEncoding.BLOCK_BYTES.get(TextLayoutTable.ACTIONBAR_LAYERS)));
        root.add("hud_actionbar_text", actionbar);

        JsonObject bossBinding = new JsonObject();
        bossBinding.addProperty("binding_name", "#bossName");
        bossBinding.addProperty("binding_type", "collection");
        bossBinding.addProperty("binding_collection_name", "boss_bars");
        JsonObject bossLabel = new JsonObject();
        bossLabel.addProperty("type", "label");
        bossLabel.addProperty("color", "$boss_name_text_color");
        bossLabel.addProperty("enable_profanity_filter", true);
        bossLabel.addProperty("text", "#bossName");
        bossLabel.addProperty("anchor_from", "top_middle");
        bossLabel.addProperty("anchor_to", "top_middle");
        bossLabel.addProperty("shadow", true);
        // Java never clips a boss bar name; Bedrock's label would end at the 182-unit bar with an ellipsis.
        JsonArray unclipped = new JsonArray();
        unclipped.add("default");
        unclipped.add("default");
        bossLabel.add("size", unclipped);
        // Top-anchored like a chest title: layer line spacing lifts the first line (measured 4 October 2026).
        JsonArray lifted = new JsonArray();
        lifted.add(0);
        lifted.add(LayerLabels.FIRST_LINE_LIFT);
        bossLabel.add("offset", lifted);
        JsonObject bossPanel = new JsonObject();
        bossPanel.add("modifications", relabel("boss_name", "twilight_boss_name", bossLabel,
                LayerLabels.Source.binding(bossBinding, "#bossName"),
                LayerEncoding.BLOCK_BYTES.get(TextLayoutTable.BOSS_LAYERS)));
        root.add("boss_name_panel", bossPanel);

        // Bars Java draws transparent are hidden: their names start with a zero-width marker.
        JsonObject bar = new JsonObject();
        bar.add("offset", array(0, 10));
        JsonArray barBindings = new JsonArray();
        barBindings.add(bossBinding.deepCopy());
        JsonObject visible = new JsonObject();
        visible.addProperty("binding_type", "view");
        String marked = LayerEncoding.CLIENT_PREFIX + LayerEncoding.HIDDEN_BAR;
        visible.addProperty("source_property_name", "(not (('%." + LayerEncoding.bytes(marked) + "s' * #bossName) = '"
                + marked + "'))");
        visible.addProperty("target_property_name", "#visible");
        barBindings.add(visible);
        bar.add("bindings", barBindings);
        JsonObject barControl = new JsonObject();
        barControl.add("progress_bar_for_collections@common.progress_bar_for_collections", bar);
        JsonArray barControls = new JsonArray();
        barControls.add(barControl);
        JsonObject removeBar = new JsonObject();
        removeBar.addProperty("array_name", "controls");
        removeBar.addProperty("operation", "remove");
        removeBar.addProperty("control_name", "progress_bar_for_collections");
        JsonArray healthModifications = new JsonArray();
        healthModifications.add(removeBar);
        healthModifications.addAll(LayerLabels.insertBack(barControls));
        JsonObject health = new JsonObject();
        health.add("modifications", healthModifications);
        root.add("boss_health_panel", health);
        return root;
    }

    /**
     * Replaces a vanilla label (removed and inserted again, it is an array entry) by the same label
     * showing layer 0, followed by one label per further layer.
     */
    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    private static JsonArray relabel(String name, String prefix, JsonObject vanilla, LayerLabels.Source source,
                                     int blockBytes) {
        JsonObject label = vanilla.deepCopy();
        LayerLabels.showBlock(label, source, 0, blockBytes);
        JsonObject template = vanilla.deepCopy();
        template.remove("text");
        template.remove("bindings");
        // Lower layers carry the text Java draws without a shadow; the top (vanilla) label keeps it.
        template.addProperty("shadow", false);
        // Lower layers first, the vanilla label with the top layer last (drawn on top).
        JsonArray controls = LayerLabels.layerLabels(prefix, template, source, blockBytes);
        JsonObject control = new JsonObject();
        control.add(name, label);
        controls.add(control);

        JsonObject remove = new JsonObject();
        remove.addProperty("array_name", "controls");
        remove.addProperty("operation", "remove");
        remove.addProperty("control_name", name);
        JsonArray modifications = new JsonArray();
        modifications.add(remove);
        modifications.addAll(LayerLabels.insertBack(controls));
        return modifications;
    }
}
