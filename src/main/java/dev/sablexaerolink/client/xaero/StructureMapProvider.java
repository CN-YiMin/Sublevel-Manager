package dev.sablexaerolink.client.xaero;

import dev.sablexaerolink.client.ClientSettings;
import dev.sablexaerolink.client.ClientStructureCache;
import dev.sablexaerolink.network.StructureEntry;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import xaero.map.element.MapElementRenderProvider;
import xaero.map.element.render.ElementRenderLocation;

public final class StructureMapProvider extends MapElementRenderProvider<StructureMapElement, StructureMapContext> {

    private final StructureMapContext context;
    private Iterator<StructureEntry> iterator = Collections.emptyIterator();

    public StructureMapProvider(final StructureMapContext context) {
        this.context = context;
    }

    @Override
    public void begin(final int location, final StructureMapContext context) {
        begin(ElementRenderLocation.fromIndex(location), context);
    }

    @Override
    public boolean hasNext(final int location, final StructureMapContext context) {
        return hasNext(ElementRenderLocation.fromIndex(location), context);
    }

    @Override
    public StructureMapElement getNext(final int location, final StructureMapContext context) {
        return getNext(ElementRenderLocation.fromIndex(location), context);
    }

    @Override
    public void end(final int location, final StructureMapContext context) {
        end(ElementRenderLocation.fromIndex(location), context);
    }

    @Override
    public void begin(final ElementRenderLocation location, final StructureMapContext ignored) {
        if (!ClientSettings.overlayEnabled()) {
            iterator = Collections.emptyIterator();
            return;
        }
        final List<StructureEntry> entries = ClientStructureCache.entries();
        iterator = entries.stream()
                .filter(entry -> context.matches(new StructureMapElement(entry)))
                .iterator();
    }

    @Override
    public boolean hasNext(final ElementRenderLocation location, final StructureMapContext ignored) {
        return iterator.hasNext();
    }

    @Override
    public StructureMapElement getNext(final ElementRenderLocation location, final StructureMapContext ignored) {
        return new StructureMapElement(iterator.next());
    }

    @Override
    public void end(final ElementRenderLocation location, final StructureMapContext ignored) {
        iterator = Collections.emptyIterator();
    }
}
