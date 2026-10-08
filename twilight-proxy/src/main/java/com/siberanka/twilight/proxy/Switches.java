/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bedrock server switches that need a pack reconnect, from the transfer until the player arrives. A switch
 * remembers where the player wants to go while the client reconnects, loads the pack and (on networks with a
 * login plugin) logs in again; it ends when the player reaches that server, or at its deadline, which grows
 * with the size of the pack. Pure bookkeeping with an explicit clock, so it is tested without a proxy.
 */
final class Switches {
    /** A transferred player that has not come back after this long gets one warning in the log. */
    static final long RECONNECT_WARNING_MILLIS = 60_000;
    private static final int MAX_TRACKED = 100_000;

    /** One reconnect in progress. */
    static final class Switch {
        final String xuid;
        final String name;
        final String from;
        final String to;
        final long packBytes;
        final long started;
        final long deadline;
        final String address;
        volatile long reconnected;
        /** The first server of the reconnected session was set to {@link #to}. */
        volatile boolean routed;
        /** The player is held on a login or limbo server and goes on at its next server change. */
        volatile boolean deferred;
        volatile String heldOn;
        volatile boolean warned;

        Switch(String xuid, String name, String from, String to, long packBytes, long started, long deadline, String address) {
            this.xuid = xuid;
            this.name = name;
            this.from = from;
            this.to = to;
            this.packBytes = packBytes;
            this.started = started;
            this.deadline = deadline;
            this.address = address;
        }

        long seconds(long now) {
            return Math.max(0, (now - started + 500) / 1000);
        }
    }

    /** What happens to the first server of a reconnected session. */
    sealed interface Initial {
        /** Not a reconnect, or it expired: the proxy and other plugins decide. */
        record Keep() implements Initial {}

        /** Send the player to the server it reconnected for. */
        record Route(Switch entry) implements Initial {}

        /** Another plugin chose a login or limbo server; the switch goes on at the next server change. */
        record Defer(Switch entry, String heldOn) implements Initial {}
    }

    /** What a server connection of a player with a switch in progress means. */
    sealed interface Connect {
        /** No switch, or the client has not come back yet. */
        record None() implements Connect {}

        /** The player reaches the server it reconnected for; the switch ends. */
        record Arrived(Switch entry) implements Connect {}

        /** The first server, or a login server, is another one: the switch waits there. */
        record Hold(Switch entry, boolean first) implements Connect {}

        /** A later server change to another server: the switch ends and the player goes to its server instead. */
        record Redirect(Switch entry) implements Connect {}
    }

    /** A line for the log; warnings point at a setup problem. */
    record Note(boolean warning, String text) {}

    private final Map<String, Switch> byXuid = new ConcurrentHashMap<>();
    /** Java name -> XUID, for proxies that choose the first server before Geyser knows the Java UUID. */
    private final Map<String, String> byName = new ConcurrentHashMap<>();

    boolean start(Switch entry) {
        if (byXuid.size() >= MAX_TRACKED && !byXuid.containsKey(entry.xuid)) return false;
        byXuid.put(entry.xuid, entry);
        if (entry.name != null && byName.size() < MAX_TRACKED) byName.put(entry.name.toLowerCase(Locale.ROOT), entry.xuid);
        return true;
    }

    void cancel(String xuid) {
        Switch removed = byXuid.remove(xuid);
        if (removed != null && removed.name != null) byName.remove(removed.name.toLowerCase(Locale.ROOT), xuid);
    }

    /** The live switch of {@code xuid}. */
    Optional<Switch> get(String xuid, long now) {
        Switch entry = xuid == null ? null : byXuid.get(xuid);
        return entry == null || entry.deadline <= now ? Optional.empty() : Optional.of(entry);
    }

    /** XUID of a transferred player known only by its Java name; accepted only once it reconnected. */
    Optional<String> xuidByName(String name, long now) {
        if (name == null) return Optional.empty();
        String xuid = byName.get(name.toLowerCase(Locale.ROOT));
        return get(xuid, now).filter(entry -> entry.reconnected > 0).map(entry -> entry.xuid);
    }

