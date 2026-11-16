package dev.sablexaerolink.server;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Optional Shtreimel freeze integration.
 *
 * <p>All Shtreimel classes are resolved through reflection so a server without
 * Shtreimel never loads a missing class merely by reading the structure directory.
 */
public final class ShtreimelFreezeBridge {

    private static final String FROZEN_SUBLEVELS = "dev.playcdu.shtreimel.freeze.FrozenSubLevels";
    private static boolean resolved;
    private static Method isFrozenMethod;
    private static Method freezeMethod;
    private static Method unfreezeMethod;

    private ShtreimelFreezeBridge() {
    }

    public static boolean available() {
        if (!ModList.get().isLoaded("shtreimel")) {
            return false;
        }
        resolve();
        return isFrozenMethod != null && freezeMethod != null && unfreezeMethod != null;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            final Class<?> type = Class.forName(FROZEN_SUBLEVELS);
            isFrozenMethod = type.getMethod("isFrozen", UUID.class);
            freezeMethod = type.getMethod("freeze", ServerSubLevel.class, ResourceKey.class);
            unfreezeMethod = type.getMethod("unfreeze", UUID.class);
        } catch (final ReflectiveOperationException | LinkageError exception) {
            isFrozenMethod = null;
            freezeMethod = null;
            unfreezeMethod = null;
            dev.sablexaerolink.SableXaeroLink.LOGGER.warn(
                    "[shtreimel-plus] Shtreimel freeze API unavailable", exception);
        }
    }

    public static boolean isFrozen(final UUID id) {
        if (!available()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isFrozenMethod.invoke(null, id));
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /** @return true when the structure is frozen after the call. */
    public static boolean toggle(final ServerSubLevel sub, final ResourceKey<Level> dimension) {
        if (!available()) {
            return false;
        }
        try {
            if (Boolean.TRUE.equals(isFrozenMethod.invoke(null, sub.getUniqueId()))) {
                unfreezeMethod.invoke(null, sub.getUniqueId());
                return false;
            }
            freezeMethod.invoke(null, sub, dimension);
            return Boolean.TRUE.equals(isFrozenMethod.invoke(null, sub.getUniqueId()));
        } catch (final ReflectiveOperationException | RuntimeException exception) {
            dev.sablexaerolink.SableXaeroLink.LOGGER.warn(
                    "[shtreimel-plus] Shtreimel freeze toggle failed", exception);
            return false;
        }
    }
}
