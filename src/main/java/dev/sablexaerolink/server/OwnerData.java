package dev.sablexaerolink.server;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Reads Shtreimel's owner keys directly so the server side has no hard Shtreimel class dependency. */
public record OwnerData(String id, String name) {

    public static final OwnerData UNKNOWN = new OwnerData("", "");

    public static OwnerData fromUserData(final CompoundTag userData) {
        if (userData == null || !userData.contains("shtreimel", CompoundTag.TAG_COMPOUND)) {
            return UNKNOWN;
        }
        final CompoundTag owner = userData.getCompound("shtreimel");
        final UUID id = owner.contains("owner") ? owner.getUUID("owner") : null;
        final String name = owner.getString("ownerName");
        return new OwnerData(id == null ? "" : id.toString(), name);
    }
}
