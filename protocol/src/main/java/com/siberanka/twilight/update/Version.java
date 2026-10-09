/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.update;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Twilight version such as {@code 1.0.0}, {@code 1.0.0-pre.14} or a development build
 * {@code 1.0.0-pre.14-SNAPSHOT}, ordered like Semantic Versioning: a prerelease comes before its release
 * and numeric parts compare as numbers. A development build comes before the version it leads to.
 */
public record Version(int major, int minor, int patch, List<String> pre, boolean snapshot) implements Comparable<Version> {
    private static final Pattern FORMAT = Pattern.compile("v?(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})(?:-([0-9A-Za-z.-]{1,64}))?");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}");
    private static final String SNAPSHOT = "SNAPSHOT";

    public Version {
        pre = List.copyOf(pre);
    }

    public static Optional<Version> parse(String text) {
        if (text == null) return Optional.empty();
        Matcher matcher = FORMAT.matcher(text.strip());
        if (!matcher.matches()) return Optional.empty();
        String suffix = matcher.group(4);
        boolean snapshot = false;
        if (suffix != null && (suffix.equals(SNAPSHOT) || suffix.endsWith("-" + SNAPSHOT))) {
            snapshot = true;
            suffix = suffix.equals(SNAPSHOT) ? null : suffix.substring(0, suffix.length() - SNAPSHOT.length() - 1);
        }
        List<String> pre = suffix == null ? List.of() : List.of(suffix.split("\\.", -1));
        for (String part : pre) {
            if (part.isEmpty()) return Optional.empty();
        }
        return Optional.of(new Version(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)), pre, snapshot));
    }

    /** A prerelease or development build. */
    public boolean prerelease() {
        return snapshot || !pre.isEmpty();
    }

    @Override
    public int compareTo(Version other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        if (result == 0) result = comparePre(pre, other.pre);
        if (result == 0) result = Boolean.compare(!snapshot, !other.snapshot);
        return result;
    }

    private static int comparePre(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) return Boolean.compare(left.isEmpty(), right.isEmpty());
        for (int index = 0; index < Math.min(left.size(), right.size()); index++) {
            String a = left.get(index);
            String b = right.get(index);
            boolean numericA = NUMBER.matcher(a).matches();
            boolean numericB = NUMBER.matcher(b).matches();
            int result = numericA && numericB ? Integer.compare(Integer.parseInt(a), Integer.parseInt(b))
                    : numericA != numericB ? (numericA ? -1 : 1)
                    : a.compareTo(b);
            if (result != 0) return result;
        }
        return Integer.compare(left.size(), right.size());
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
        if (!pre.isEmpty()) text.append('-').append(String.join(".", pre));
        if (snapshot) text.append('-').append(SNAPSHOT);
        return text.toString();
    }
}
