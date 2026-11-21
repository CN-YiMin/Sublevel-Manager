package dev.sublevelmanager.network;

import dev.sublevelmanager.SublevelManager;
import dev.sublevelmanager.client.ClientStructureCache;
import dev.sublevelmanager.server.StructureDirectory;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SublevelManager.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class Network {

    private Network() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        // Optional channels let a client without Sublevel Manager join a server that has it.
        final PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(
                StructureSnapshotPayload.TYPE,
                StructureSnapshotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ClientStructureCache.accept(payload);
                    }
                }));
        registrar.playToClient(
                StructureDeltaPayload.TYPE,
                StructureDeltaPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        ClientStructureCache.applyDelta(payload);
                    }
                }));
        registrar.playToServer(
                StructureSubscriptionPayload.TYPE,
                StructureSubscriptionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StructureDirectory.setSubscribed(player, payload.enabled());
                    }
                }));
        registrar.playToServer(
                StructureActionPayload.TYPE,
                StructureActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StructureDirectory.handleAction(player, payload);
                    }
                }));
        registrar.playToServer(
                StructureBatchDeletePayload.TYPE,
                StructureBatchDeletePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StructureDirectory.deleteBatch(player, payload.ids());
                    }
                }));
        registrar.playToServer(
                StructureRescanPayload.TYPE,
                StructureRescanPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        StructureDirectory.rescanUnloaded(player);
                    }
                }));
    }
}
