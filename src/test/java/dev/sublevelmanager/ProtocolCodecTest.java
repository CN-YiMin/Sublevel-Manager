package dev.sublevelmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sublevelmanager.client.ClientStructureCache;
import dev.sublevelmanager.network.StructureDeltaPayload;
import dev.sublevelmanager.network.StructureEntry;
import dev.sublevelmanager.network.StructureSnapshotPayload;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ProtocolCodecTest {

    private static StructureEntry entry(final UUID id, final boolean loaded, final double x) {
        return new StructureEntry(id, "name", "minecraft:overworld", x, 64.0, 128.0, loaded, "", "00000000-0000-0000-0000-000000000001", "Alice");
    }

    @Test
    void entryRoundTrips() {
        final StructureEntry input = entry(UUID.randomUUID(), true, 12.5);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        StructureEntry.STREAM_CODEC.encode(buffer, input);
        assertEquals(input, StructureEntry.STREAM_CODEC.decode(buffer));
    }

    @Test
    void snapshotAndDeltaRoundTrip() {
        final StructureEntry loaded = entry(UUID.randomUUID(), true, 1.0);
        final StructureEntry unloaded = entry(UUID.randomUUID(), false, 2.0);
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        StructureSnapshotPayload.STREAM_CODEC.encode(buffer,
                new StructureSnapshotPayload(List.of(loaded, unloaded), true));
        final StructureSnapshotPayload snapshot = StructureSnapshotPayload.STREAM_CODEC.decode(buffer);
        assertTrue(snapshot.full());
        assertEquals(2, snapshot.entries().size());

        StructureDeltaPayload.STREAM_CODEC.encode(buffer,
                new StructureDeltaPayload(List.of(loaded), List.of(unloaded.id())));
        final StructureDeltaPayload delta = StructureDeltaPayload.STREAM_CODEC.decode(buffer);
        assertEquals(1, delta.upserts().size());
        assertEquals(unloaded.id(), delta.removed().getFirst());
    }

    @Test
    void deltaRemovesAndUpserts() {
        final StructureEntry first = entry(UUID.randomUUID(), true, 1.0);
        final StructureEntry second = entry(UUID.randomUUID(), false, 2.0);
        ClientStructureCache.clear();
        ClientStructureCache.accept(new StructureSnapshotPayload(List.of(first, second), true));
        assertEquals(2, ClientStructureCache.entries().size());

        final StructureEntry moved = new StructureEntry(first.id(), first.name(), first.dimension(), 9.0, first.y(), first.z(), true, "", first.ownerId(), first.ownerName());
        ClientStructureCache.applyDelta(new StructureDeltaPayload(List.of(moved), List.of(second.id())));
        assertEquals(1, ClientStructureCache.entries().size());
        assertEquals(9.0, ClientStructureCache.entries().getFirst().x());
    }

    @Test
    void nonFullSnapshotPreservesUnloadedEntries() {
        final StructureEntry loaded = entry(UUID.randomUUID(), true, 1.0);
        final StructureEntry unloaded = entry(UUID.randomUUID(), false, 2.0);
        ClientStructureCache.clear();
        ClientStructureCache.accept(new StructureSnapshotPayload(List.of(loaded, unloaded), true));

        final StructureEntry moved = new StructureEntry(loaded.id(), loaded.name(), loaded.dimension(), 7.0, loaded.y(), loaded.z(), true, "", loaded.ownerId(), loaded.ownerName());
        ClientStructureCache.accept(new StructureSnapshotPayload(List.of(moved), false));

        assertEquals(2, ClientStructureCache.entries().size());
        assertTrue(ClientStructureCache.entries().stream()
                .anyMatch(e -> e.id().equals(unloaded.id()) && !e.loaded()));
        assertEquals(7.0, ClientStructureCache.entries().stream()
                .filter(e -> e.id().equals(loaded.id())).findFirst().orElseThrow().x());
    }
}
