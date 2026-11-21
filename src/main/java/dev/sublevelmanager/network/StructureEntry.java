package dev.sublevelmanager.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One physical structure as the server knows it.
 *
 * @param id        Sable sub-level UUID
 * @param name      display name, possibly empty
 * @param dimension dimension id such as {@code minecraft:overworld}
 * @param x         logical world X, not the plot X
 * @param y         logical world Y
 * @param z         logical world Z
 * @param loaded    true when the sub-level is currently loaded
 * @param chainId   stable id shared by rope/constraint-connected sub-levels; empty for unloaded
 * @param ownerId   owner UUID string, empty when unknown
 * @param ownerName owner display name, empty when unknown
 */
public record StructureEntry(
        UUID id,
        String name,
        String dimension,
        double x,
        double y,
        double z,
        boolean loaded,
        String chainId,
        String ownerId,
        String ownerName) {

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureEntry> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, entry) -> {
                        buffer.writeUUID(entry.id());
                        buffer.writeUtf(entry.name());
                        buffer.writeUtf(entry.dimension());
                        buffer.writeDouble(entry.x());
                        buffer.writeDouble(entry.y());
                        buffer.writeDouble(entry.z());
                        buffer.writeBoolean(entry.loaded());
                        buffer.writeUtf(entry.chainId());
                        buffer.writeUtf(entry.ownerId());
                        buffer.writeUtf(entry.ownerName());
                    },
                    buffer -> new StructureEntry(
                            buffer.readUUID(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readBoolean(),
                            buffer.readUtf(),
                            buffer.readUtf(),
                            buffer.readUtf()));

    public String ownerLabel() {
        if (!ownerName.isBlank()) {
            return ownerName;
        }
        if (!ownerId.isBlank()) {
            return ownerId;
        }
        return "未知";
    }
}
