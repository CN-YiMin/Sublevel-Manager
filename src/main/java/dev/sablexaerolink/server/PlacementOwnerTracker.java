package dev.sablexaerolink.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Best-effort owner assignment for placement items that create Sable sub-levels without
 * setting Shtreimel's owner (for example some package/vehicle items).
 *
 * <p>Pure event path: a recent right-click is remembered, then the Sable onSubLevelAdded
 * callback tries to consume it. No timer, no container scan, no per-tick work.
 */
public final class PlacementOwnerTracker {

    private static final long MAX_AGE_MS = 10_000L;
    private static final double MAX_DISTANCE = 24.0;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private PlacementOwnerTracker() {
    }

    public static void reset() {
        PENDING.clear();
    }

    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            record(player, event.getLevel(), Vec3.atCenterOf(event.getPos()));
        }
    }

    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            record(player, event.getLevel(), player.position());
        }
    }

    private static void record(final ServerPlayer player, final Level level, final Vec3 pos) {
        final long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> now - entry.getValue().time() > MAX_AGE_MS);
        PENDING.put(player.getUUID(), new Pending(
                player.getUUID(), player.getGameProfile().getName(),
                level.dimension(), pos, now));
    }

    /** Called from Sable's onSubLevelAdded event. */
    public static void tryAssign(final ServerLevel level, final ServerSubLevel sub) {
        final long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(entry -> now - entry.getValue().time() > MAX_AGE_MS);
        if (PENDING.isEmpty() || sub.isRemoved()) {
            return;
        }
        final CompoundTag userData = sub.getUserDataTag();
        if (userData != null
                && userData.contains("shtreimel", CompoundTag.TAG_COMPOUND)
                && userData.getCompound("shtreimel").contains("owner")) {
            return;
        }
        final var pos = sub.logicalPose().position();
        Pending best = null;
        for (final Pending pending : PENDING.values()) {
            if (!pending.dimension().equals(level.dimension())) {
                continue;
            }
            if (now - pending.time() > MAX_AGE_MS) {
                continue;
            }
            if (pending.pos().distanceToSqr(pos.x, pos.y, pos.z) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            if (best == null || pending.time() > best.time()) {
                best = pending;
            }
        }
        if (best == null) {
            return;
        }
        final CompoundTag user = userData == null ? new CompoundTag() : userData.copy();
        final CompoundTag owner = user.contains("shtreimel", CompoundTag.TAG_COMPOUND)
                ? user.getCompound("shtreimel")
                : new CompoundTag();
        owner.putUUID("owner", best.player());
        owner.putString("ownerName", best.name());
        owner.putString("how", "placement");
        owner.putLong("since", now);
        user.put("shtreimel", owner);
        sub.setUserDataTag(user);
        dev.sablexaerolink.SableXaeroLink.LOGGER.info(
                "[shtreimel-plus] assigned placement owner {} to {}",
                best.name(), sub.getUniqueId());
    }

    private record Pending(UUID player, String name, ResourceKey<Level> dimension, Vec3 pos, long time) {
    }
}
