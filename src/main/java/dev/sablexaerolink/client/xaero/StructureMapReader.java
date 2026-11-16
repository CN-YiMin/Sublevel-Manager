package dev.sablexaerolink.client.xaero;

import java.util.ArrayList;
import java.util.UUID;
import dev.sablexaerolink.client.XaeroWaypointActions;
import dev.sablexaerolink.network.StructureActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import xaero.map.element.MapElementReader;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public final class StructureMapReader extends MapElementReader<StructureMapElement, StructureMapContext, StructureMapRenderer> {

    @Override
    public boolean isHidden(final StructureMapElement element, final StructureMapContext context) {
        return !context.matches(element);
    }

    @Override
    public double getRenderX(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return element.entry().x();
    }

    @Override
    public double getRenderZ(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return element.entry().z();
    }

    @Override
    public int getInteractionBoxLeft(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return -18;
    }

    @Override
    public int getInteractionBoxRight(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return 18;
    }

    @Override
    public int getInteractionBoxTop(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return -18;
    }

    @Override
    public int getInteractionBoxBottom(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return 18;
    }

    @Override
    public int getRenderBoxLeft(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return -18;
    }

    @Override
    public int getRenderBoxRight(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return 18;
    }

    @Override
    public int getRenderBoxTop(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return -18;
    }

    @Override
    public int getRenderBoxBottom(final StructureMapElement element, final StructureMapContext context, final float partialTicks) {
        return 18;
    }

    @Override
    public int getLeftSideLength(final StructureMapElement element, final Minecraft minecraft) {
        return 18;
    }

    @Override
    public String getMenuName(final StructureMapElement element) {
        final String owner = element.entry().ownerLabel();
        final String name = element.entry().name().isBlank()
                ? element.entry().id().toString().substring(0, 8)
                : element.entry().name();
        return owner + " · " + name;
    }

    @Override
    public String getFilterName(final StructureMapElement element) {
        return getMenuName(element);
    }

    @Override
    public int getMenuTextFillLeftPadding(final StructureMapElement element) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(final StructureMapElement element) {
        return 0xFF6D5BD0;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return true;
    }

    @Override
    public boolean isRightClickValid(final StructureMapElement element) {
        return true;
    }

    @Override
    public boolean isInteractable(final int location, final StructureMapElement element) {
        return true;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(
            final StructureMapElement element, final IRightClickableElement target) {
        final ArrayList<RightClickOption> options = new ArrayList<>();
        final boolean admin = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.hasPermissions(2);
        if (admin) {
            options.add(new ActionOption("shtreimel_plus.action.teleport", options.size(), target,
                    element.entry().id(), StructureActionPayload.Action.TELEPORT, true));
            if (element.entry().loaded() && ModList.get().isLoaded("shtreimel")) {
                options.add(new ActionOption("shtreimel_plus.action.freeze", options.size(), target,
                        element.entry().id(), StructureActionPayload.Action.TOGGLE_FREEZE, true));
            }
        }
        if (ModList.get().isLoaded("xaerominimap")) {
            options.add(new WaypointOption("shtreimel_plus.action.waypoint", options.size(), target,
                    element.entry(), false));
            options.add(new WaypointOption("shtreimel_plus.action.share", options.size(), target,
                    element.entry(), true));
        }
        if (ModList.get().isLoaded("create_aeronautics_toolgun")) {
            addToolgunOption(options, target, element.entry());
        }
        if (admin) {
            options.add(new DeleteOption("shtreimel_plus.action.delete", options.size(), target,
                    element.entry()));
        }
        return options;
    }

    private static void addToolgunOption(
            final ArrayList<RightClickOption> options,
            final IRightClickableElement target,
            final dev.sablexaerolink.network.StructureEntry entry) {
        try {
            final Class<?> type = Class.forName("dev.sablexaerolink.client.ToolgunMapOption");
            final var constructor = type.getConstructor(
                    String.class, int.class, IRightClickableElement.class,
                    dev.sablexaerolink.network.StructureEntry.class);
            options.add((RightClickOption) constructor.newInstance(
                    "shtreimel_plus.action.toolgun", options.size(), target, entry));
        } catch (final ReflectiveOperationException ignored) {
            // The optional toolgun integration is simply absent.
        }
    }

    private static final class DeleteOption extends RightClickOption {

        private final dev.sablexaerolink.network.StructureEntry entry;

        private DeleteOption(
                final String name,
                final int index,
                final IRightClickableElement target,
                final dev.sablexaerolink.network.StructureEntry entry) {
            super(name, Style.EMPTY, index, target);
            this.entry = entry;
        }

        @Override
        public void onAction(final Screen screen) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new dev.sablexaerolink.client.DeleteStructureScreen(screen, entry));
        }
    }

    private static final class WaypointOption extends RightClickOption {

        private final dev.sablexaerolink.network.StructureEntry entry;
        private final boolean share;

        private WaypointOption(
                final String name,
                final int index,
                final IRightClickableElement target,
                final dev.sablexaerolink.network.StructureEntry entry,
                final boolean share) {
            super(name, Style.EMPTY, index, target);
            this.entry = entry;
            this.share = share;
        }

        @Override
        public void onAction(final Screen screen) {
            if (share) {
                XaeroWaypointActions.share(screen, entry);
            } else {
                XaeroWaypointActions.add(entry);
            }
        }
    }

    private static final class ActionOption extends RightClickOption {

        private final UUID id;
        private final StructureActionPayload.Action action;

        private ActionOption(
                final String name,
                final int index,
                final IRightClickableElement target,
                final UUID id,
                final StructureActionPayload.Action action,
                final boolean active) {
            super(name, Style.EMPTY, index, target);
            this.id = id;
            this.action = action;
            setActive(active);
        }

        @Override
        public void onAction(final Screen screen) {
            PacketDistributor.sendToServer(new StructureActionPayload(id, action));
        }
    }
}
