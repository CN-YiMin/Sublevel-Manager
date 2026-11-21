package dev.sublevelmanager.client;

import dev.sublevelmanager.network.StructureDeltaPayload;
import dev.sublevelmanager.network.StructureEntry;
import dev.sublevelmanager.network.StructureSnapshotPayload;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Collection;

/** Last directory received from the server. Client-only content, no Xaero classes here. */
public final class ClientStructureCache {

    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile List<StructureEntry> entries = List.of();
    private static volatile long version;

    private ClientStructureCache() {
    }

    public static long version() {
        return version;
    }

    public static void addListener(final Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(final Runnable listener) {
        LISTENERS.remove(listener);
    }

    public static void accept(final StructureSnapshotPayload payload) {
        if (payload.full()) {
            entries = List.copyOf(payload.entries());
            changed();
            return;
        }
        // Periodic packets contain loaded structures only. Keep the unloaded entries
        // from the last full snapshot, otherwise the grey markers would vanish every second.
        final Map<UUID, StructureEntry> merged = new LinkedHashMap<>();
        for (final StructureEntry entry : entries) {
            if (!entry.loaded()) {
                merged.put(entry.id(), entry);
            }
        }
        for (final StructureEntry entry : payload.entries()) {
            merged.put(entry.id(), entry);
        }
        entries = List.copyOf(merged.values());
        changed();
    }

    public static List<StructureEntry> entries() {
        return entries;
    }

    public static void applyDelta(final StructureDeltaPayload payload) {
        final Map<UUID, StructureEntry> merged = new LinkedHashMap<>();
        for (final StructureEntry entry : entries) {
            merged.put(entry.id(), entry);
        }
        for (final UUID removed : payload.removed()) {
            merged.remove(removed);
        }
        for (final StructureEntry entry : payload.upserts()) {
            merged.put(entry.id(), entry);
        }
        entries = List.copyOf(merged.values());
        changed();
    }

    /** Optimistic local removal while the server response is in flight. */
    public static void removeLocal(final Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        final Map<UUID, StructureEntry> merged = new LinkedHashMap<>();
        for (final StructureEntry entry : entries) {
            if (!ids.contains(entry.id())) {
                merged.put(entry.id(), entry);
            }
        }
        entries = List.copyOf(merged.values());
        changed();
    }

    public static void clear() {
        entries = List.of();
        changed();
    }

    /** Signals a display-state change without discarding the last snapshot. */
    public static void touch() {
        changed();
    }

    private static void changed() {
        version++;
        for (final Runnable listener : LISTENERS) {
            try {
                listener.run();
            } catch (final RuntimeException ignored) {
                // A closed screen or broken listener must not block cache updates.
            }
        }
    }
}
