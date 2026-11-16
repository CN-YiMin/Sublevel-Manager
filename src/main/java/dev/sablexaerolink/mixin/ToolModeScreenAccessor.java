package dev.sablexaerolink.mixin;

import com.enxv.aeronauticsstructuretool.client.screen.StructurePreviewViewState;
import com.enxv.aeronauticsstructuretool.client.screen.ToolModeScreen;
import com.enxv.aeronauticsstructuretool.client.screen.toolmode.ToolModeVehicleQueryController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Optional bridge: lets the map select one structure in the toolgun query panel. */
@Mixin(ToolModeScreen.class)
public interface ToolModeScreenAccessor {

    @Accessor("vehicleQuery")
    ToolModeVehicleQueryController sableXaeroLink$vehicleQuery();

    @Accessor("previewView")
    StructurePreviewViewState sableXaeroLink$previewView();
}
