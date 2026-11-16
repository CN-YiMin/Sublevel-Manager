package dev.sablexaerolink.network;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Incremental updates for structures that moved, changed metadata, or disappeared. */
public record StructureDeltaPayload(
        List<StructureEntry> upserts,
        List<UUID> removed) implements CustomPacketPayload {

    public static final Type<StructureDeltaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("shtreimel_plus", "delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureDeltaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    StructureEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureDeltaPayload::upserts,
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureDeltaPayload::removed,
                    StructureDeltaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
