/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.compiler;

import com.siberanka.twilight.source.ResourceIndex;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Boss bar colours the Java packs draw invisibly (CustomNameplates and HUD plugins replace the
 * white bar with transparent sprites and use only the boss bar's name). Bedrock always draws its
 * own bar, so the generated HUD hides the bar of these colours.
 */
final class BossBars {
    /** Java's boss bar colours in protocol order. */
    static final List<String> COLOURS = List.of("pink", "blue", "red", "green", "yellow", "purple", "white");

    private BossBars() {}

    static Set<String> hidden(ResourceIndex resources) {
        Set<String> hidden = new TreeSet<>();
        for (String colour : COLOURS) {
            if (transparent(resources, colour + "_background") && transparent(resources, colour + "_progress")) {
                hidden.add(colour);
            }
        }
        return hidden;
    }

    private static boolean transparent(ResourceIndex resources, String sprite) {
        var asset = resources.find("assets/minecraft/textures/gui/sprites/boss_bar/" + sprite + ".png");
        if (asset.isEmpty()) return false;
        try {
            BufferedImage image = PngImages.read(asset.get().readBytes());
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) return false;
                }
            }
            return true;
        } catch (IOException | RuntimeException unreadable) {
            return false;
        }
    }
}
