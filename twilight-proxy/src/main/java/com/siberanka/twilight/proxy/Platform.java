/*
 * Copyright (c) 2026 siberanka.
 * Licensed under the GNU LGPL v3.0 or later.
 */
package com.siberanka.twilight.proxy;

import java.nio.file.Path;
import java.util.Collection;

/** What the proxy platform (Velocity or BungeeCord) provides to the shared core. */
public interface Platform {
    Path dataDirectory();

    /** The proxy's working directory (velocity.toml, plugins/). */
    Path proxyRoot();

    boolean velocity();

    Collection<String> serverNames();

    /** The server players join first when nothing else decides. */
    String defaultServer();

    void info(String message);

    void warn(String message, Throwable failure);

    /** Runs a task off the network threads. */
    void async(Runnable task);

    void repeat(Runnable task, long periodSeconds);
}
