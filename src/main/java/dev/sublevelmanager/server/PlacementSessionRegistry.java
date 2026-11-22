package dev.sublevelmanager.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/** Active placement transaction per server thread. */
public final class PlacementSessionRegistry {

    private static final ThreadLocal<Deque<PlacementSession>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final long EXPIRY_MS = 10_000L;

    private PlacementSessionRegistry() {
    }

    public static PlacementSession begin(
            final ServerLevel level,
            final UUID ownerId,
            final String ownerName,
            final String source,
            final String blueprint) {
        final PlacementSession session = new PlacementSession(
                UUID.randomUUID(), level, ownerId, ownerName, source, blueprint,
                System.currentTimeMillis(), System.currentTimeMillis() + EXPIRY_MS);
        final Deque<PlacementSession> stack = STACK.get();
        stack.push(session);
        return session;
    }

    public static Optional<PlacementSession> active(final ServerLevel level) {
        final Deque<PlacementSession> stack = STACK.get();
        purge(stack);
        for (final PlacementSession session : stack) {
            if (session.level() == level) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }

    public static void finish(final PlacementSession session) {
        final Deque<PlacementSession> stack = STACK.get();
        stack.remove(session);
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    public static void clear() {
        STACK.remove();
    }

    private static void purge(final Deque<PlacementSession> stack) {
        final long now = System.currentTimeMillis();
        stack.removeIf(session -> now > session.expiresAt());
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    public record PlacementSession(
            UUID placementId,
            ServerLevel level,
            UUID ownerId,
            String ownerName,
            String source,
            String blueprint,
            long createdAt,
            long expiresAt) {
    }
}
