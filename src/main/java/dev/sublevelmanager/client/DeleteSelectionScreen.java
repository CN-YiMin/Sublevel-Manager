package dev.sublevelmanager.client;

import dev.sublevelmanager.network.StructureBatchDeletePayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Confirmation for deleting all selected structures. */
public final class DeleteSelectionScreen extends Screen {

    private final Screen parent;
    private final List<UUID> ids;

    public DeleteSelectionScreen(final Screen parent, final List<UUID> ids) {
        super(Component.literal("删除选中的物理结构"));
        this.parent = parent;
        this.ids = List.copyOf(ids);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("取消"), button -> onClose())
                .bounds(width / 2 - 110, height / 2 + 40, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("确认删除"), button -> {
            ClientStructureCache.removeLocal(ids);
            PacketDistributor.sendToServer(new StructureBatchDeletePayload(ids));
            onClose();
        }).bounds(width / 2 + 10, height / 2 + 40, 100, 20).build());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("将删除 " + ids.size() + " 个结构，包含整条连接链"),
                width / 2, height / 2 - 16, 0xFFFFD24A);
        graphics.drawCenteredString(font,
                Component.literal("删除前会按服务器规则备份未加载区域文件"),
                width / 2, height / 2 + 6, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
