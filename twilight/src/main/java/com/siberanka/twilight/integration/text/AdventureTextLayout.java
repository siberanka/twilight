/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.text;

import com.siberanka.twilight.text.TextLayout;
import com.siberanka.twilight.text.TextLayoutTable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@link TextLayout} to an Adventure component tree without linking against
 * Adventure: Geyser relocates it on Spigot, so methods are resolved by name from the
 * component's own classes. Styles, colours and the tree structure are preserved; only
 * text contents change, and unmeasurable components are preceded by spacers.
 */
final class AdventureTextLayout {
    private static final Map<Class<?>, Adventure> ADVENTURE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> COMPONENT_TYPES = new ConcurrentHashMap<>();

    private AdventureTextLayout() {}

    /** Whether a value is an Adventure component, relocated or not. */
    static boolean isComponent(Object value) {
        return value != null && COMPONENT_TYPES.computeIfAbsent(value.getClass(), AdventureTextLayout::implementsComponent);
    }

    private static boolean implementsComponent(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Class<?> contract : current.getInterfaces()) {
                if (contract.getName().endsWith("kyori.adventure.text.Component") || implementsComponent(contract)) return true;
            }
        }
        return false;
    }

    /** Lays out a chest title (relocated or not) and returns the rewritten component. */
    static Object layoutTitle(TextLayoutTable table, Object title) throws ReflectiveOperationException {
        return layoutTitle(table, title, false);
    }

    /** @param substituteOnly touch layout: replace glyphs without Java positioning (see {@link TextLayout#substitute}) */
    static Object layoutTitle(TextLayoutTable table, Object title, boolean substituteOnly)
            throws ReflectiveOperationException {
        return substituteOnly ? substitute(table, title) : layout(table, title, TextLayout.Mode.CONTAINER);
    }

    /**
     * Lays out text for a surface. Returns the same instance when nothing changes, which is
     * the common case for chat and entity names without custom font characters.
     */
    static Object layout(TextLayoutTable table, Object component, TextLayout.Mode mode)
            throws ReflectiveOperationException {
        Adventure adventure = Adventure.of(component);
        List<TextLayout.Segment> segments = segments(adventure, component);
        TextLayout.Result result = TextLayout.layout(table, segments, mode);
        boolean origin = mode == TextLayout.Mode.CONTAINER && table.containerOrigin() > 0;
        if (result.customCharacters() == 0 && !origin) return component;
        Object rebuilt = rebuild(adventure, component, result.texts(), new int[]{0});
        if (result.suffix().isEmpty()) return rebuilt;
        // An unstyled wrapper keeps the padding free of the text's own style (bold changes advances).
        return adventure.append(adventure.append(adventure.text(""), rebuilt), adventure.text(result.suffix()));
    }

    /** Glyph substitution only (touch layouts). */
    static Object substitute(TextLayoutTable table, Object component) throws ReflectiveOperationException {
        Adventure adventure = Adventure.of(component);
        TextLayout.Result result = TextLayout.substitute(table, segments(adventure, component));
        if (result.customCharacters() == 0) return component;
        return rebuild(adventure, component, result.texts(), new int[]{0});
    }

    /** Lays out plain default-font text, such as the signed content of a player chat message. */
    static String layout(TextLayoutTable table, String text, TextLayout.Mode mode) {
        TextLayout.Result result = TextLayout.layout(table, List.of(new TextLayout.Segment(text, null)), mode);
        return result.customCharacters() == 0 ? text : result.texts().getFirst() + result.suffix();
    }

    /** Fallback: Java text unchanged, preceded only by the spacers that reach Java's origin. */
    static Object originOnly(TextLayoutTable table, Object title) throws ReflectiveOperationException {
        Adventure adventure = Adventure.of(title);
        String origin = TextLayout.layout(table, List.of(new TextLayout.Segment(null, null))).texts().getFirst();
        return adventure.append(adventure.text(origin), title);
    }

    private static List<TextLayout.Segment> segments(Adventure adventure, Object component)
            throws ReflectiveOperationException {
        List<TextLayout.Segment> segments = new ArrayList<>();
        collect(adventure, component, null, false, segments);
        return segments;
    }

    /** Pre-order: a text component's content precedes its children; other components are opaque. */
    private static void collect(Adventure adventure, Object component, String inheritedFont, boolean inheritedBold,
                                List<TextLayout.Segment> segments) throws ReflectiveOperationException {
        String font = adventure.font(component, inheritedFont);
        boolean bold = adventure.bold(component, inheritedBold);
        if (!adventure.isText(component)) {
            segments.add(new TextLayout.Segment(null, font, bold));
            return;
        }
        segments.add(new TextLayout.Segment(adventure.content(component), font, bold));
        for (Object child : adventure.children(component)) collect(adventure, child, font, bold, segments);
    }

    private static Object rebuild(Adventure adventure, Object component, List<String> texts, int[] cursor)
            throws ReflectiveOperationException {
        String replacement = texts.get(cursor[0]++);
        if (!adventure.isText(component)) {
            return replacement.isEmpty() ? component : adventure.append(adventure.text(replacement), component);
        }
        Object rebuilt = replacement.equals(adventure.content(component)) ? component
                : adventure.withContent(component, replacement);
        List<?> children = adventure.children(component);
        if (children.isEmpty()) return rebuilt;
        List<Object> replaced = new ArrayList<>(children.size());
        boolean changed = false;
        for (Object child : children) {
            Object next = rebuild(adventure, child, texts, cursor);
            changed |= next != child;
            replaced.add(next);
        }
        return changed ? adventure.withChildren(rebuilt, replaced) : rebuilt;
    }

    /** Adventure access through the (possibly relocated) runtime classes of a component. */
    record Adventure(Class<?> component, Class<?> textComponent, Method children, Method withChildren,
                     Method style, Method font, Method keyString, Method content, Method withContent,
                     Method text, Method append, Method decoration, Object boldDecoration) {
        static Adventure of(Object sample) throws ReflectiveOperationException {
            Adventure cached = ADVENTURE.get(sample.getClass());
            if (cached != null) return cached;
            String name = sample.getClass().getName();
            int split = name.indexOf(".text.");
            if (split < 0) throw new ClassNotFoundException("Not an Adventure component: " + name);
            String base = name.substring(0, split);
            ClassLoader loader = sample.getClass().getClassLoader();
            Class<?> component = Class.forName(base + ".text.Component", false, loader);
            Class<?> textComponent = Class.forName(base + ".text.TextComponent", false, loader);
            Class<?> style = Class.forName(base + ".text.format.Style", false, loader);
            Class<?> key = Class.forName(base + ".key.Key", false, loader);
            Class<?> decorations = Class.forName(base + ".text.format.TextDecoration", false, loader);
            Object bold = null;
            for (Object constant : decorations.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals("BOLD")) bold = constant;
            }
            Adventure adventure = new Adventure(component, textComponent,
                    component.getMethod("children"), component.getMethod("children", List.class),
                    component.getMethod("style"), style.getMethod("font"), key.getMethod("asString"),
                    textComponent.getMethod("content"), textComponent.getMethod("content", String.class),
                    component.getMethod("text", String.class), component.getMethod("append", component),
                    style.getMethod("decoration", decorations), bold);
            ADVENTURE.put(sample.getClass(), adventure);
            return adventure;
        }

        boolean isText(Object value) { return textComponent.isInstance(value); }

        String font(Object value, String inherited) throws ReflectiveOperationException {
            Object key = font.invoke(style.invoke(value));
            return key == null ? inherited : (String) keyString.invoke(key);
        }

        /** Whether Java renders the component bold: TRUE/FALSE set it, NOT_SET inherits. */
        boolean bold(Object value, boolean inherited) throws ReflectiveOperationException {
            String state = ((Enum<?>) decoration.invoke(style.invoke(value), boldDecoration)).name();
            return state.equals("NOT_SET") ? inherited : state.equals("TRUE");
        }

        String content(Object value) throws ReflectiveOperationException { return (String) content.invoke(value); }
        List<?> children(Object value) throws ReflectiveOperationException { return (List<?>) children.invoke(value); }
        Object withContent(Object value, String text) throws ReflectiveOperationException { return withContent.invoke(value, text); }
        Object withChildren(Object value, List<?> list) throws ReflectiveOperationException { return withChildren.invoke(value, list); }
        Object text(String value) throws ReflectiveOperationException { return text.invoke(null, value); }
        Object append(Object parent, Object child) throws ReflectiveOperationException { return append.invoke(parent, child); }
    }
}
