package dev.sublevelmanager.mixin;

import dev.freightera.blueprint.ShipBlueprint;
import dev.freightera.compat.ShipPlacement;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures HNG's central blueprint/vehicle-package placement entry, which already has the player. */
@Pseudo
@Mixin(targets = "dev.freightera.compat.ShipPlacement", remap = false)
public abstract class HngShipPlacementMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"), require = 0)
    private static void sublevelManager$begin(
            final ServerPlayer player,
            final ServerLevel level,
            final ShipBlueprint blueprint,
            final byte[] bytes,
            final Vec3 target,
            final CallbackInfoReturnable<ShipPlacement.Result> callback) {
        if (PlacementSessionRegistry.active(level).isPresent()) {
            return;
        }
        SESSION.set(StructureOwnershipService.begin(level, player, "hng_blueprint", blueprint.id()));
    }

    @Inject(method = "place", at = @At("RETURN"), require = 0)
    private static void sublevelManager$end(
            final ServerPlayer player,
            final ServerLevel level,
            final ShipBlueprint blueprint,
            final byte[] bytes,
            final Vec3 target,
            final CallbackInfoReturnable<ShipPlacement.Result> callback) {
        final PlacementSessionRegistry.PlacementSession session = SESSION.get();
        if (session != null) {
            StructureOwnershipService.finish(session);
            SESSION.remove();
        }
    }
}
