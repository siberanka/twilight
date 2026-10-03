package com.siberanka.twilight.integration.text;

import com.siberanka.twilight.text.TextLayoutTable;
import com.siberanka.twilight.text.TextLayout;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdventureTextLayoutTest {
    // Shift -1 (U+4E03), a 176-wide menu image aliased to U+F700, and a named-font icon.
    private static final TextLayoutTable TABLE = new TextLayoutTable(Map.of(
            TextLayoutTable.DEFAULT_FONT, Map.of(
                    0x4E03, TextLayoutTable.Entry.advanceOnly(-1),
                    0x1F03, new TextLayoutTable.Entry(0xF700, -1, 0, 176, 177)),
            "demo:icons", Map.of(0xE121, new TextLayoutTable.Entry(0xF600, 0xF680, 0, 8, 9))),
            0xF800, 32);

    @Test
    void rewritesNestedTextKeepingStylesAndStructure() throws Exception {
        // DeluxeMenus '&f\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u1F03' as Paper deserializes it: an empty root with a white child.
        Component title = Component.text().append(Component.text("\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u1F03", NamedTextColor.WHITE)).build();
        Component laidOut = (Component) AdventureTextLayout.layoutTitle(TABLE, title);

        String expected = TextLayout.layout(TABLE, List.of(new TextLayout.Segment("", null),
                new TextLayout.Segment("\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u4E03\u1F03", null))).texts().get(1);
        TextComponent child = (TextComponent) laidOut.children().getFirst();
        assertEquals(expected, child.content());
        assertEquals(NamedTextColor.WHITE, child.color());
        assertEquals("", ((TextComponent) laidOut).content());
        assertTrue(child.content().contains("\uF700"));
        assertFalse(child.content().contains("\u1F03"));
    }

    @Test
    void touchLayoutGetsGlyphSubstitutionWithoutSpacers() throws Exception {
        // Bedrock's pocket chest title is a centred label unrelated to Java geometry: no origin spacers.
        Component title = Component.text("\u4E03\u4E03\u1F03 Shop", NamedTextColor.WHITE);
        TextComponent laidOut = (TextComponent) AdventureTextLayout.layoutTitle(TABLE, title, true);
        assertEquals("\uF700 Shop", laidOut.content());
        assertEquals(NamedTextColor.WHITE, laidOut.color());
    }

    @Test
    void usesTheInheritedNamedFontOfEachComponent() throws Exception {
        Component title = Component.text().font(Key.key("demo:icons"))
                .append(Component.text("\uE121")).build();
        Component laidOut = (Component) AdventureTextLayout.layoutTitle(TABLE, title);
        String icon = ((TextComponent) laidOut.children().getFirst()).content();
        assertTrue(icon.endsWith("\uF600"), icon);
        assertEquals(Key.key("demo:icons"), laidOut.style().font());
    }

    @Test
    void precedesUnmeasurableComponentsWithOriginSpacers() throws Exception {
        TranslatableComponent chest = Component.translatable("container.chest");
        Component laidOut = (Component) AdventureTextLayout.layoutTitle(TABLE, chest);
        String spacers = ((TextComponent) laidOut).content();
        assertEquals(TextLayout.layout(TABLE, List.of(new TextLayout.Segment(null, null))).texts().getFirst(), spacers);
        assertSame(chest, laidOut.children().getFirst());
        int advance = spacers.codePoints().map(cp -> cp - 0xF800 + 2).sum();
        assertEquals(TextLayoutTable.ORIGIN, advance, "translations start at Java's title origin");
    }

    @Test
    void fallbackKeepsJavaTextAfterTheOriginSpacers() throws Exception {
        Component title = Component.text("Plain");
        Component fallback = (Component) AdventureTextLayout.originOnly(TABLE, title);
        assertSame(title, fallback.children().getFirst());
    }
}
