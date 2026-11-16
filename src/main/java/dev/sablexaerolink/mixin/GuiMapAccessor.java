package dev.sablexaerolink.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

/** Optional bridge for centering the Xaero world map from our structure list. */
@Mixin(GuiMap.class)
public interface GuiMapAccessor {

    @Accessor("cameraX")
    void sableXaeroLink$setCameraX(double value);

    @Accessor("cameraZ")
    void sableXaeroLink$setCameraZ(double value);

    @Accessor("cameraDestination")
    void sableXaeroLink$setCameraDestination(int[] value);
}
