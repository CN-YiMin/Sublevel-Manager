package dev.sublevelmanager.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StructureSnapshotPayload(List<StructureEntry> entries, boolean full) implements CustomPacketPayload {

    public static final Type<StructureSnapshotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("sublevel_manager", "snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureSnapshotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    StructureEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), StructureSnapshotPayload::entries,
                    ByteBufCodecs.BOOL, StructureSnapshotPayload::full,
                    StructureSnapshotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
