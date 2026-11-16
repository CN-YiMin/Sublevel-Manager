package dev.sablexaerolink.client;

import dev.sablexaerolink.SableXaeroLink;
import dev.sablexaerolink.client.xaero.StructureMapContext;
import dev.sablexaerolink.client.xaero.StructureMapRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import xaero.map.WorldMap;
import xaero.map.gui.GuiMap;

/** Loaded only when xaeroworldmap is present. */
final class XaeroBridge {

    private static final int BUTTON_SIZE = 22;
    private static xaero.map.element.MapElementRenderHandler registeredHandler;

    private XaeroBridge() {
    }

    static void tryRegister() {
        final xaero.map.element.MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
        if (handler == null || handler == registeredHandler) {
            return;
        }
        try {
            handler.add(new StructureMapRenderer(new StructureMapContext()));
            registeredHandler = handler;
            SableXaeroLink.LOGGER.info("[shtreimel-plus] registered Xaero world-map overlay");
        } catch (final RuntimeException exception) {
            SableXaeroLink.LOGGER.warn("[shtreimel-plus] could not register Xaero overlay", exception);
        }
    }

    static void onScreenRender(final ScreenEvent.Render.Post event) {
        final Screen screen = event.getScreen();
        if (!(screen instanceof GuiMap)) {
            return;
        }
        final GuiGraphics graphics = event.getGuiGraphics();
        final int x = screen.width - 28;
        final int y = screen.height / 2 - BUTTON_SIZE / 2;
        graphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0xAA1D1D24);
        graphics.renderOutline(x, y, BUTTON_SIZE, BUTTON_SIZE, 0xFF8B7BFF);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal("S"), x + BUTTON_SIZE / 2, y + 7, 0xFFFFFFFF);
    }

    static boolean onMousePressed(final ScreenEvent.MouseButtonPressed.Pre event) {
        final Screen screen = event.getScreen();
        if (!(screen instanceof GuiMap)) {
            return false;
        }
        final int x = screen.width - 28;
        final int y = screen.height / 2 - BUTTON_SIZE / 2;
        if (event.getMouseX() >= x && event.getMouseX() < x + BUTTON_SIZE
                && event.getMouseY() >= y && event.getMouseY() < y + BUTTON_SIZE) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new StructureConfigScreen(screen));
            event.setCanceled(true);
            return true;
        }
        return false;
    }
}

