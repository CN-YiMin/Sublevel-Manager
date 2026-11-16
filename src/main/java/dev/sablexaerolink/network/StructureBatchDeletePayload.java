package dev.sablexaerolink.network;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Batch delete request from the structure list. */
public record StructureBatchDeletePayload(List<UUID> ids) implements CustomPacketPayload {

    public static final Type<StructureBatchDeletePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("shtreimel_plus", "batch_delete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureBatchDeletePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureBatchDeletePayload::ids,
                    StructureBatchDeletePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
