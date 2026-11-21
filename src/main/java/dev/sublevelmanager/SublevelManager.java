package dev.sublevelmanager;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Server-authoritative directory of Sable sub-levels, with an optional Xaero world-map overlay.
 *
 * <p>The server side is deliberately independent of Xaero. Client-only classes live under
 * {@code dev.sublevelmanager.client} and are only touched from client event handlers.
 */
@Mod(SublevelManager.MOD_ID)
public final class SublevelManager {

    public static final String MOD_ID = "sublevel_manager";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SublevelManager(final IEventBus modBus) {
        LOGGER.info("[sublevel-manager] loaded; server directory active, client overlay optional");
    }
}
