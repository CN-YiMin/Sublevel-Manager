package dev.sublevelmanager.server;

import dev.sublevelmanager.SublevelManager;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = SublevelManager.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerEvents {

    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        StructureDirectory.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        PlacementOwnerTracker.onRightClickBlock(event);
        StructureCreatorTracker.onRightClickBlock(event);
    }

    @SubscribeEvent
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        PlacementOwnerTracker.onRightClickItem(event);
        StructureCreatorTracker.onRightClickItem(event);
    }

    @SubscribeEvent
    public static void onSableContainerReady(final ForgeSableSubLevelContainerReadyEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getContainer() instanceof ServerSubLevelContainer container) {
            StructureDirectory.attach(level, container);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        for (final ServerLevel level : event.getServer().getAllLevels()) {
            final ServerSubLevelContainer container =
                    dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
            if (container != null) {
                StructureDirectory.attach(level, container);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        PlacementOwnerTracker.reset();
        StructureCreatorTracker.reset();
        StructureDirectory.detachAll();
    }

    @SubscribeEvent
    public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            StructureDirectory.remove(player);
        }
    }
}
