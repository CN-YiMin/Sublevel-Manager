package dev.sublevelmanager.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Per-save ownership ledger. Disk writes are debounced and never run on the server tick. */
public final class OwnershipLedger {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long WRITE_DELAY_MS = 1500L;

    private final Map<UUID, PlacementRecord> placements = new HashMap<>();
    private final Map<UUID, UUID> bySubLevel = new HashMap<>();
    private final ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "sublevel-manager-ledger");
        thread.setDaemon(true);
        return thread;
    });
    private Path file;
    private ScheduledFuture<?> pendingWrite;
    private boolean closed;

    public synchronized void load(final MinecraftServer server, final boolean rebuildIfMissing) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("sublevel_manager/ownership.json");
        placements.clear();
        bySubLevel.clear();
        if (!Files.isRegularFile(file)) {
            if (rebuildIfMissing) {
                rebuildLoaded(server);
            }
            return;
        }
        try {
            final JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            final JsonObject placementJson = root.has("placements") ? root.getAsJsonObject("placements") : new JsonObject();
            for (final String key : placementJson.keySet()) {
                final JsonObject node = placementJson.getAsJsonObject(key);
                final UUID placementId = UUID.fromString(key);
                final PlacementRecord record = new PlacementRecord(
                        placementId,
                        UUID.fromString(node.get("owner").getAsString()),
                        node.get("ownerName").getAsString(),
                        node.get("source").getAsString(),
                        node.has("blueprint") ? node.get("blueprint").getAsString() : "",
                        node.get("createdAt").getAsLong());
                if (node.has("subLevelIds")) {
                    for (final var id : node.getAsJsonArray("subLevelIds")) {
                        final UUID subId = UUID.fromString(id.getAsString());
                        record.subLevelIds.add(subId);
                        bySubLevel.put(subId, placementId);
                    }
                }
                placements.put(placementId, record);
            }
            if (root.has("subLevels")) {
                final JsonObject subJson = root.getAsJsonObject("subLevels");
                for (final String key : subJson.keySet()) {
                    final UUID subId = UUID.fromString(key);
                    final UUID placementId = UUID.fromString(subJson.getAsJsonObject(key).get("placementId").getAsString());
                    bySubLevel.put(subId, placementId);
                }
            }
        } catch (final RuntimeException | IOException exception) {
            dev.sublevelmanager.SublevelManager.LOGGER.warn("[sublevel-manager] could not read ownership ledger", exception);
        }
    }

    private void rebuildLoaded(final MinecraftServer server) {
        for (final var level : server.getAllLevels()) {
            final var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (final var sub : container.getAllSubLevels()) {
                final OwnerData owner = OwnershipTags.read(sub);
                if (!owner.owned()) {
                    continue;
                }
                final UUID placementId = owner.placementId().isBlank()
                        ? UUID.nameUUIDFromBytes(("legacy:" + sub.getUniqueId()).getBytes(StandardCharsets.UTF_8))
                        : UUID.fromString(owner.placementId());
                record(placementId, UUID.fromString(owner.id()), owner.name(),
                        owner.source().isBlank() ? "legacy" : owner.source(), "", sub.getUniqueId());
            }
        }
    }

    public synchronized void record(
            final UUID placementId,
            final UUID ownerId,
            final String ownerName,
            final String source,
            final String blueprint,
            final UUID subLevelId) {
        final PlacementRecord record = placements.computeIfAbsent(placementId, id ->
                new PlacementRecord(id, ownerId, ownerName == null ? "" : ownerName,
                        source == null ? "unknown" : source, blueprint == null ? "" : blueprint,
                        System.currentTimeMillis()));
        record.subLevelIds.add(subLevelId);
        bySubLevel.put(subLevelId, placementId);
        markDirty();
    }

    public synchronized void remove(final UUID subLevelId) {
        final UUID placementId = bySubLevel.remove(subLevelId);
        if (placementId == null) {
            return;
        }
        final PlacementRecord record = placements.get(placementId);
        if (record != null) {
            record.subLevelIds.remove(subLevelId);
            if (record.subLevelIds.isEmpty()) {
                placements.remove(placementId);
            }
        }
        markDirty();
    }

    private synchronized void markDirty() {
        if (closed || file == null) {
            return;
        }
        if (pendingWrite != null && !pendingWrite.isDone()) {
            return;
        }
        pendingWrite = writer.schedule(this::flushNow, WRITE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void flushNow() {
        if (closed || file == null) {
            return;
        }
        try {
            final JsonObject root = new JsonObject();
            root.addProperty("schema", 1);
            root.addProperty("updatedAt", System.currentTimeMillis());
            final JsonObject placementJson = new JsonObject();
            for (final PlacementRecord record : placements.values()) {
                final JsonObject node = new JsonObject();
                node.addProperty("owner", record.ownerId.toString());
                node.addProperty("ownerName", record.ownerName);
                node.addProperty("source", record.source);
                node.addProperty("blueprint", record.blueprint);
                node.addProperty("createdAt", record.createdAt);
                final JsonArray ids = new JsonArray();
                for (final UUID id : record.subLevelIds) {
                    ids.add(id.toString());
                }
                node.add("subLevelIds", ids);
                placementJson.add(record.placementId.toString(), node);
            }
            root.add("placements", placementJson);
            final JsonObject subJson = new JsonObject();
            for (final var entry : bySubLevel.entrySet()) {
                final JsonObject node = new JsonObject();
                node.addProperty("placementId", entry.getValue().toString());
                subJson.add(entry.getKey().toString(), node);
            }
            root.add("subLevels", subJson);
            Files.createDirectories(file.getParent());
            final Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final IOException | RuntimeException exception) {
            dev.sublevelmanager.SublevelManager.LOGGER.warn("[sublevel-manager] could not write ownership ledger", exception);
        }
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        flushNow();
        closed = true;
        writer.shutdownNow();
    }

    private static final class PlacementRecord {
        private final UUID placementId;
        private final UUID ownerId;
        private final String ownerName;
        private final String source;
        private final String blueprint;
        private final long createdAt;
        private final Set<UUID> subLevelIds = new HashSet<>();

        private PlacementRecord(final UUID placementId, final UUID ownerId, final String ownerName,
                                final String source, final String blueprint, final long createdAt) {
            this.placementId = placementId;
            this.ownerId = ownerId;
            this.ownerName = ownerName;
            this.source = source;
            this.blueprint = blueprint;
            this.createdAt = createdAt;
        }
    }
}
