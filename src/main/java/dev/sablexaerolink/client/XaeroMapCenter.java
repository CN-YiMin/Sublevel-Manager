package dev.sablexaerolink.client;

import dev.sablexaerolink.SableXaeroLink;
import dev.sablexaerolink.mixin.GuiMapAccessor;
import dev.sablexaerolink.network.StructureEntry;
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
            accessor.sableXaeroLink$setCameraX(entry.x());
            accessor.sableXaeroLink$setCameraZ(entry.z());
            accessor.sableXaeroLink$setCameraDestination(new int[] {
                    (int) Math.floor(entry.x()),
                    (int) Math.floor(entry.z())
            });
            Minecraft.getInstance().setScreen(parent);
            return true;
        } catch (final RuntimeException exception) {
            SableXaeroLink.LOGGER.warn("[shtreimel-plus] could not center Xaero map", exception);
            return false;
        }
    }
}
