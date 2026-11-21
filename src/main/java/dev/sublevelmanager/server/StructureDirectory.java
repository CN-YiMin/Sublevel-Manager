package dev.sublevelmanager.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.sublevelmanager.network.StructureActionPayload;
import dev.sublevelmanager.network.StructureDeltaPayload;
import dev.sublevelmanager.network.StructureEntry;
import dev.sublevelmanager.network.StructureSnapshotPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side subscription and snapshot fan-out. */
public final class StructureDirectory {

    private static final int SCAN_TICKS = 20;
    private static final int STATIC_REFRESH_TICKS = 200;
    private static final double MOVED_THRESHOLD = 0.05;
    private static final Map<UUID, Boolean> SUBSCRIBERS = new ConcurrentHashMap<>();
    private static final Map<UUID, SentState> LAST_SENT = new HashMap<>();
    private static final Map<UUID, Long> LAST_RESCAN = new HashMap<>();
    private static final List<StructureEntry> UNLOADED_CACHE = new ArrayList<>();
    private static final Set<SubLevelContainer> ATTACHED = new HashSet<>();
    private static long tickCounter;
    private static int scanCounter;

    private StructureDirectory() {
    }

    /** Attach once per Sable container; add/remove events are pushed immediately. */
    public static void attach(final ServerLevel level, final ServerSubLevelContainer container) {
        if (!ATTACHED.add(container)) {
            return;
        }
        container.addObserver(new DirectoryObserver(level));
    }

    public static void detachAll() {
        ATTACHED.clear();
        UNLOADED_CACHE.clear();
        LAST_RESCAN.clear();
        LAST_SENT.clear();
        SUBSCRIBERS.clear();
        tickCounter = 0;
        scanCounter = 0;
    }

    /**
     * Loaded entries from the event/cache model plus the last explicitly requested
     * unloaded scan. No region files are read on ordinary snapshots.
     */
    private static List<StructureEntry> snapshotEntries(final MinecraftServer server) {
        final List<StructureEntry> loaded = StructureScanner.scanLoaded(server);
        final List<StructureEntry> combined = new ArrayList<>(loaded);
        final Set<UUID> ids = new HashSet<>();
        for (final StructureEntry entry : loaded) {
            ids.add(entry.id());
        }
        for (final StructureEntry entry : UNLOADED_CACHE) {
            if (!ids.contains(entry.id())) {
                combined.add(entry);
            }
        }
        return List.copyOf(combined);
    }

    /** Explicit UI refresh: this is the only path that scans unloaded region storage. */
    public static void rescanUnloaded(final ServerPlayer player) {
        final long now = System.currentTimeMillis();
        final long previous = LAST_RESCAN.getOrDefault(player.getUUID(), 0L);
        if (now - previous < 5_000L) {
            player.displayClientMessage(Component.literal("刷新未加载结构过于频繁，请稍候"), true);
            return;
        }
        LAST_RESCAN.put(player.getUUID(), now);

        final List<StructureEntry> all = StructureScanner.scanAll(player.server);
        UNLOADED_CACHE.clear();
        for (final StructureEntry entry : all) {
            if (!entry.loaded()) {
                UNLOADED_CACHE.add(entry);
            }
        }
        applySnapshot(player.server, all);
        player.displayClientMessage(Component.literal(
                "已刷新未加载结构：" + UNLOADED_CACHE.size() + " 个"), true);
    }

