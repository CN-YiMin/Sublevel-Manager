package dev.sablexaerolink.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sablexaerolink.SableXaeroLink;
import dev.sablexaerolink.network.StructureEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = SableXaeroLink.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class StructureCommands {

    private StructureCommands() {
    }

    @SubscribeEvent
    public static void register(final RegisterCommandsEvent event) {
        final LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("shtreimelplus")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("snapshot")
                        .executes(context -> {
                            final ServerPlayer player = context.getSource().getPlayerOrException();
                            StructureDirectory.sendSnapshot(player);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("on")
                        .executes(context -> {
                            StructureDirectory.setSubscribed(context.getSource().getPlayerOrException(), true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(context -> {
                            StructureDirectory.setSubscribed(context.getSource().getPlayerOrException(), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("tp")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .executes(context -> {
                                    final ServerPlayer player = context.getSource().getPlayerOrException();
                                    try {
                                        final UUID id = UUID.fromString(
                                                StringArgumentType.getString(context, "uuid"));
                                        if (StructureDirectory.teleport(player, id)) {
                                            player.displayClientMessage(Component.literal("已传送"), true);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        player.displayClientMessage(Component.literal("找不到该结构"), true);
                                    } catch (final IllegalArgumentException exception) {
                                        player.displayClientMessage(Component.literal("UUID 格式无效"), true);
                                    }
                                    return 0;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("uuids", StringArgumentType.greedyString())
                                .executes(context -> {
                                    final ServerPlayer player = context.getSource().getPlayerOrException();
                                    final String raw = StringArgumentType.getString(context, "uuids");
                                    final List<UUID> ids = new java.util.ArrayList<>();
                                    for (final String token : raw.trim().split("\\s+")) {
                                        try {
                                            ids.add(UUID.fromString(token));
                                        } catch (final IllegalArgumentException ignored) {
                                            player.displayClientMessage(
                                                    Component.literal("跳过无效 UUID: " + token), false);
                                        }
                                    }
                                    if (ids.isEmpty()) {
                                        player.displayClientMessage(Component.literal("没有有效 UUID"), false);
                                        return 0;
                                    }
                                    return StructureDirectory.deleteBatch(player, ids);
                                })))
                .then(Commands.literal("list")
                        .executes(context -> {
                            final ServerPlayer player = context.getSource().getPlayerOrException();
                            final List<StructureEntry> entries = StructureScanner.scanAll(player.server);
                            player.sendSystemMessage(Component.literal(
                                    "Sable structures: " + entries.size()
                                            + " (loaded=" + entries.stream().filter(StructureEntry::loaded).count()
                                            + ", unloaded=" + entries.stream().filter(e -> !e.loaded()).count()
                                            + ")"));
                            entries.stream().limit(50).forEach(entry -> player.sendSystemMessage(
                                    Component.literal((entry.loaded() ? "[L] " : "[U] ")
                                            + entry.ownerLabel() + " | " + entry.name()
                                            + " | " + entry.dimension()
                                            + " | " + String.format("%.1f %.1f %.1f", entry.x(), entry.y(), entry.z())
                                            + " | " + entry.id())));
                            return entries.size();
                        }))
                .then(Commands.literal("diagnose")
                        .executes(context -> {
                            final ServerPlayer player = context.getSource().getPlayerOrException();
                            final List<StructureEntry> entries = StructureScanner.scanAll(player.server);
                            final long loaded = entries.stream().filter(StructureEntry::loaded).count();
                            final long unloaded = entries.size() - loaded;
                            final long owned = entries.stream().filter(e -> !e.ownerId().isBlank()).count();
                            player.sendSystemMessage(Component.literal(
                                    "Shtreimel Plus diagnose: total=" + entries.size()
                                            + " loaded=" + loaded
                                            + " unloaded=" + unloaded
                                            + " owner=" + owned
                                            + " missingOwner=" + (entries.size() - owned)));
                            player.sendSystemMessage(Component.literal(
                                    "Server: Sable=OK, Shtreimel="
                                            + (net.neoforged.fml.ModList.get().isLoaded("shtreimel") ? "OK" : "MISSING")));
                            return entries.size();
                        }))
                .then(Commands.literal("archives")
                        .executes(context -> {
                            final ServerPlayer player = context.getSource().getPlayerOrException();
                            final java.nio.file.Path folder = player.server.getServerDirectory()
                                    .resolve("shtreimel-plus-archive");
                            if (!java.nio.file.Files.isDirectory(folder)) {
                                player.sendSystemMessage(Component.literal("No archive folder yet."));
                                return 0;
                            }
                            try (var files = java.nio.file.Files.list(folder)) {
                                final List<java.nio.file.Path> list = files
                                        .filter(java.nio.file.Files::isRegularFile)
                                        .sorted(java.util.Comparator.comparingLong(
                                                (java.nio.file.Path p) -> p.toFile().lastModified()).reversed())
                                        .limit(20)
                                        .toList();
                                player.sendSystemMessage(Component.literal("Archives: " + list.size()));
                                for (final var file : list) {
                                    player.sendSystemMessage(Component.literal(
                                            file.getFileName() + " (" + file.toFile().length() + " bytes)"));
                                }
                                return list.size();
                            } catch (final java.io.IOException exception) {
                                player.sendSystemMessage(Component.literal("Could not read archive folder."));
                                return 0;
                            }
                        }));
        event.getDispatcher().register(root);
    }
}
