package dev.sablexaerolink.client;

import dev.sablexaerolink.network.StructureEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/**
 * Loaded reflectively only when create_aeronautics_toolgun is present, so the map reader
 * itself never references a toolgun class in an environment without the optional mod.
 */
public final class ToolgunMapOption extends RightClickOption {

    private final StructureEntry entry;

    public ToolgunMapOption(
            final String name,
            final int index,
            final IRightClickableElement target,
            final StructureEntry entry) {
        super(name, Style.EMPTY, index, target);
        this.entry = entry;
    }

    @Override
    public void onAction(final Screen screen) {
        ToolgunBridge.preview(entry);
    }
}
