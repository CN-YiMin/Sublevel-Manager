package dev.sublevelmanager.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Bounded, event-driven record of a player's most recent placement interaction. */
public final class PlacementIntentRegistry {

    private static final long MAX_AGE_MS = 10_000L;
    private static final Map<UUID, Intent> INTENTS = new HashMap<>();

    private PlacementIntentRegistry() {
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
        INTENTS.entrySet().removeIf(entry -> now - entry.getValue().time() > MAX_AGE_MS);
        INTENTS.put(player.getUUID(), new Intent(
                player.getUUID(), player.getGameProfile().getName(), level.dimension(), pos, now));
    }

    public static Optional<Intent> findNearest(
            final ServerLevel level, final Vec3 pos, final double maxDistance) {
        final long now = System.currentTimeMillis();
        Intent best = null;
        for (final Intent intent : INTENTS.values()) {
            if (!intent.dimension().equals(level.dimension())
                    || now - intent.time() > MAX_AGE_MS
                    || intent.position().distanceToSqr(pos) > maxDistance * maxDistance) {
                continue;
            }
            if (best == null || intent.time() > best.time()) {
                best = intent;
            }
        }
        return Optional.ofNullable(best);
    }

    public static Optional<Intent> findLatest(final ServerLevel level) {
        final long now = System.currentTimeMillis();
        Intent best = null;
        for (final Intent intent : INTENTS.values()) {
            if (!intent.dimension().equals(level.dimension()) || now - intent.time() > MAX_AGE_MS) {
                continue;
            }
            if (best == null || intent.time() > best.time()) {
                best = intent;
            }
        }
        return Optional.ofNullable(best);
    }

    public static void remove(final UUID playerId) {
        INTENTS.remove(playerId);
    }

    public static void clear() {
        INTENTS.clear();
    }

    public record Intent(UUID playerId, String playerName, ResourceKey<Level> dimension, Vec3 position, long time) {
    }
}
