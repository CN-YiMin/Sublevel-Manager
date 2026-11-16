package dev.sablexaerolink.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A client-side map action. The server re-checks permission and whether the structure is loaded. */
public record StructureActionPayload(UUID id, Action action) implements CustomPacketPayload {

    public enum Action {
        TELEPORT,
        DELETE,
        TOGGLE_FREEZE
    }

    public static final Type<StructureActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("shtreimel_plus", "action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeUUID(payload.id());
                        buffer.writeEnum(payload.action());
                    },
                    buffer -> new StructureActionPayload(buffer.readUUID(), buffer.readEnum(Action.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
