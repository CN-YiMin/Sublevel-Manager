package dev.sublevelmanager.server;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Central NBT read/write for standalone ownership tags. */
public final class OwnershipTags {

    public static final String ROOT = "sublevel_manager";

    private OwnershipTags() {
    }

    public static OwnerData read(final ServerSubLevel sub) {
        return OwnerData.fromUserData(sub.getUserDataTag());
    }

    /** @return true when the owner was written; existing owners are preserved unless forced. */
    public static boolean write(
            final ServerSubLevel sub,
            final UUID ownerId,
            final String ownerName,
            final String source,
            final UUID placementId,
            final boolean force) {
        final CompoundTag current = sub.getUserDataTag();
        final CompoundTag manager = current != null
                && current.contains(ROOT, CompoundTag.TAG_COMPOUND)
                ? current.getCompound(ROOT).copy()
                : new CompoundTag();
        if (!force && manager.contains("owner")) {
            return false;
        }
        manager.putUUID("owner", ownerId);
        manager.putString("ownerName", ownerName == null ? "" : ownerName);
        manager.putString("ownerSource", source == null ? "unknown" : source);
        manager.putString("placementId", placementId == null ? "" : placementId.toString());
        manager.putLong("ownerSince", System.currentTimeMillis());
        final CompoundTag user = current == null ? new CompoundTag() : current.copy();
        user.put(ROOT, manager);
        sub.setUserDataTag(user);
        return true;
    }

    public static void clear(final ServerSubLevel sub) {
        final CompoundTag current = sub.getUserDataTag();
        if (current == null || !current.contains(ROOT, CompoundTag.TAG_COMPOUND)) {
            return;
        }
        final CompoundTag user = current.copy();
        user.remove(ROOT);
        sub.setUserDataTag(user);
    }
}
