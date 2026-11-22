package dev.sublevelmanager.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import dev.sublevelmanager.mixin.SableStorageAccessors;
import dev.sublevelmanager.network.StructureEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Reads saved-but-not-loaded sub-levels from Sable's region files.
 *
 * <p>Sable's own {@code /sable storage find_all_sub_levels} uses the same two-step API:
 * read the holding chunk for a region cell, then read every pointer's sub-level data. We reuse
 * that path instead of parsing the region format ourselves.
 */
public final class UnloadedStructureScanner {

    private static final Map<UUID, UnloadedRef> FOUND = new ConcurrentHashMap<>();

    private UnloadedStructureScanner() {
    }

    public static void clear() {
        FOUND.clear();
    }

    public static boolean teleport(final ServerPlayer player, final UUID id) {
        final UnloadedRef ref = FOUND.get(id);
        if (ref == null) {
            return false;
        }
        player.teleportTo(ref.level(), ref.position().x, ref.position().y + 1.0, ref.position().z,
                player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean delete(final MinecraftServer server, final UUID id) {        final UnloadedRef ref = FOUND.get(id);
        if (ref == null) {
            return false;
        }
        final var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(ref.level());
        if (container == null) {
            return false;
        }
        final SubLevelStorage storage = container.getHoldingChunkMap().getStorage();
        final SubLevelHoldingChunk holding = storage.attemptLoadHoldingChunk(ref.chunk());
        if (holding == null) {
            return false;
        }
        if (!backup(storage, server, ref.chunk())) {
            return false;
        }
        final boolean removed = holding.getSubLevelPointers().removeIf(pointer -> pointer.equals(ref.pointer()));
        if (!removed) {
            return false;
        }
        storage.attemptSaveHoldingChunk(ref.chunk(), holding);
        try {
            final var file = ((SableStorageAccessors.Storage) storage)
                    .sublevelManager$regionStorageFile(ref.chunk(), ref.pointer().storageIndex());
            if (file != null) {
                ((SableStorageAccessors.File) file).sublevelManager$clear(ref.pointer().subLevelIndex());
            }
        } catch (final IOException | RuntimeException exception) {
            // The pointer is already gone, so failing to reclaim the sector must not turn a
            // successful logical delete into a failed action.
            dev.sublevelmanager.SublevelManager.LOGGER.warn(
                    "[sublevel-manager] logical delete succeeded but sector reclaim failed", exception);
        }
        FOUND.remove(id);
        return true;
    }

    private static boolean backup(final SubLevelStorage storage, final MinecraftServer server, final ChunkPos chunk) {
        try {
            final int regionX = chunk.x >> SubLevelRegionFile.LOG_SIDE_LENGTH;
            final int regionZ = chunk.z >> SubLevelRegionFile.LOG_SIDE_LENGTH;
            final Path source = storage.getFolder().resolve("r." + regionX + "." + regionZ + SubLevelRegionFile.FILE_EXTENSION);
            if (!Files.isRegularFile(source)) {
                return false;
            }
            final Path archive = server.getServerDirectory().resolve("sublevel-manager-archive");
            Files.createDirectories(archive);
            final Path target = archive.resolve(source.getFileName() + "." + System.currentTimeMillis() + ".bak");
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            return true;
        } catch (final IOException exception) {
            dev.sublevelmanager.SublevelManager.LOGGER.warn(
                    "[sublevel-manager] could not back up region before unloaded delete", exception);
            return false;
        }
    }

    private record UnloadedRef(ServerLevel level, ChunkPos chunk, SavedSubLevelPointer pointer, Vec3 position) {
    }

    public static List<StructureEntry> scan(final ServerLevel level, final Set<UUID> alreadyLoaded) {
        final List<StructureEntry> result = new ArrayList<>();
        final Set<UUID> seen = new HashSet<>(alreadyLoaded);
        final var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
        if (container == null) {
            return List.of();
        }
        final SubLevelStorage storage = container.getHoldingChunkMap().getStorage();
        final Path folder = storage.getFolder();
        if (folder == null || !Files.isDirectory(folder)) {
            return List.of();
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.slvlr")) {
            for (final Path file : files) {
                final int[] region = parseRegion(file.getFileName().toString());
                if (region == null) {
                    continue;
                }
                final int side = SubLevelRegionFile.SIDE_LENGTH;
                for (int localX = 0; localX < side; localX++) {
                    for (int localZ = 0; localZ < side; localZ++) {
                        final ChunkPos chunk = new ChunkPos(
                                region[0] * side + localX,
                                region[1] * side + localZ);
                        final SubLevelHoldingChunk holding = storage.attemptLoadHoldingChunk(chunk);
                        if (holding == null) {
                            continue;
                        }
                        for (final SavedSubLevelPointer pointer : holding.getSubLevelPointers()) {
                            final SubLevelData data = storage.attemptLoadSubLevel(chunk, pointer);
                            if (data == null || !seen.add(data.uuid())) {
                                continue;
                            }
                            final CompoundTag full = data.fullTag();
                            final OwnerData owner = OwnerData.fromUserData(
                                    full.getCompound("user_data"));
                            StructureOwnershipService.recordExisting(data.uuid(), owner);
                            final String name = full.contains("display_name")
                                    ? full.getString("display_name")
                                    : "";
                            final var position = data.pose().position();
                            FOUND.put(data.uuid(), new UnloadedRef(level, chunk, pointer,
                                    new Vec3(position.x, position.y, position.z)));
                            result.add(new StructureEntry(
                                    data.uuid(),
                                    name,
                                    level.dimension().location().toString(),
                                    position.x,
                                    position.y,
                                    position.z,
                                    false,
                                    "",
                                    owner.id(),
                                    owner.name()));
                        }
                    }
                }
            }
        } catch (final IOException exception) {
            dev.sublevelmanager.SublevelManager.LOGGER.warn(
                    "[sublevel-manager] could not scan unloaded sub-levels in {}",
                    level.dimension().location(),
                    exception);
        }
        return List.copyOf(result);
    }

    private static int[] parseRegion(final String fileName) {
        // Sable writes r.<regionX>.<regionZ>.slvlr.
        if (!fileName.startsWith("r.") || !fileName.endsWith(".slvlr")) {
            return null;
        }
        final String[] parts = fileName.substring(2, fileName.length() - ".slvlr".length()).split("\\.");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }
}
