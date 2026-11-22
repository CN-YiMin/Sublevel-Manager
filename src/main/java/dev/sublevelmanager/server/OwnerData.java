package dev.sublevelmanager.server;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Reads the standalone Sublevel Manager ownership data from a Sable sub-level. */
public record OwnerData(String id, String name, String source, String placementId, long since) {

    public static final OwnerData UNKNOWN = new OwnerData("", "", "", "", 0L);

    public static OwnerData fromUserData(final CompoundTag userData) {
        if (userData == null || !userData.contains("sublevel_manager", CompoundTag.TAG_COMPOUND)) {
            return UNKNOWN;
        }
        final CompoundTag manager = userData.getCompound("sublevel_manager");
        final UUID id = manager.contains("owner") ? manager.getUUID("owner") : null;
        return new OwnerData(
                id == null ? "" : id.toString(),
                manager.getString("ownerName"),
                manager.getString("ownerSource"),
                manager.getString("placementId"),
                manager.getLong("ownerSince"));
    }

    public boolean owned() {
        return !id.isBlank();
    }
}
