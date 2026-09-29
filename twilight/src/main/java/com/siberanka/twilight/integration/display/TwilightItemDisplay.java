/* Copyright (c) 2026 siberanka. Licensed under the GNU LGPL v3.0 or later. */
package com.siberanka.twilight.integration.display;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

/** One Java display, owned by Geyser's normal per-session entity lifecycle. */
final class TwilightItemDisplay extends Entity {
    private final GeyserDisplayBridge bridge;
    private final float[] pending = DisplayPose.identity().values();
    private final DisplayTimeline timeline = new DisplayTimeline();
    private final DisplayAppearance appearance = new DisplayAppearance();
    private Integer durationTicks, delayTicks;
    private int sequence;
    private boolean poseDirty;

    TwilightItemDisplay(EntitySpawnContext spawn, GeyserDisplayBridge bridge) {
        super(spawn);
        this.bridge = bridge;
        // Java displays have one entity rotation, not an independent living head.
        // A zero head yaw makes Bedrock turn mounted meshes toward that heading.
        setHeadYaw(getYaw());
    }

    @Override public void moveRelativeRaw(double x, double y, double z, float yaw, float pitch,
                                          float headYaw, boolean onGround) {
        super.moveRelativeRaw(x, y, z, yaw, pitch, yaw, onGround);
    }

    @Override public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw,
                                          boolean onGround, boolean teleported) {
        super.moveAbsoluteRaw(position, yaw, pitch, yaw, onGround, teleported);
    }

    @Override public void updateHeadLookRotation(float headYaw) {
        // A display has no independently rotatable head.
    }

    @Override public void setRiderSeatPosition(Vector3f position) {
        Entity vehicle = getVehicle();
        if (vehicle != null && vehicle.getEntityType() == EntityType.AREA_EFFECT_CLOUD) {
            // Java clouds attach passengers at their full height; displays attach
            // at their feet. Geyser's generic 75% mount offset places a display
            // 0.125 blocks too low, potentially sampling light inside the floor.
            position = Vector3f.from(position.getX(), vehicle.getJavaDefinition().height(), position.getZ());
        }
        super.setRiderSeatPosition(position);
    }

    void delay(int ticks) { delayTicks = Math.clamp(ticks, -72_000, 72_000); }
    void duration(int ticks) { durationTicks = Math.clamp(ticks, 0, 72_000); }
    void vector(int offset, Vector3f value) {
        pending[offset] = value.getX(); pending[offset + 1] = value.getY(); pending[offset + 2] = value.getZ(); poseDirty = true;
    }
    void rotation(int offset, Quaternionf value) {
        pending[offset] = value.getX(); pending[offset + 1] = value.getY(); pending[offset + 2] = value.getZ(); pending[offset + 3] = value.getW(); poseDirty = true;
    }
    void context(byte value) { appearance.context(value); updateAppearance(); }
    void viewRange(float value) { appearance.viewRange(value); updateAppearance(); }
    private void updateAppearance() { updateProperty(bridge.appearance, appearance.packed()); }
    void item(ItemStack stack) {
        int selected = -1;
        if (stack != null && stack.getAmount() > 0) {
            var item = ItemTranslator.translateToBedrock(session, stack);
            selected = bridge.variants.getOrDefault(item.getDefinition().getIdentifier(), -1);
        }
        appearance.variant(selected);
        updateAppearance();
    }

    @Override public void updateBedrockMetadata() {
        if ((poseDirty || durationTicks != null || delayTicks != null) && bridge != null) {
            var state = timeline.update(poseDirty ? new DisplayPose(pending) : null, durationTicks, delayTicks, System.nanoTime());
            poseDirty = false;
            durationTicks = null;
            delayTicks = null;
            sequence = (sequence + 1) % 1_000_001;
            // Both endpoints arrive atomically; the resource pack performs render-frame SLERP.
            updatePropertiesBatched(batch -> {
                for (int i = 0; i < DisplayPose.SIZE; i++) {
                    batch.update(bridge.from[i], state.from().value(i));
                    batch.update(bridge.to[i], state.to().value(i));
                }
                batch.update(bridge.seconds, state.seconds());
                batch.update(bridge.delay, state.delay());
                batch.update(bridge.revision, sequence);
            }, false);
        }
        super.updateBedrockMetadata();
    }
}