    /** The client came back; returns the switch the first time, for the log. */
    Optional<Switch> reconnected(String xuid, long now) {
        Optional<Switch> entry = get(xuid, now);
        if (entry.isEmpty() || entry.get().reconnected > 0) return Optional.empty();
        entry.get().reconnected = now;
        return entry;
    }

    /**
     * The first server of a session: {@code chosen} is the server picked after every other plugin ran, and
     * {@code original} the one the proxy itself picked. When another plugin changed it (a login or limbo
     * server), that plugin wins and the switch waits.
     */
    Initial initial(String xuid, String chosen, String original, long now) {
        Optional<Switch> found = get(xuid, now);
        if (found.isEmpty()) return new Initial.Keep();
        Switch entry = found.get();
        if (chosen != null && original != null && !chosen.equalsIgnoreCase(original) && !chosen.equalsIgnoreCase(entry.to)) {
            entry.deferred = true;
            entry.heldOn = chosen.toLowerCase(Locale.ROOT);
            return new Initial.Defer(entry, entry.heldOn);
        }
        entry.routed = true;
        return new Initial.Route(entry);
    }

    /**
     * A server connection that every plugin allowed. {@code hold} is true for the first server of a session
     * and for login servers: there the switch waits instead of sending the player on.
     */
    Connect connect(String xuid, String target, boolean hold, long now) {
        Optional<Switch> found = get(xuid, now);
        if (found.isEmpty()) return new Connect.None();
        Switch entry = found.get();
        if (entry.reconnected == 0 && !entry.routed && !entry.deferred) return new Connect.None();
        if (entry.to.equalsIgnoreCase(target)) {
            cancel(xuid);
            return new Connect.Arrived(entry);
        }
        if (hold) {
            boolean first = !entry.deferred;
            entry.deferred = true;
            entry.heldOn = target.toLowerCase(Locale.ROOT);
            return new Connect.Hold(entry, first);
        }
        cancel(xuid);
        return new Connect.Redirect(entry);
    }

    /**
     * Another plugin refused the first connection to the server the player reconnected for (a login plugin
     * that blocks every other server): the switch waits for the next server change. Empty when that was not
     * a routed switch.
     */
    Optional<Switch> refused(String xuid, String target, long now) {
        Optional<Switch> found = get(xuid, now).filter(entry -> entry.routed && entry.to.equalsIgnoreCase(target));
        found.ifPresent(entry -> entry.deferred = true);
        return found;
    }

    /** Notes about switches that need attention; expired switches are removed. */
    List<Note> sweep(long now) {
        List<Note> messages = new ArrayList<>();
        for (Iterator<Switch> each = byXuid.values().iterator(); each.hasNext(); ) {
            Switch entry = each.next();
            if (entry.deadline <= now) {
                each.remove();
                if (entry.name != null) byName.remove(entry.name.toLowerCase(Locale.ROOT), entry.xuid);
                messages.add(new Note(entry.reconnected > 0 && !entry.deferred, entry.reconnected == 0
                        ? entry.name + " did not come back after the reconnect for " + entry.to + "'s pack."
                        : entry.deferred
                        ? entry.name + " stayed on " + entry.heldOn + "; it is no longer sent on to " + entry.to + "."
                        : entry.name + " did not finish loading " + entry.to + "'s pack within "
                        + (entry.deadline - entry.started) / 60_000 + " min; the reconnect was abandoned."));
            } else if (entry.reconnected == 0 && !entry.warned && now - entry.started >= RECONNECT_WARNING_MILLIS) {
                entry.warned = true;
                messages.add(new Note(true, entry.name + " has not come back " + entry.seconds(now) + " s after the transfer to "
                        + entry.address + ". Check that Bedrock players reach this address over UDP (transfer-address, "
                        + "transfer-port), and that DDoS protection or anti-bot plugins allow a quick reconnect."));
            }
        }
        return messages;
    }

    int size() {
        return byXuid.size();
    }
}
