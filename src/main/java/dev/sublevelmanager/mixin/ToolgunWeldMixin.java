package dev.sublevelmanager.mixin;

import com.enxv.aeronauticsstructuretool.toolgun.weld.SimpleWeldRequest;
import com.enxv.aeronauticsstructuretool.toolgun.weld.WeldRequest;
import com.enxv.aeronauticsstructuretool.toolgun.weld.WeldToolService;
import dev.sublevelmanager.server.PlacementIntentRegistry;
import dev.sublevelmanager.server.PlacementSessionRegistry;
import dev.sublevelmanager.server.StructureOwnershipService;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toolgun weld: the initiator owns every body involved in the weld. */
@Pseudo
@Mixin(targets = "com.enxv.aeronauticsstructuretool.toolgun.weld.WeldToolService", remap = false)
public abstract class ToolgunWeldMixin {

    private static final ThreadLocal<PlacementSessionRegistry.PlacementSession> SESSION = new ThreadLocal<>();

    @Inject(method = "weld", at = @At("HEAD"), require = 0)
    private static void sublevelManager$beginWeld(
            final ServerLevel level, final WeldRequest request, final CallbackInfo callback) {
        begin(level);
    }

    @Inject(method = "weld", at = @At("RETURN"), require = 0)
    private static void sublevelManager$endWeld(
            final ServerLevel level, final WeldRequest request, final CallbackInfo callback) {
        end(level, List.of(request.firstSubLevelId(), request.secondSubLevelId()));
    }

    @Inject(method = "simpleWeld", at = @At("HEAD"), require = 0)
    private static void sublevelManager$beginSimpleWeld(
            final ServerLevel level, final SimpleWeldRequest request, final CallbackInfo callback) {
        begin(level);
    }

    @Inject(method = "simpleWeld", at = @At("RETURN"), require = 0)
    private static void sublevelManager$endSimpleWeld(
            final ServerLevel level, final SimpleWeldRequest request, final CallbackInfo callback) {
        end(level, List.of(request.childSubLevelId(), request.parentSubLevelId()));
    }

    private static void begin(final ServerLevel level) {
        final var intent = PlacementIntentRegistry.findLatest(level);
        final UUID owner = intent.map(PlacementIntentRegistry.Intent::playerId).orElse(new UUID(0L, 0L));
        final String name = intent.map(PlacementIntentRegistry.Intent::playerName).orElse("system");
        SESSION.set(PlacementSessionRegistry.begin(level, owner, name, "weld", ""));
    }

    private static void end(final ServerLevel level, final List<UUID> ids) {
        final var session = SESSION.get();
        if (session == null) {
            return;
        }
        SESSION.remove();
        StructureOwnershipService.reassign(level, ids, session.ownerId(), session.ownerName(), "weld", "");
        StructureOwnershipService.finish(session);
    }
}
