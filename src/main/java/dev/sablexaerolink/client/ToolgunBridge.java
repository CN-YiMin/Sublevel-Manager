package dev.sablexaerolink.client;

import com.enxv.aeronauticsstructuretool.SyncQueryVehiclesPayload;
import com.enxv.aeronauticsstructuretool.client.screen.ToolModeScreen;
import com.enxv.aeronauticsstructuretool.client.screen.toolmode.ToolModeVehicleQueryController;
import dev.sablexaerolink.SableXaeroLink;
import dev.sablexaerolink.mixin.ToolModeScreenAccessor;
import dev.sablexaerolink.network.StructureEntry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Opens the toolgun's native query panel when that optional mod is installed. */
public final class ToolgunBridge {

    private ToolgunBridge() {
    }

    public static void openQueryPanel() {
        try {
            Minecraft.getInstance().setScreen(new ToolModeScreen());
        } catch (final RuntimeException exception) {
            SableXaeroLink.LOGGER.warn("[shtreimel-plus] could not open toolgun query panel", exception);
        }
    }

    public static void preview(final StructureEntry entry) {
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            final ToolModeScreen screen = new ToolModeScreen();
            minecraft.setScreen(screen);
            final ToolModeScreenAccessor accessor = (ToolModeScreenAccessor) (Object) screen;
            final ToolModeVehicleQueryController query = accessor.sableXaeroLink$vehicleQuery();
            final BlockPos pos = BlockPos.containing(entry.x(), entry.y(), entry.z());
            final String name = entry.name().isBlank()
                    ? entry.id().toString().substring(0, 8)
                    : entry.name();
            query.state().replaceEntries(List.of(new SyncQueryVehiclesPayload.Entry(
                    entry.id(), name, name, 0.0, pos, entry.loaded(), false)));
            query.select(entry.id(), minecraft, accessor.sableXaeroLink$previewView());
        } catch (final RuntimeException exception) {
            SableXaeroLink.LOGGER.warn("[shtreimel-plus] could not select structure in toolgun", exception);
            openQueryPanel();
        }
    }
}
