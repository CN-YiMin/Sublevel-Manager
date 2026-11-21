package dev.sublevelmanager.client.xaero;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Shared context for one Xaero map pass. */
public final class StructureMapContext {

    private ResourceKey<Level> dimension;

    public void setDimension(final ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    public boolean matches(final StructureMapElement element) {
        return dimension == null
                || element.entry().dimension().equals(dimension.location().toString());
    }
}
