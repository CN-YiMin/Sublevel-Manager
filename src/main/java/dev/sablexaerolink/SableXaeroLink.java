package dev.sablexaerolink;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Server-authoritative directory of Sable sub-levels, with an optional Xaero world-map overlay.
 *
 * <p>The server side is deliberately independent of Xaero. Client-only classes live under
 * {@code dev.sablexaerolink.client} and are only touched from client event handlers.
 */
@Mod(SableXaeroLink.MOD_ID)
public final class SableXaeroLink {

    public static final String MOD_ID = "shtreimel_plus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SableXaeroLink(final IEventBus modBus) {
        LOGGER.info("[shtreimel-plus] loaded; server directory active, client overlay optional");
    }
}
