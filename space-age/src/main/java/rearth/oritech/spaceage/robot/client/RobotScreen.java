package rearth.oritech.spaceage.robot.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.robot.RobotMenu;

import java.util.Locale;

/** The same action list is used by the offhand recorder and the machine controller. */
public final class RobotScreen extends OritechWidgetScreen<RobotMenu> {

    private ScrollWidget list;
    private int revision = -1;

    public RobotScreen(RobotMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 396, menu.arm == null ? 208 : 270);
    }

    private Component text(String key) {

        return Component.translatable("screen.oritech_space_age.robot." + key);
    }

    private void button(int x, int y, int width, String key, int action) {

        addComponent(SpaceAgeButtons.panel(x, y, width, 18, text(key), ignored ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action)));
    }

    @Override
    protected void buildComponents() {

        var scroll = list == null ? 0 : list.getScrollY();
        addComponent(new SurfaceWidget(0, 0, imageWidth, imageHeight, OritechSurface.PANEL));
        addComponent(new LabelWidget(12, 9, 370, Component.translatable("screen.oritech_space_age.robot.actions", menu.program.actions().size())).withDarkColor());
        list = new ScrollWidget(12, 27, 372, 106).withRenderCulling(2);
        for (int i = 0; i < menu.program.actions().size(); i++) {
            var action = menu.program.actions().get(i);
            var index = i;
            var label = Component.literal((i + 1) + ". ").append(text(action.kind().name().toLowerCase(Locale.ROOT)))
                    .append(" " + action.pos().toShortString());
            list.addChild(new LabelWidget(5, i * 23 + 6, menu.arm == null ? 310 : 235, label)
                    .withColor(menu.arm != null && menu.cursor == i ? 0xFF906020 : 0xFF202020)
                    .withTooltip(action.item().isEmpty() ? new Component[]{action.state().getBlock().getName()}
                            : new Component[]{action.state().getBlock().getName(), action.item().getHoverName()}));
            if (menu.arm == null) {
                list.addChild(SpaceAgeButtons.panel(326, i * 23 + 2, 24, 18, Component.literal("×"), ignored ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1000 + index)));
            } else if (menu.redstone == 3) {
                list.addChild(SpaceAgeButtons.panel(240, i * 23 + 2, 110, 18,
                        text(action.signal().name().toLowerCase(Locale.ROOT)), ignored ->
                                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2000 + index)));
            }
        }
        list.setContentDimensions(352, menu.program.actions().size() * 23);
        list.setScrollPosition(0, scroll);
        addComponent(list);
        if (menu.arm == null) {
            button(12, 144, 116, "clear", 6);
            addComponent(new LabelWidget(12, 174, 366, 28, text("record_help")).withDarkColor().withWrap(true));
            return;
        }
        button(12, 139, 64, menu.running ? "pause" : "start", 1);
        button(80, 139, 64, "restart", 2);
        button(148, 139, 68, menu.repeat ? "repeat" : "once", 3);
        button(220, 139, 72, menu.skip ? "skip" : "wait", 4);
        button(296, 139, 88, "redstone_" + menu.redstone, 5);
        addComponent(new LabelWidget(12, 161, 372, Component.translatable("screen.oritech_space_age.robot.status",
                text(menu.status), menu.energy)).withDarkColor());
        button(42, 180, 138, "load", 0);
        for (var slot : menu.slots) addComponent(new SurfaceWidget(slot.x - 1, slot.y - 1, 18, 18, OritechSurface.PANEL_DARK));
        addComponent(new LabelWidget(18, 245, 180, Component.translatable("screen.oritech_space_age.robot.addons",
                String.format(Locale.ROOT, "%.1f", menu.arm.getCoreQuality()), menu.arm.getConnectedAddons().size())).withDarkColor());
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (menu.revision != revision) {
            revision = menu.revision;
            rebuildComponents();
        }
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.ROBOT_ARM.get().defaultBlockState();
    }
}
