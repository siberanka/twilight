/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.display;

/** A Java invisible point cloud is a non-rendering anchor, not a lingering effect. */
final class CloudAnchorPolicy {
    private CloudAnchorPolicy() { }

    static boolean useAnchor(float radius, boolean invisible) {
        return invisible && radius == 0f;
    }
}
