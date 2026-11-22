package dev.sublevelmanager.mixin;

import dev.sublevelmanager.server.PlacementIntentRegistry;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures Sable Schematic API placements; the API has no player argument, so use the recent intent. */
@Pseudo
@Mixin(targets = "dev.rew1nd.sableschematicapi.blueprint.tool.BlueprintToolService", remap = false)
public abstract class SableSchematicPlacementMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "loadBytes", at = @At("HEAD"), require = 0)
    private static void sublevelManager$begin(
            final ServerLevel level,
            final Vec3 at,
            final byte[] data,
            final String name,
            final CallbackInfoReturnable<Object> callback) {
        if (PlacementSessionRegistry.active(level).isPresent()) {
            return;
        }
        final var intent = PlacementIntentRegistry.findNearest(level, at, 32.0);
        if (intent.isPresent()) {
            final var session = PlacementSessionRegistry.begin(
                    level, intent.get().playerId(), intent.get().playerName(), "sable_schematic", name);
            SESSION.set(session);
        }
    }

    @Inject(method = "loadBytes", at = @At("RETURN"), require = 0)
    private static void sublevelManager$end(
            final ServerLevel level,
            final Vec3 at,
            final byte[] data,
            final String name,
            final CallbackInfoReturnable<Object> callback) {
        final PlacementSessionRegistry.PlacementSession session = SESSION.get();
        if (session != null) {
            StructureOwnershipService.finish(session);
            SESSION.remove();
        }
    }
}