    private static void applySnapshot(final MinecraftServer server, final List<StructureEntry> entries) {
        final StructureSnapshotPayload payload = new StructureSnapshotPayload(entries, true);
        LAST_SENT.clear();
        for (final StructureEntry entry : entries) {
            if (entry.loaded()) {
                LAST_SENT.put(entry.id(), new SentState(entry, tickCounter));
            }
        }
        for (final UUID id : List.copyOf(SUBSCRIBERS.keySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                SUBSCRIBERS.remove(id);
                continue;
            }
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static final class DirectoryObserver implements SubLevelObserver {

        private final ServerLevel level;

        private DirectoryObserver(final ServerLevel level) {
            this.level = level;
        }

        @Override
        public void onSubLevelAdded(final SubLevel sub) {
            if (!(sub instanceof ServerSubLevel server)
                    || server.getLevel() != level
                    || server.isRemoved()) {
                return;
            }
            PlacementOwnerTracker.tryAssign(level, server);
            StructureCreatorTracker.tryAssign(level, server);
            final StructureEntry entry = StructureScanner.describe(level, server);
            if (entry == null) {
                return;
            }
            LAST_SENT.put(entry.id(), new SentState(entry, tickCounter));
            broadcastUpsert(level.getServer(), entry);
        }

        @Override
        public void onSubLevelRemoved(final SubLevel sub, final SubLevelRemovalReason reason) {
            if (!(sub instanceof ServerSubLevel server)) {
                return;
            }
            if (LAST_SENT.remove(server.getUniqueId()) == null) {
                return;
            }
            final MinecraftServer minecraftServer = level.getServer();
            if (minecraftServer != null) {
                broadcastRemoved(minecraftServer, List.of(server.getUniqueId()));
            }
        }
    }

    private static void broadcastUpsert(final MinecraftServer server, final StructureEntry entry) {
        if (server == null || SUBSCRIBERS.isEmpty()) {
            return;
        }
        final StructureDeltaPayload payload = new StructureDeltaPayload(List.of(entry), List.of());
        for (final UUID subscriber : List.copyOf(SUBSCRIBERS.keySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(subscriber);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void setSubscribed(final ServerPlayer player, final boolean enabled) {
        dev.sublevelmanager.SublevelManager.LOGGER.info(
                "[sublevel-manager] subscription {} for {}", enabled, player.getGameProfile().getName());
        if (enabled) {
            final boolean first = SUBSCRIBERS.isEmpty();
            SUBSCRIBERS.put(player.getUUID(), Boolean.TRUE);
            if (first) {
                LAST_SENT.clear();
            }
            sendSnapshot(player);
        } else {
            SUBSCRIBERS.remove(player.getUUID());
            if (SUBSCRIBERS.isEmpty()) {
                LAST_SENT.clear();
            }
        }
    }

    public static void remove(final ServerPlayer player) {
        SUBSCRIBERS.remove(player.getUUID());
        if (SUBSCRIBERS.isEmpty()) {
            LAST_SENT.clear();
        }
    }

    public static void tick(final MinecraftServer server) {
        tickCounter++;
        if (++scanCounter < SCAN_TICKS) {
            return;
        }
        scanCounter = 0;
        if (SUBSCRIBERS.isEmpty()) {
            return;
        }

        final List<StructureEntry> current = StructureScanner.scanLoaded(server);
        final Map<UUID, StructureEntry> currentById = new HashMap<>();
        for (final StructureEntry entry : current) {
            currentById.put(entry.id(), entry);
        }

        final List<StructureEntry> upserts = new ArrayList<>();
        for (final StructureEntry entry : current) {
            final SentState previous = LAST_SENT.get(entry.id());
            final boolean moved = previous == null || moved(previous.entry(), entry);
            final boolean changed = previous == null || !previous.entry().equals(entry);
            final boolean refresh = previous == null
                    || tickCounter - previous.sentTick() >= STATIC_REFRESH_TICKS;
            if (moved || changed || refresh) {
                upserts.add(entry);
                LAST_SENT.put(entry.id(), new SentState(entry, tickCounter));
            }
        }

        final List<UUID> removed = new ArrayList<>();
        final Set<UUID> currentIds = new HashSet<>(currentById.keySet());
        for (final UUID previousId : List.copyOf(LAST_SENT.keySet())) {
            if (!currentIds.contains(previousId)) {
                removed.add(previousId);
                LAST_SENT.remove(previousId);
            }
        }

        if (upserts.isEmpty() && removed.isEmpty()) {
            return;
        }
        if (!removed.isEmpty()) {
            // A parent deletion can cascade into child sub-levels, including children that
            // were already unloaded. A loaded-only delta cannot remove those stale markers,
            // so take one authoritative full snapshot after any disappearance.
            refreshAll(server);
            return;
        }
        final StructureDeltaPayload payload = new StructureDeltaPayload(upserts, removed);
        for (final UUID id : List.copyOf(SUBSCRIBERS.keySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                SUBSCRIBERS.remove(id);
                if (SUBSCRIBERS.isEmpty()) {
                    LAST_SENT.clear();
                }
                continue;
            }
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static boolean moved(final StructureEntry previous, final StructureEntry current) {
        return Math.abs(previous.x() - current.x()) > MOVED_THRESHOLD
                || Math.abs(previous.y() - current.y()) > MOVED_THRESHOLD
                || Math.abs(previous.z() - current.z()) > MOVED_THRESHOLD;
    }

    /** Immediate client-side removal; the full refresh follows as authoritative reconciliation. */
    private static void broadcastRemoved(final MinecraftServer server, final Collection<UUID> ids) {
        if (ids.isEmpty() || SUBSCRIBERS.isEmpty()) {
            return;
        }
        final StructureDeltaPayload payload = new StructureDeltaPayload(List.of(), List.copyOf(ids));
        for (final UUID subscriber : List.copyOf(SUBSCRIBERS.keySet())) {
            final ServerPlayer player = server.getPlayerList().getPlayer(subscriber);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** Sends a fresh full snapshot to every subscriber; used after cascading deletions. */
    public static void refreshAll(final MinecraftServer server) {
        if (SUBSCRIBERS.isEmpty()) {
            return;
        }
        applySnapshot(server, snapshotEntries(server));
    }

    public static void sendSnapshot(final ServerPlayer player) {
        final List<StructureEntry> entries = snapshotEntries(player.server);
        PacketDistributor.sendToPlayer(player, new StructureSnapshotPayload(entries, true));
        dev.sublevelmanager.SublevelManager.LOGGER.info(
                "[sublevel-manager] sent full snapshot ({} structures) to {}",
                entries.size(), player.getGameProfile().getName());
        LAST_SENT.clear();
        for (final StructureEntry entry : entries) {
            if (entry.loaded()) {
                LAST_SENT.put(entry.id(), new SentState(entry, tickCounter));
            }
        }
    }

    public static void handleAction(final ServerPlayer player, final StructureActionPayload payload) {
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("没有权限操作物理结构"), true);
            return;
        }
        final LoadedStructure found = findLoaded(player, payload.id());
        if (found == null) {
            if (payload.action() == StructureActionPayload.Action.TELEPORT
                    && UnloadedStructureScanner.teleport(player, payload.id())) {
                player.displayClientMessage(Component.literal("已传送到未加载结构的保存位置"), true);
            } else if (payload.action() == StructureActionPayload.Action.DELETE
                    && UnloadedStructureScanner.delete(player.server, payload.id())) {
                LAST_SENT.remove(payload.id());
                UNLOADED_CACHE.removeIf(entry -> entry.id().equals(payload.id()));
                broadcastRemoved(player.server, List.of(payload.id()));
                refreshAll(player.server);
                player.displayClientMessage(Component.literal("已删除未加载结构（区域文件已备份）"), true);
            } else {
                player.displayClientMessage(Component.literal("该结构未加载，暂不能执行此操作"), true);
            }
            return;
        }
        switch (payload.action()) {
            case TELEPORT -> {
                final var pos = found.sub().logicalPose().position();
                player.teleportTo(found.level(), pos.x, pos.y + 1.0, pos.z,
                        player.getYRot(), player.getXRot());
                player.displayClientMessage(Component.literal("已传送到结构位置"), true);
            }
            case DELETE -> {
                final List<UUID> removed = deleteConnected(found);
                broadcastRemoved(player.server, removed);
                refreshAll(player.server);
                player.displayClientMessage(Component.literal(
                        "已删除结构（含连接子结构 " + removed.size() + " 个）"), true);
            }
        }
    }

    /** Command path: teleport to a loaded or saved structure by UUID. */
    public static boolean teleport(final ServerPlayer player, final UUID id) {
        if (!player.hasPermissions(2)) {
            return false;
        }
        final LoadedStructure found = findLoaded(player, id);
        if (found != null) {
            final var pos = found.sub().logicalPose().position();
            player.teleportTo(found.level(), pos.x, pos.y + 1.0, pos.z,
                    player.getYRot(), player.getXRot());
            return true;
        }
        return UnloadedStructureScanner.teleport(player, id);
    }

    /** Batch delete; connected chains are removed once and the directory refreshes once. */
    public static int deleteBatch(final ServerPlayer player, final Collection<UUID> ids) {
        if (!player.hasPermissions(2)) {
            return 0;
        }
        final Set<UUID> removedIds = new HashSet<>();
        final Set<UUID> seen = new HashSet<>();
        for (final UUID id : ids) {
            if (!seen.add(id)) {
                continue;
            }
            final LoadedStructure found = findLoaded(player, id);
            if (found != null) {
                removedIds.addAll(deleteConnected(found));
            } else if (UnloadedStructureScanner.delete(player.server, id)) {
                removedIds.add(id);
                UNLOADED_CACHE.removeIf(entry -> entry.id().equals(id));
            }
        }
        broadcastRemoved(player.server, removedIds);
        refreshAll(player.server);
        player.displayClientMessage(Component.literal(
                "批量删除完成，共移除 " + removedIds.size() + " 个结构"), true);
        return removedIds.size();
    }

    /**
     * Deletes the target and every Sable sub-level connected to it by the same graph Sable
     * exposes for ropes/constraints. Sable's public {@code getConnectedChain} is the closest
     * match to the toolgun's unified removal behavior without depending on toolgun classes.
     */
    private static List<UUID> deleteConnected(final LoadedStructure found) {
        final Collection<SubLevel> chain = SubLevelHelper.getConnectedChain(found.sub());
        final Set<ServerSubLevel> targets = new java.util.LinkedHashSet<>();
        if (chain != null) {
            for (final SubLevel sub : chain) {
                if (sub instanceof ServerSubLevel server
                        && server.getLevel() == found.level()
                        && !server.isRemoved()) {
                    targets.add(server);
                }
            }
        }
        if (targets.isEmpty() && !found.sub().isRemoved()) {
            targets.add(found.sub());
        }
        final List<UUID> removed = new ArrayList<>();
        for (final ServerSubLevel target : targets) {
            if (target.isRemoved()) {
                continue;
            }
            removed.add(target.getUniqueId());
            found.container().removeSubLevel(target, SubLevelRemovalReason.REMOVED);
        }
        found.container().processSubLevelRemovals();
        return removed;
    }

    private static LoadedStructure findLoaded(final ServerPlayer player, final UUID id) {        for (final ServerLevel level : player.server.getAllLevels()) {
            final ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                continue;
            }
            for (final ServerSubLevel sub : container.getAllSubLevels()) {
                if (sub.getUniqueId().equals(id)) {
                    return new LoadedStructure(level, container, sub);
                }
            }
        }
        return null;
    }

    private record LoadedStructure(
            ServerLevel level,
            ServerSubLevelContainer container,
            ServerSubLevel sub) {
    }

    private record SentState(StructureEntry entry, long sentTick) {
    }
}
