package dev.sublevelmanager.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client asks the server to start or stop sending the directory. */
public record StructureSubscriptionPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<StructureSubscriptionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("sublevel_manager", "subscription"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureSubscriptionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, StructureSubscriptionPayload::enabled,
                    StructureSubscriptionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
