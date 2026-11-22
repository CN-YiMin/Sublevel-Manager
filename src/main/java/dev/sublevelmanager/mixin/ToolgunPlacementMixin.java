package dev.sublevelmanager.mixin;

import com.enxv.aeronauticsstructuretool.PlacementSnapMode;
import com.enxv.aeronauticsstructuretool.blueprint.model.NativeBlueprintPlacementResult;
import com.enxv.aeronauticsstructuretool.blueprint.placement.BlueprintPlacementAttachment;
import com.enxv.aeronauticsstructuretool.blueprint.placement.BlueprintVerticalPlacement;
import com.enxv.aeronauticsstructuretool.blueprint.placement.NativeBlueprintPlacementService;
import com.enxv.aeronauticsstructuretool.blueprint.runtime.BlueprintPlacementObserver;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the Toolgun's own blueprint loader when it supplies an actor UUID. */
@Pseudo
@Mixin(targets = "com.enxv.aeronauticsstructuretool.blueprint.placement.NativeBlueprintPlacementService", remap = false)
public abstract class ToolgunPlacementMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"), require = 0)
    private static void sublevelManager$begin(
            final ServerLevel level,
            final BlockPos pos,
            final Direction direction,
            final String name,
            final byte[] data,
            final double x,
            final double y,
            final double z,
            final int a,
            final int b,
            final int c,
            final int d,
            final int e,
            final PlacementSnapMode snap,
            final BlueprintPlacementAttachment attachment,
            final BlueprintVerticalPlacement vertical,
            final UUID actor,
            final BlueprintPlacementObserver observer,
            final CallbackInfoReturnable<NativeBlueprintPlacementResult> callback) {
        if (actor == null || PlacementSessionRegistry.active(level).isPresent()) {
            return;
        }
        final ServerPlayer player = level.getServer() == null
                ? null
                : level.getServer().getPlayerList().getPlayer(actor);
        if (player != null) {
            SESSION.set(StructureOwnershipService.begin(level, player, "toolgun", name));
        }
    }

    @Inject(method = "place", at = @At("RETURN"), require = 0)
    private static void sublevelManager$end(
            final ServerLevel level,
            final BlockPos pos,
            final Direction direction,
            final String name,
            final byte[] data,
            final double x,
            final double y,
            final double z,
            final int a,
            final int b,
            final int c,
            final int d,
            final int e,
            final PlacementSnapMode snap,
            final BlueprintPlacementAttachment attachment,
            final BlueprintVerticalPlacement vertical,
            final UUID actor,
            final BlueprintPlacementObserver observer,
            final CallbackInfoReturnable<NativeBlueprintPlacementResult> callback) {
        final PlacementSessionRegistry.PlacementSession session = SESSION.get();
        if (session != null) {
            StructureOwnershipService.finish(session);
            SESSION.remove();
        }
    }
}
