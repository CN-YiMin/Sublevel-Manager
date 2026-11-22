package dev.sublevelmanager.mixin;

import dev.sublevelmanager.server.PlacementIntentRegistry;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the native Sable assembler; the player comes from the recent interaction intent. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SableAssemblyPlacementMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "assembleBlocks", at = @At("HEAD"), require = 0)
    private static void sublevelManager$beginAssembly(
            final ServerLevel level,
            final BlockPos origin,
            final Iterable<BlockPos> blocks,
            final BoundingBox3ic bounds,
            final CallbackInfoReturnable<ServerSubLevel> callback) {
        final var intent = PlacementIntentRegistry.findLatest(level);
        final UUID owner = intent.map(PlacementIntentRegistry.Intent::playerId).orElse(new UUID(0L, 0L));
        final String name = intent.map(PlacementIntentRegistry.Intent::playerName).orElse("system");
        SESSION.set(PlacementSessionRegistry.begin(level, owner, name, "assembler", ""));
    }

    @Inject(method = "assembleBlocks", at = @At("RETURN"), require = 0)
    private static void sublevelManager$endAssembly(
            final ServerLevel level,
            final BlockPos origin,
            final Iterable<BlockPos> blocks,
            final BoundingBox3ic bounds,
            final CallbackInfoReturnable<ServerSubLevel> callback) {
        final var session = SESSION.get();
        if (session != null) {
            SESSION.remove();
            StructureOwnershipService.finish(session);
        }
    }
}
