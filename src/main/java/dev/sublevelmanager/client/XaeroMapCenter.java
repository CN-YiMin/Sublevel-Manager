package dev.sublevelmanager.client;

import dev.sublevelmanager.SublevelManager;
import dev.sublevelmanager.mixin.GuiMapAccessor;
import dev.sublevelmanager.network.StructureEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import xaero.map.gui.GuiMap;

/** Optional bridge: centers an existing Xaero world-map screen on one structure. */
public final class XaeroMapCenter {

    private XaeroMapCenter() {
    }

    public static boolean center(final Screen parent, final StructureEntry entry) {
        if (!(parent instanceof GuiMap map)) {
            return false;
        }
        try {
            final GuiMapAccessor accessor = (GuiMapAccessor) (Object) map;
            accessor.sublevelManager$setCameraX(entry.x());
            accessor.sublevelManager$setCameraZ(entry.z());
            accessor.sublevelManager$setCameraDestination(new int[] {
                    (int) Math.floor(entry.x()),
                    (int) Math.floor(entry.z())
            });
            Minecraft.getInstance().setScreen(parent);
            return true;
        } catch (final RuntimeException exception) {
            SublevelManager.LOGGER.warn("[sublevel-manager] could not center Xaero map", exception);
            return false;
        }
    }
}
