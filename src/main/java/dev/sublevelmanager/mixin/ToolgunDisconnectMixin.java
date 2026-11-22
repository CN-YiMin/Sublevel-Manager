package dev.sublevelmanager.mixin;

import com.enxv.aeronauticsstructuretool.DisconnectSubLevelPayload;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toolgun disconnect/split: every fragment created by this handler belongs to the remover. */
@Pseudo
@Mixin(targets = "com.enxv.aeronauticsstructuretool.network.handler.ConstraintToolPayloadHandlers", remap = false)
public abstract class ToolgunDisconnectMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "lambda$handleDisconnect$0", at = @At("HEAD"), require = 0)
    private static void sublevelManager$beginDisconnect(
            final IPayloadContext context,
            final DisconnectSubLevelPayload payload,
            final CallbackInfo callback) {
        if (context.player() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel level) {
            SESSION.set(StructureOwnershipService.begin(level, player, "split", ""));
        }
    }

    @Inject(method = "lambda$handleDisconnect$0", at = @At("RETURN"), require = 0)
    private static void sublevelManager$endDisconnect(
            final IPayloadContext context,
            final DisconnectSubLevelPayload payload,
            final CallbackInfo callback) {
        final var session = SESSION.get();
        if (session != null) {
            SESSION.remove();
            StructureOwnershipService.finish(session);
        }
    }
}
