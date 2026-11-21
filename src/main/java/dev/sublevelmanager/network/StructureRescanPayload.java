package dev.sublevelmanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Explicit request to scan unloaded Sable structures and return a full snapshot. */
public record StructureRescanPayload() implements CustomPacketPayload {

    public static final Type<StructureRescanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("sublevel_manager", "rescan_unloaded"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureRescanPayload> STREAM_CODEC =
            StreamCodec.unit(new StructureRescanPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
