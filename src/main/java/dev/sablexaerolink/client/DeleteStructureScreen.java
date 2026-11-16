package dev.sablexaerolink.client;

import dev.sablexaerolink.network.StructureActionPayload;
import dev.sablexaerolink.network.StructureEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Second step for a destructive map action; the map right-click never deletes immediately. */
public final class DeleteStructureScreen extends Screen {

    private final Screen parent;
    private final StructureEntry entry;

    public DeleteStructureScreen(final Screen parent, final StructureEntry entry) {
        super(Component.literal("删除物理结构"));
        this.parent = parent;
        this.entry = entry;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("取消"), button -> onClose())
                .bounds(width / 2 - 110, height / 2 + 40, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("确认删除"), button -> {
            ClientStructureCache.removeLocal(java.util.List.of(entry.id()));
            PacketDistributor.sendToServer(new StructureActionPayload(
                    entry.id(), StructureActionPayload.Action.DELETE));
            onClose();
        }).bounds(width / 2 + 10, height / 2 + 40, 100, 20).build());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal(entry.ownerLabel() + " · "
                        + (entry.name().isBlank() ? entry.id().toString() : entry.name())),
                width / 2, height / 2 - 28, 0xFFFFD24A);
        graphics.drawCenteredString(font,
                Component.literal(entry.loaded() ? "结构已加载：将从世界中移除" : "结构未加载：逻辑删除，区域文件会先备份"),
                width / 2, height / 2 - 10, 0xFFAAAAAA);
        graphics.drawCenteredString(font, Component.literal(entry.id().toString()),
                width / 2, height / 2 + 8, 0xFF888888);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
