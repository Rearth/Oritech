package rearth.oritech.spaceage.client.robot_arm;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemSlotWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.UpgradableOritechScreen;
import rearth.oritech.spaceage.block.robot_arm.RobotMenu;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.util.ColorHelper;

import java.util.List;
import java.util.Set;

/**
 * One wide machine panel: energy on the left, the playbook above the player inventory,
 * and the controls above the arm's storage on the right.
 */
public final class RobotScreen extends UpgradableOritechScreen<RobotMenu> {

    private static final int WIDTH = 350;
    private static final int HEIGHT = 219;
    private static final int LIST_X = 32;
    private static final int LIST_WIDTH = 194;
    private static final int CONTROLS_X = 234;
    private static final int CONTROLS_WIDTH = 108;
    private static final int GROUP_SEPARATOR = ColorHelper.argb(0.6f, 0.6f, 0.62f);
    private static final int ISSUE_COLOR = ColorHelper.argb(0.7f, 0.18f, 0.15f);
    private static final Set<String> NORMAL_STATUS = Set.of("idle", "ready", "working", "finished");

    private RobotActionList list;
    private RobotProgram listedProgram;
    private LabelWidget statusLabel;
    private LabelWidget stepLabel;
    private ButtonWidget runButton;
    private ButtonWidget repeatButton;
    private ButtonWidget skipButton;
    private ButtonWidget redstoneButton;

    public RobotScreen(RobotMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, WIDTH, HEIGHT);
        // The standard background only fits 176 wide panels, so the panel and inventory slots are drawn as widgets.
        backgroundTexture = null;
    }

    private static Component text(String key) {

        return Component.translatable("screen.oritech_space_age.robot." + key);
    }

    private void click(int button) {

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void rebuildComponents() {

        var scroll = list == null ? 0 : list.scroll.getScrollY();
        list = null;
        super.rebuildComponents();
        list.scrollTo(scroll);
    }

    @Override
    protected void addExtraComponents() {

        super.addExtraComponents();
        addComponent(new SurfaceWidget(0, 0, imageWidth, imageHeight, OritechSurface.PANEL).withZIndex(-5));
        for (var slot : menu.slots.subList(menu.inventory.size(), menu.slots.size()))
            addComponent(new ItemSlotWidget(slot.x, slot.y));

        statusLabel = new LabelWidget(LIST_X, 8, LIST_WIDTH - 54, Component.empty());
        addComponent(statusLabel);
        stepLabel = new LabelWidget(LIST_X + LIST_WIDTH - 50, 8, 50, Component.empty()).withDarkColor().withAlignment(LabelWidget.Alignment.RIGHT);
        addComponent(stepLabel);
        rebuildList();

        runButton = SpaceAgeButtons.panel(CONTROLS_X, 8, 52, 18, Component.empty(), ignored -> click(RobotMenu.RUN_BUTTON));
        addComponent(runButton);
        addComponent(SpaceAgeButtons.panel(CONTROLS_X + 56, 8, 52, 18, text("restart"), ignored -> click(RobotMenu.RESTART_BUTTON)));
        var load = SpaceAgeButtons.panel(CONTROLS_X + 22, 30, CONTROLS_WIDTH - 22, 18, text("load"), ignored -> click(RobotMenu.LOAD_BUTTON));
        load.withTooltip(text("load.tooltip"));
        addComponent(load);
        addComponent(BoxWidget.filled(CONTROLS_X, 55, CONTROLS_WIDTH, 1, GROUP_SEPARATOR));
        repeatButton = setting(62, RobotMenu.REPEAT_BUTTON, "repeat.tooltip");
        skipButton = setting(86, RobotMenu.SKIP_BUTTON, "blocked.tooltip");
        redstoneButton = setting(110, RobotMenu.REDSTONE_BUTTON, null);
        updateControls();
    }

    private ButtonWidget setting(int y, int action, String tooltip) {

        var button = SpaceAgeButtons.panel(CONTROLS_X, y, CONTROLS_WIDTH, 18, Component.empty(), ignored -> click(action));
        if (tooltip != null) button.withTooltip(text(tooltip));
        addComponent(button);
        return button;
    }

    private void rebuildList() {

        float scroll = 0;
        if (list != null) {
            scroll = list.scroll.getScrollY();
            removeComponent(list.scroll);
        }
        listedProgram = menu.program;
        list = new RobotActionList(LIST_X, 20, LIST_WIDTH, 108, menu.program, text("empty"),
                (index, match) -> click(RobotMenu.matchButton(index, match)), null);
        list.scrollTo(scroll);
        addComponent(list.scroll);
    }

    private void updateControls() {

        statusLabel.setText(text(menu.status));
        statusLabel.withColor(NORMAL_STATUS.contains(menu.status) ? LabelWidget.DARK_TEXT : ISSUE_COLOR);
        var actions = menu.program.actions().size();
        stepLabel.setText(actions == 0 ? Component.empty()
                : Component.translatable("screen.oritech_space_age.robot.step", Math.min(menu.cursor, actions - 1) + 1, actions));
        list.setCursor(menu.running || menu.cursor > 0 ? menu.cursor : -1);
        runButton.setLabel(text(menu.running ? "pause" : "start"));
        repeatButton.setLabel(text(menu.repeat ? "repeat" : "once"));
        skipButton.setLabel(text(menu.skip ? "skip" : "wait"));
        redstoneButton.setLabel(text("redstone_" + menu.redstone));
        redstoneButton.setTooltip(List.of(text("redstone_" + menu.redstone + ".tooltip")));
    }

    @Override
    protected void tickExtra() {

        super.tickExtra();
        if (menu.program != listedProgram) rebuildList();
        updateControls();
    }
}
