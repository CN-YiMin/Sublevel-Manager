package dev.sublevelmanager.client.xaero;

import dev.sublevelmanager.network.StructureEntry;

/** Thin wrapper so Xaero's generic element machinery has a concrete type. */
public record StructureMapElement(StructureEntry entry) {
}
