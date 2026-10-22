package rearth.oritech.spaceage.client.robot_arm;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.robot_arm.ActionCardMenu;
import rearth.oritech.spaceage.block.robot_arm.RobotMenu;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

/** Edits the playbook of a held action card, using the same rows as the arm. */
public final class ActionCardScreen extends OritechWidgetScreen<ActionCardMenu> {

    private RobotActionList list;
    private int revision;

    public ActionCardScreen(ActionCardMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 236, 210);
    }

    private static Component text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.robot." + key, args);
    }

    private void click(int button) {

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    @Override
    protected void buildComponents() {

        var scroll = list == null ? 0 : list.scroll.getScrollY();
        revision = menu.revision;
        var program = menu.program;
        addComponent(new SurfaceWidget(0, 0, imageWidth, imageHeight, OritechSurface.PANEL));
        addComponent(new LabelWidget(10, 9, 216, text("actions", program.actions().size())).withDarkColor());
        addComponent(new LabelWidget(10, 21, 216, program.binding()
                .map(binding -> text("bound", binding.pos().toShortString()))
                .orElse(text("unbound"))).withColor(LabelWidget.DARK_TEXT));
        list = new RobotActionList(10, 34, 216, 108, program, text("card_empty"),
                (index, match) -> click(RobotMenu.matchButton(index, match)),
                index -> click(ActionCardMenu.DELETE_BUTTON + index));
        list.scrollTo(scroll);
        addComponent(list.scroll);
        addComponent(SpaceAgeButtons.panel(10, 148, 116, 18, text("clear"), ignored -> click(ActionCardMenu.CLEAR_BUTTON)));
        addComponent(new LabelWidget(10, 174, 216, 27, text("record_help")).withDarkColor().withWrap(true));
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (menu.revision != revision) rebuildComponents();
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.ROBOT_ARM.get().defaultBlockState();
    }

    @Override
    public Component getTitleText() {

        return menu.getCard().getHoverName();
    }

    @Override
    public ItemStack getTitleIcon() {

        return menu.getCard().copyWithCount(1);
    }
}
