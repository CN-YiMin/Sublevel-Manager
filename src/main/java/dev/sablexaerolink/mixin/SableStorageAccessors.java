package dev.sablexaerolink.mixin;

import dev.ryanhcode.sable.sublevel.storage.region.SubLevelStorageFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import java.io.IOException;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Sable's storage file has no public per-sub-level delete; this is the narrow accessor used after a backup. */
public final class SableStorageAccessors {

    private SableStorageAccessors() {
    }

    @Mixin(SubLevelStorage.class)
    public interface Storage {
        @Invoker("getRegionStorageFile")
        SubLevelStorageFile sableXaeroLink$regionStorageFile(ChunkPos chunk, int storageIndex) throws IOException;
    }

    @Mixin(SubLevelStorageFile.class)
    public interface File {
        @Invoker("clear")
        void sableXaeroLink$clear(int subLevelIndex) throws IOException;
    }
}
