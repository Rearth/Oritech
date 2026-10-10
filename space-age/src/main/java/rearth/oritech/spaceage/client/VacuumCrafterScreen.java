package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.block.VacuumCrafterMenu;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.RocketNetworking;
import rearth.oritech.spaceage.simulation.VacuumProcessing;

/**
 * Shared naming panel for the slotless crafter and cargo controllers.
 */
public final class VacuumCrafterScreen extends OritechWidgetScreen<VacuumCrafterMenu> {

    private EditBox nameField;

    public VacuumCrafterScreen(VacuumCrafterMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 320, 180);
    }

    @Override
    protected void buildComponents() {

        var name = nameField == null ? menu.name : nameField.getValue();
        addComponent(new SurfaceWidget(0, 0, imageWidth, imageHeight, OritechSurface.PANEL));
        addComponent(new LabelWidget(12, 14, 66, Component.translatable("screen.oritech_space_age.pair.name")).withDarkColor());
        nameField = addRenderableWidget(new EditBox(font, leftPos + 80, topPos + 9, 228, 20,
                Component.translatable("screen.oritech_space_age.pair.name")));
        nameField.setMaxLength(32);
        nameField.setValue(name);
        setInitialFocus(nameField);

        addComponent(new SurfaceWidget(12, 40, 296, 104, OritechSurface.PANEL_DARK));
        if (minecraft.level != null) {
            var state = minecraft.level.getBlockState(menu.pos);
            if (state.getBlock() instanceof BlockPairController) {
                var cells = BlockPairController.cells(menu.pos, state);
                var first = minecraft.level.getBlockState(cells.getFirst());
                var second = minecraft.level.getBlockState(cells.getLast());
                addComponent(new BlockWidget(20, 47, 24, first));
                addComponent(new LabelWidget(52, 50, 244, 22, first.getBlock().getName()).withBrightColor().withWrap(true));
                addComponent(new BlockWidget(20, 76, 24, second));
                addComponent(new LabelWidget(52, 79, 244, 22, second.getBlock().getName()).withBrightColor().withWrap(true));
                var match = VacuumProcessing.match(first, second, SpaceAgeClientRecipes.vacuum());
                var result = match.valid() ? Component.translatable("screen.oritech_space_age.pair.result", match.recipe().resultState().getBlock().getName())
                        : Component.translatable("status.oritech_space_age." + match.issue());
                addComponent(new LabelWidget(20, 110, 280, 30, result).withColor(0xFFFFCD7B).withWrap(true).withTooltip(result));
            }
        }
        addComponent(SpaceAgeButtons.panel(12, 152, 100, 20, Component.translatable("gui.cancel"), ignored -> onClose()));
        addComponent(SpaceAgeButtons.orangePanel(208, 152, 100, 20, Component.translatable("gui.done"), ignored -> saveName()));
    }

    private void saveName() {

        ClientPacketDistributor.sendToServer(new RocketNetworking.NameControllerPayload(menu.pos, nameField.getValue()));
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (nameField != null && nameField.isMouseOver(event.x(), event.y())) {
            setFocused(nameField);
            return nameField.mouseClicked(event, doubleClick);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (event.isConfirmation()) {
            saveName();
            return true;
        }
        // Inventory hotkeys must not close the panel while entering a controller name.
        if (!event.isEscape() && nameField != null && nameField.isFocused()) {
            nameField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public BlockState getTitleState() {

        return minecraft.level == null ? SpaceAgeBlocks.VACUUM_CRAFTER.get().defaultBlockState() : minecraft.level.getBlockState(menu.pos);
    }
}
