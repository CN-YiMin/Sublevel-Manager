package dev.sublevelmanager.client;

import dev.sublevelmanager.SublevelManager;
import dev.sublevelmanager.network.StructureSubscriptionPayload;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-side overlay state, persisted in config/sublevel_manager-client.properties. */
public final class ClientSettings {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("sublevel_manager-client.properties");
    private static boolean loaded;
    private static boolean overlayEnabled = true;

    private ClientSettings() {
    }

    public static boolean overlayEnabled() {
        load();
        return overlayEnabled;
    }

    public static void setOverlayEnabled(final boolean enabled) {
        load();
        overlayEnabled = enabled;
        save();
        // Keep the last snapshot while the overlay is off. Turning it back on must
        // immediately show the known structures instead of waiting for a new scan.
        ClientStructureCache.touch();
        PacketDistributor.sendToServer(new StructureSubscriptionPayload(enabled));
        SublevelManager.LOGGER.info("[sublevel-manager] client overlay toggled: {}", enabled);
    }

    public static void requestInitialSnapshot() {
        if (overlayEnabled()) {
            PacketDistributor.sendToServer(new StructureSubscriptionPayload(true));
        }
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        final Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            overlayEnabled = Boolean.parseBoolean(properties.getProperty("overlay", "true"));
        } catch (final IOException exception) {
            SublevelManager.LOGGER.warn("[sublevel-manager] could not read client config", exception);
        }
    }

    private static synchronized void save() {
        final Properties properties = new Properties();
        properties.setProperty("overlay", Boolean.toString(overlayEnabled));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Sublevel Manager client settings");
            }
        } catch (final IOException exception) {
            SublevelManager.LOGGER.warn("[sublevel-manager] could not write client config", exception);
        }
    }
}
