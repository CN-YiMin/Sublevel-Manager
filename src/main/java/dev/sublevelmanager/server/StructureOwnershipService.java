package dev.sublevelmanager.server;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Single ownership path for every placement source. */
public final class StructureOwnershipService {

    private static final UUID SYSTEM_ID = new UUID(0L, 0L);
    private static final String SYSTEM_NAME = "system";
    private static final OwnershipLedger LEDGER = new OwnershipLedger();
    private static boolean initialized;

    private StructureOwnershipService() {
    }

    public static void initialize(final MinecraftServer server) {
        LEDGER.load(server, true);
        initialized = true;
    }

    public static void shutdown() {
        PlacementSessionRegistry.clear();
        PlacementIntentRegistry.clear();
        LEDGER.close();
        initialized = false;
    }

    public static PlacementSessionRegistry.PlacementSession begin(
            final ServerLevel level,
            final ServerPlayer player,
            final String source,
            final String blueprint) {
        return PlacementSessionRegistry.begin(level, player.getUUID(), player.getGameProfile().getName(), source, blueprint);
    }

    public static void finish(final PlacementSessionRegistry.PlacementSession session) {
        PlacementSessionRegistry.finish(session);
    }

    public static void onSubLevelAdded(final ServerLevel level, final ServerSubLevel sub) {
        if (!initialized) {
            return;
        }
        final Optional<PlacementSessionRegistry.PlacementSession> active = PlacementSessionRegistry.active(level);
        if (active.isPresent()) {
            assign(sub, active.get(), false);
            return;
        }
        final var logical = sub.logicalPose().position();
        final Vec3 pos = new Vec3(logical.x, logical.y, logical.z);
        final Optional<PlacementIntentRegistry.Intent> intent = PlacementIntentRegistry.findNearest(level, pos, 24.0);
        if (intent.isPresent()) {
            final var fallback = PlacementSessionRegistry.begin(
                    level, intent.get().playerId(), intent.get().playerName(), "manual", "");
            assign(sub, fallback, false);
            return;
        }
        assignSystem(level, sub);
    }

    private static void assignSystem(final ServerLevel level, final ServerSubLevel sub) {
        if (OwnershipTags.read(sub).owned()) {
            return;
        }
        final UUID placementId = UUID.randomUUID();
        if (OwnershipTags.write(sub, SYSTEM_ID, SYSTEM_NAME, "system", placementId, false)) {
            LEDGER.record(placementId, SYSTEM_ID, SYSTEM_NAME, "system", "", sub.getUniqueId());
        }
    }

    private static void assign(
            final ServerSubLevel sub,
            final PlacementSessionRegistry.PlacementSession session,
            final boolean force) {
        final OwnerData existing = OwnershipTags.read(sub);
        if (existing.owned() && !force) {
            return;
        }
        if (OwnershipTags.write(sub, session.ownerId(), session.ownerName(),
                session.source(), session.placementId(), force)) {
            LEDGER.record(session.placementId(), session.ownerId(), session.ownerName(),
                    session.source(), session.blueprint(), sub.getUniqueId());
        }
    }

    public static void onSubLevelRemoved(final UUID subLevelId) {
        LEDGER.remove(subLevelId);
    }

    /** Rebuild/refresh path for an already-persisted owner tag (for example an unloaded scan). */
    public static void recordExisting(final UUID subLevelId, final OwnerData owner) {
        if (!owner.owned()) {
            return;
        }
        final UUID placementId = owner.placementId().isBlank()
                ? UUID.nameUUIDFromBytes(("legacy:" + subLevelId).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                : UUID.fromString(owner.placementId());
        LEDGER.record(placementId, UUID.fromString(owner.id()), owner.name(),
                owner.source().isBlank() ? "legacy" : owner.source(), "", subLevelId);
    }

    /** Explicit override for weld/split, which intentionally ignores the existing-owner rule. */
    public static void reassign(
            final ServerLevel level,
            final Collection<UUID> subLevelIds,
            final UUID ownerId,
            final String ownerName,
            final String source,
            final String blueprint) {
        final var container = SubLevelContainer.getContainer(level);
        if (container == null || subLevelIds.isEmpty()) {
            return;
        }
        final UUID placementId = UUID.randomUUID();
        for (final ServerSubLevel sub : container.getAllSubLevels()) {
            if (!subLevelIds.contains(sub.getUniqueId())) {
                continue;
            }
            if (OwnershipTags.write(sub, ownerId, ownerName, source, placementId, true)) {
                LEDGER.record(placementId, ownerId, ownerName, source, blueprint, sub.getUniqueId());
            }
        }
    }

    public static boolean initialized() {
        return initialized;
    }
}
