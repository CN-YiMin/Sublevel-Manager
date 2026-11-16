package dev.sablexaerolink.client.xaero;

import dev.sablexaerolink.network.StructureEntry;

/** Thin wrapper so Xaero's generic element machinery has a concrete type. */
public record StructureMapElement(StructureEntry entry) {
}
