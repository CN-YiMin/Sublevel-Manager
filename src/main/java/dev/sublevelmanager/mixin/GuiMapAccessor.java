package dev.sublevelmanager.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.GuiMap;

/** Optional bridge for centering the Xaero world map from our structure list. */
@Mixin(GuiMap.class)
public interface GuiMapAccessor {

    @Accessor("cameraX")
    void sublevelManager$setCameraX(double value);

    @Accessor("cameraZ")
    void sublevelManager$setCameraZ(double value);

    @Accessor("cameraDestination")
    void sublevelManager$setCameraDestination(int[] value);
}
