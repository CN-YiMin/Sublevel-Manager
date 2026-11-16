package dev.sablexaerolink.client;

import dev.sablexaerolink.SableXaeroLink;
import dev.sablexaerolink.network.StructureEntry;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.hud.minimap.waypoint.WaypointColor;

/** Creates a normal Xaero minimap waypoint at a structure. Loaded only when Xaero's Minimap exists. */
public final class XaeroWaypointActions {

    private XaeroWaypointActions() {
    }

    public static boolean add(final StructureEntry entry) {
        return withWaypoint(entry, (session, waypoint, world) -> {
            world.getCurrentSet().add(waypoint);
            session.getWaypointsManager().updateWaypoints();
            return true;
        });
    }

    public static boolean share(final net.minecraft.client.gui.screens.Screen screen, final StructureEntry entry) {
        return withWaypoint(entry, (session, waypoint, world) -> {
            session.getWaypointSharing().shareWaypoint(screen, waypoint, world);
            return true;
        });
    }

    private static boolean withWaypoint(
            final StructureEntry entry,
            final WaypointUser user) {
        try {
            final XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            if (session == null) {
                return false;
            }
            final WaypointsManager manager = session.getWaypointsManager();
            final WaypointWorld world = manager.getCurrentWorld();
            if (world == null || world.getCurrentSet() == null) {
                return false;
            }
            final String label = entry.ownerLabel() + " · " + (entry.name().isBlank()
                    ? entry.id().toString().substring(0, 8)
                    : entry.name());
            final Waypoint waypoint = new Waypoint(
                    (int) Math.floor(entry.x()),
                    (int) Math.floor(entry.y()),
                    (int) Math.floor(entry.z()),
                    label,
                    "",
                    WaypointColor.PURPLE);
            waypoint.setYIncluded(true);
            waypoint.setTemporary(false);
            return user.use(session, waypoint, world);
        } catch (final RuntimeException exception) {
            SableXaeroLink.LOGGER.warn("[shtreimel-plus] Xaero waypoint action failed", exception);
            return false;
        }
    }

    @FunctionalInterface
    private interface WaypointUser {
        boolean use(XaeroMinimapSession session, Waypoint waypoint, WaypointWorld world);
    }
}
