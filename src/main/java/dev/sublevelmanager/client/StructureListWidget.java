package dev.sublevelmanager.client;

import dev.sublevelmanager.network.StructureEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

/** Scrollable list grouped by Sable's connected-chain id. */
public final class StructureListWidget extends ObjectSelectionList<StructureListWidget.Row> {

    private final Set<UUID> marked = new LinkedHashSet<>();

    public StructureListWidget(final Minecraft minecraft, final int width, final int height, final int y, final int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        rebuild(null);
    }

    @Override
    public int getRowWidth() {
        return 280;
    }

    @Override
    protected int getScrollbarPosition() {
        return getX() + getWidth() - 6;
    }

    public StructureEntry selectedEntry() {
        final Row row = getSelected();
        return row == null ? null : row.entry();
    }

    public List<UUID> markedIds() {
        return List.copyOf(marked);
    }

    public void clearMarked() {
        marked.clear();
        reload();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        // A button below the list must never be interpreted as a row toggle.
        if (mouseX < getX() || mouseX >= getX() + getWidth()
                || mouseY < getY() || mouseY >= getY() + getHeight()
                || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        final boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return handled;
        }
        final Row row = getSelected();
        if (row == null) {
            return handled;
        }
        if (row.groupIds() != null && !row.groupIds().isEmpty()) {
            if (marked.containsAll(row.groupIds())) {
                marked.removeAll(row.groupIds());
            } else {
                marked.addAll(row.groupIds());
            }
            reload();
            return true;
        }
        if (row.entry() == null) {
            return handled;
        }
        final UUID id = row.entry().id();
        if (!marked.remove(id)) {
            marked.add(id);
        }
        reload();
        return true;
    }

    public void reload() {
        final StructureEntry selected = selectedEntry();
        rebuild(selected);
    }

    private void rebuild(final StructureEntry selected) {
        clearEntries();
        final Map<String, List<StructureEntry>> groups = new LinkedHashMap<>();
        final List<StructureEntry> sorted = new ArrayList<>(ClientStructureCache.entries());
        sorted.sort(Comparator.comparing((StructureEntry entry) -> {
            final String chain = entry.chainId();
            return chain == null || chain.isBlank() ? entry.id().toString() : chain;
        }).thenComparing(StructureEntry::name));

        for (final StructureEntry entry : sorted) {
            final String chain = entry.chainId();
            final String key = chain == null || chain.isBlank() ? entry.id().toString() : chain;
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }

        for (final Map.Entry<String, List<StructureEntry>> group : groups.entrySet()) {
            if (group.getValue().size() > 1) {
                addEntry(Row.header("连接组 " + group.getKey().substring(0, 8)
                        + " · " + group.getValue().size() + " 个结构",
                        group.getValue().stream().map(StructureEntry::id).toList()));
            }
            for (final StructureEntry entry : group.getValue()) {
                final Row row = Row.entry(entry, marked.contains(entry.id()));
                addEntry(row);
                if (selected != null && selected.id().equals(entry.id())) {
                    setSelected(row);
                }
            }
        }
    }

    public static final class Row extends ObjectSelectionList.Entry<Row> {

        private final StructureEntry entry;
        private final String header;
        private final List<UUID> groupIds;
        private final boolean marked;

        private Row(final StructureEntry entry, final String header, final List<UUID> groupIds, final boolean marked) {
            this.entry = entry;
            this.header = header;
            this.groupIds = groupIds;
            this.marked = marked;
        }

        static Row header(final String text, final List<UUID> groupIds) {
            return new Row(null, text, groupIds, false);
        }

        static Row entry(final StructureEntry entry, final boolean marked) {
            return new Row(entry, null, null, marked);
        }

        StructureEntry entry() {
            return entry;
        }

        List<UUID> groupIds() {
            return groupIds;
        }

        @Override
        public void render(
                final GuiGraphics graphics,
                final int index,
                final int top,
                final int left,
                final int width,
                final int height,
                final int mouseX,
                final int mouseY,
                final boolean hovering,
                final float partialTick) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (header != null) {
                graphics.drawString(minecraft.font, header, left + 4, top + 9, 0xFFB9A8FF, false);
                return;
            }
            final String marker = (marked ? "[x] " : "[ ] ")
                    + (entry.loaded() ? "[L] " : "[U] ");
            final String name = entry.name().isBlank()
                    ? entry.id().toString().substring(0, 8)
                    : entry.name();
            final String text = marker + entry.ownerLabel() + " · " + name;
            graphics.drawString(minecraft.font, text, left + 4, top + 4, 0xFFFFFFFF, false);
            graphics.drawString(minecraft.font,
                    String.format("%.0f %.0f %.0f", entry.x(), entry.y(), entry.z()),
                    left + 4, top + 16, 0xFF8A8A8A, false);
        }

        @Override
        public Component getNarration() {
            return header != null
                    ? Component.literal(header)
                    : Component.literal(entry.ownerLabel() + " · " + entry.name());
        }
    }
}
