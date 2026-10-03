/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.text;

import com.siberanka.twilight.text.TextLayoutTable;
import com.siberanka.twilight.text.TitleLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies {@link TitleLayout} to an Adventure component tree without linking against
 * Adventure: Geyser relocates it on Spigot, so methods are resolved by name from the
 * component's own classes. Styles, colours and the tree structure are preserved; only
 * text contents change, and unmeasurable components are preceded by spacers.
 */
final class AdventureTitleLayout {
    private AdventureTitleLayout() {}

    /** Lays out an Adventure title (relocated or not) and returns the rewritten component. */
    static Object layoutTitle(TextLayoutTable table, Object title) throws ReflectiveOperationException {
        return layoutTitle(table, title, false);
    }

    /** @param substituteOnly touch layout: replace glyphs without Java positioning (see {@link TitleLayout#substitute}) */
    static Object layoutTitle(TextLayoutTable table, Object title, boolean substituteOnly)
            throws ReflectiveOperationException {
        Adventure adventure = Adventure.of(title);
        List<Node> nodes = new ArrayList<>();
        collect(adventure, title, null, nodes);
        List<TitleLayout.Segment> segments = nodes.stream()
                .map(node -> new TitleLayout.Segment(node.text, node.font)).toList();
        TitleLayout.Result result = substituteOnly ? TitleLayout.substitute(table, segments)
                : TitleLayout.layout(table, segments);
        return rebuild(adventure, title, null, result.texts(), new int[]{0});
    }

    /** Fallback: Java text unchanged, preceded only by the spacers that reach Java's origin. */
    static Object originOnly(TextLayoutTable table, Object title) throws ReflectiveOperationException {
        Adventure adventure = Adventure.of(title);
        String origin = TitleLayout.layout(table, List.of(new TitleLayout.Segment(null, null))).texts().getFirst();
        return adventure.append(adventure.text(origin), title);
    }

    /** Pre-order: a text component's content precedes its children; other components are opaque. */
    private static void collect(Adventure adventure, Object component, String inheritedFont, List<Node> nodes)
            throws ReflectiveOperationException {
        String font = adventure.font(component, inheritedFont);
        if (!adventure.isText(component)) {
            nodes.add(new Node(null, font));
            return;
        }
        nodes.add(new Node(adventure.content(component), font));
        for (Object child : adventure.children(component)) collect(adventure, child, font, nodes);
    }

    private static Object rebuild(Adventure adventure, Object component, String inheritedFont, List<String> texts,
                                  int[] cursor) throws ReflectiveOperationException {
        String replacement = texts.get(cursor[0]++);
        if (!adventure.isText(component)) {
            return replacement.isEmpty() ? component : adventure.append(adventure.text(replacement), component);
        }
        String font = adventure.font(component, inheritedFont);
        Object rebuilt = adventure.withContent(component, replacement);
        List<?> children = adventure.children(component);
        if (children.isEmpty()) return rebuilt;
        List<Object> replaced = new ArrayList<>(children.size());
        for (Object child : children) replaced.add(rebuild(adventure, child, font, texts, cursor));
        return adventure.withChildren(rebuilt, replaced);
    }

    private record Node(String text, String font) {}

    /** Adventure access through the (possibly relocated) runtime classes of a component. */
    record Adventure(Class<?> component, Class<?> textComponent, Method children, Method withChildren,
                     Method style, Method font, Method keyString, Method content, Method withContent,
                     Method text, Method append) {
        static Adventure of(Object sample) throws ReflectiveOperationException {
            String name = sample.getClass().getName();
            int split = name.indexOf(".text.");
            if (split < 0) throw new ClassNotFoundException("Not an Adventure component: " + name);
            String base = name.substring(0, split);
            ClassLoader loader = sample.getClass().getClassLoader();
            Class<?> component = Class.forName(base + ".text.Component", false, loader);
            Class<?> textComponent = Class.forName(base + ".text.TextComponent", false, loader);
            Class<?> style = Class.forName(base + ".text.format.Style", false, loader);
            Class<?> key = Class.forName(base + ".key.Key", false, loader);
            return new Adventure(component, textComponent,
                    component.getMethod("children"), component.getMethod("children", List.class),
                    component.getMethod("style"), style.getMethod("font"), key.getMethod("asString"),
                    textComponent.getMethod("content"), textComponent.getMethod("content", String.class),
                    component.getMethod("text", String.class), component.getMethod("append", component));
        }

        boolean isText(Object value) { return textComponent.isInstance(value); }

        String font(Object value, String inherited) throws ReflectiveOperationException {
            Object key = font.invoke(style.invoke(value));
            return key == null ? inherited : (String) keyString.invoke(key);
        }

        String content(Object value) throws ReflectiveOperationException { return (String) content.invoke(value); }
        List<?> children(Object value) throws ReflectiveOperationException { return (List<?>) children.invoke(value); }
        Object withContent(Object value, String text) throws ReflectiveOperationException { return withContent.invoke(value, text); }
        Object withChildren(Object value, List<?> list) throws ReflectiveOperationException { return withChildren.invoke(value, list); }
        Object text(String value) throws ReflectiveOperationException { return text.invoke(null, value); }
        Object append(Object parent, Object child) throws ReflectiveOperationException { return append.invoke(parent, child); }
    }
}
