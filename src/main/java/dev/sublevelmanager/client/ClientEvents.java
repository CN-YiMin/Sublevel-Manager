package dev.sublevelmanager.client;

import dev.sublevelmanager.SublevelManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Client tick/screen hooks that never name an Xaero class directly. */
@EventBusSubscriber(modid = SublevelManager.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        if (ModList.get().isLoaded("xaeroworldmap")) {
            XaeroBridge.tryRegister();
        }
    }

    @SubscribeEvent
    public static void onLogin(final ClientPlayerNetworkEvent.LoggingIn event) {
        ClientSettings.requestInitialSnapshot();
    }

    @SubscribeEvent
    public static void onLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStructureCache.clear();
    }

    @SubscribeEvent
    public static void onScreenRender(final ScreenEvent.Render.Post event) {
        if (ModList.get().isLoaded("xaeroworldmap")) {
            XaeroBridge.onScreenRender(event);
        }
    }

    @SubscribeEvent
    public static void onMousePressed(final ScreenEvent.MouseButtonPressed.Pre event) {
        if (ModList.get().isLoaded("xaeroworldmap")) {
            XaeroBridge.onMousePressed(event);
        }
    }
}
