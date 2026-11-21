package dev.sublevelmanager.server;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Reads the standalone sublevel_manager creator keys from a Sable sub-level's user data. */
public record CreatorData(String id, String name) {

    public static final CreatorData UNKNOWN = new CreatorData("", "");

    public static CreatorData fromUserData(final CompoundTag userData) {
        if (userData == null || !userData.contains("sublevel_manager", CompoundTag.TAG_COMPOUND)) {
            return UNKNOWN;
        }
        final CompoundTag manager = userData.getCompound("sublevel_manager");
        final UUID id = manager.contains("creator") ? manager.getUUID("creator") : null;
        final String name = manager.getString("creatorName");
        return new CreatorData(id == null ? "" : id.toString(), name);
    }
}
