package dev.sublevelmanager.server;

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
 * Tracks the player who produced a physical structure.
 *
 * <p>This is deliberately separate from {@link PlacementOwnerTracker}: creator is the
 * immutable origin of the body, while owner is the current responsibility holder. Both are
 * written under {@code user_data.sublevel_manager}, but by different trackers.
 *
 * <p>Pure Sable event path: a recent right-click is remembered, then onSubLevelAdded
 * consumes it. No timer and no container scan.
 */
public final class StructureCreatorTracker {

    private static final long MAX_AGE_MS = 10_000L;
    private static final double MAX_DISTANCE = 24.0;
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private StructureCreatorTracker() {
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
                && userData.contains("sublevel_manager", CompoundTag.TAG_COMPOUND)
                && userData.getCompound("sublevel_manager").contains("creator")) {
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
        final CompoundTag manager = user.contains("sublevel_manager", CompoundTag.TAG_COMPOUND)
                ? user.getCompound("sublevel_manager")
                : new CompoundTag();
        manager.putUUID("creator", best.player());
        manager.putString("creatorName", best.name());
        manager.putString("createdBy", "placement");
        manager.putLong("createdAt", now);
        user.put("sublevel_manager", manager);
        sub.setUserDataTag(user);
        dev.sublevelmanager.SublevelManager.LOGGER.info(
                "[sublevel-manager] tracked structure creator {} for {}",
                best.name(), sub.getUniqueId());
    }

    private record Pending(UUID player, String name, ResourceKey<Level> dimension, Vec3 pos, long time) {
    }
}
