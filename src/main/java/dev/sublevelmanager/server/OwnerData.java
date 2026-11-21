package dev.sublevelmanager.server;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Reads the standalone sublevel_manager owner keys from a Sable sub-level's user data. */
public record OwnerData(String id, String name) {

    public static final OwnerData UNKNOWN = new OwnerData("", "");

    public static OwnerData fromUserData(final CompoundTag userData) {
        if (userData == null || !userData.contains("sublevel_manager", CompoundTag.TAG_COMPOUND)) {
            return UNKNOWN;
        }
        final CompoundTag owner = userData.getCompound("sublevel_manager");
        final UUID id = owner.contains("owner") ? owner.getUUID("owner") : null;
        final String name = owner.getString("ownerName");
        return new OwnerData(id == null ? "" : id.toString(), name);
    }
}
