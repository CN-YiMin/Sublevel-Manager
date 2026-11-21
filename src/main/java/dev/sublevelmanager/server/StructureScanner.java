package dev.sublevelmanager.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.sublevelmanager.network.StructureEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Reads the loaded half of the directory from Sable. */
public final class StructureScanner {

    private StructureScanner() {
    }

    public static List<StructureEntry> scanLoaded(final MinecraftServer server) {
        final List<StructureEntry> result = new ArrayList<>();
        for (final ServerLevel level : server.getAllLevels()) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (final ServerSubLevel sub : container.getAllSubLevels()) {
                final StructureEntry entry = describe(level, sub);
                if (entry != null) {
                    result.add(entry);
                }
            }
        }
        return List.copyOf(result);
    }

    /** Single sub-level description shared by the periodic scan and the event observer. */
    public static StructureEntry describe(final ServerLevel level, final ServerSubLevel sub) {
        try {
            final var logicalPosition = sub.logicalPose().position();
            final Vec3 pos = new Vec3(logicalPosition.x, logicalPosition.y, logicalPosition.z);
            final CompoundTag userData = sub.getUserDataTag();
            final OwnerData owner = OwnerData.fromUserData(userData);
            return new StructureEntry(
                    sub.getUniqueId(),
                    sub.getName() == null ? "" : sub.getName(),
                    level.dimension().location().toString(),
                    pos.x,
                    pos.y,
                    pos.z,
                    true,
                    chainId(sub),
                    owner.id(),
                    owner.name());
        } catch (final RuntimeException exception) {
            // One malformed or half-removed sub-level must not hide the rest.
            dev.sublevelmanager.SublevelManager.LOGGER.warn(
                    "[sublevel-manager] could not read sub-level at {}",
                    level.dimension().location(),
                    exception);
            return null;
        }
    }

    /** Stable group id for a rope/constraint-connected chain; singleton uses its own UUID. */
    private static String chainId(final ServerSubLevel sub) {
        UUID smallest = sub.getUniqueId();
        try {
            for (final SubLevel member : SubLevelHelper.getConnectedChain(sub)) {
                if (member instanceof ServerSubLevel server
                        && server.getLevel() == sub.getLevel()
                        && server.getUniqueId().compareTo(smallest) < 0) {
                    smallest = server.getUniqueId();
                }
            }
        } catch (final RuntimeException ignored) {
            // A broken constraint graph must not hide the structure.
        }
        return smallest.toString();
    }

    /**
     * Loaded entries plus saved-but-not-loaded entries. The unloaded scan is intentionally
     * on-demand: it reads every region cell and must not run every server tick.
     */
    public static List<StructureEntry> scanAll(final MinecraftServer server) {
        UnloadedStructureScanner.clear();
        final List<StructureEntry> loaded = scanLoaded(server);
        final Set<UUID> seen = new HashSet<>();
        for (final StructureEntry entry : loaded) {
            seen.add(entry.id());
        }
        final List<StructureEntry> result = new ArrayList<>(loaded);
        for (final ServerLevel level : server.getAllLevels()) {
            final List<StructureEntry> unloaded = UnloadedStructureScanner.scan(level, seen);
            result.addAll(unloaded);
            for (final StructureEntry entry : unloaded) {
                seen.add(entry.id());
            }
        }
        return List.copyOf(result);
    }
}
