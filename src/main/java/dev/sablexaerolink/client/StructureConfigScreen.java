package dev.sablexaerolink.client;

import dev.sablexaerolink.network.StructureActionPayload;
import dev.sablexaerolink.network.StructureEntry;
import dev.sablexaerolink.network.StructureRescanPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

/** Configuration and structure list; the overlay toggle is intentionally the first item. */
public final class StructureConfigScreen extends Screen {

    private final Screen parent;
    private Button toggle;
    private Button teleport;
    private Button waypoint;
    private Button delete;
    private Button center;
    private StructureListWidget list;
    private int refreshCounter;
    private long lastCacheVersion = -1;
    private Runnable cacheListener;

    public StructureConfigScreen(final Screen parent) {
        super(Component.literal("Shtreimel Plus"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        toggle = addRenderableWidget(Button.builder(toggleLabel(), button -> {
            ClientSettings.setOverlayEnabled(!ClientSettings.overlayEnabled());
            button.setMessage(toggleLabel());
        }).bounds(width / 2 - 110, 54, 220, 20).build());

        list = addRenderableWidget(new StructureListWidget(minecraft, 300,
                Math.max(60, height - 230), 100, 30));
        list.setX(width / 2 - 150);
        if (cacheListener != null) {
            ClientStructureCache.removeListener(cacheListener);
        }
        cacheListener = () -> {
            if (list != null) {
                list.reload();
            }
        };
        ClientStructureCache.addListener(cacheListener);

        final int bottom = height - 70;
        teleport = addRenderableWidget(Button.builder(Component.literal("传送"), button -> {
            final StructureEntry entry = selected();
            if (entry != null) {
                PacketDistributor.sendToServer(new StructureActionPayload(
                        entry.id(), StructureActionPayload.Action.TELEPORT));
            }
        }).bounds(width / 2 - 150, bottom, 90, 20).build());

        waypoint = addRenderableWidget(Button.builder(Component.literal("创建路径点"), button -> {
            final StructureEntry entry = selected();
            if (entry != null && ModList.get().isLoaded("xaerominimap")) {
                XaeroWaypointActions.add(entry);
            }
        }).bounds(width / 2 - 50, bottom, 100, 20).build());

        delete = addRenderableWidget(Button.builder(Component.literal("删除选中"), button -> {
            final List<UUID> ids = list.markedIds();
            if (ids.size() == 1) {
                final StructureEntry entry = ClientStructureCache.entries().stream()
                        .filter(candidate -> candidate.id().equals(ids.getFirst()))
                        .findFirst().orElse(null);
                if (entry != null) {
                    minecraft.setScreen(new DeleteStructureScreen(this, entry));
                }
            } else if (!ids.isEmpty()) {
                minecraft.setScreen(new DeleteSelectionScreen(this, ids));
            }
        }).bounds(width / 2 + 60, bottom, 90, 20).build());

        center = addRenderableWidget(Button.builder(Component.literal("地图居中"), button -> {
            final StructureEntry entry = selected();
            if (entry != null && ModList.get().isLoaded("xaeroworldmap")) {
                XaeroMapCenter.center(parent, entry);
            }
        }).bounds(width / 2 + 160, bottom, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("刷新未加载结构"), button -> {
            PacketDistributor.sendToServer(new StructureRescanPayload());
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("正在扫描未加载结构..."), true);
            }
        }).bounds(width / 2 - 150, height - 110, 180, 20).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), button -> onClose())
                .bounds(width / 2 - 60, height - 40, 120, 20).build());
    }

    private StructureEntry selected() {
        return list == null ? null : list.selectedEntry();
    }

    private Component toggleLabel() {
        return Component.literal("显示物理结构叠加层：" + (ClientSettings.overlayEnabled() ? "开" : "关"));
    }

    @Override
    public void tick() {
        super.tick();
        if (list == null) {
            return;
        }
        final long version = ClientStructureCache.version();
        if (version != lastCacheVersion) {
            lastCacheVersion = version;
            refreshCounter = 0;
            list.reload();
        } else if (++refreshCounter >= 20) {
            refreshCounter = 0;
            list.reload();
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        final boolean hasSelection = selected() != null;
        final boolean hasMarked = !list.markedIds().isEmpty();
        final boolean admin = minecraft != null && minecraft.player != null
                && minecraft.player.hasPermissions(2);
        teleport.active = hasSelection && admin;
        waypoint.active = hasSelection && ModList.get().isLoaded("xaerominimap");
        delete.active = hasMarked && admin;
        center.active = hasSelection && ModList.get().isLoaded("xaeroworldmap");

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("已收到 " + ClientStructureCache.entries().size() + " 个物理结构"),
                width / 2, 82, 0xFFAAAAAA);
        graphics.drawString(font, "点击条目勾选/取消；点击连接组标题选中/取消整组",
                width / 2 - 150, height - 128, 0xFF8A8A8A, false);
    }

    @Override
    public void removed() {
        if (cacheListener != null) {
            ClientStructureCache.removeListener(cacheListener);
            cacheListener = null;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
