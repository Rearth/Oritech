package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.DrawContext;
import rearth.oritech.client.ui.ItemFilterScreen;

import static rearth.oritech.client.ui.ItemFilterScreen.FILTER_SIZE;

public class EmiItemFilterDragDropHandler implements EmiDragDropHandler<ItemFilterScreen> {
    @Override
    public void render(ItemFilterScreen screen, EmiIngredient dragged, DrawContext draw, int mouseX, int mouseY, float delta) {
        for (int i = 0; i < FILTER_SIZE; i++) {
            var container = screen.getItemContainer(i);
            draw.fill(container.x(), container.y(), container.x() + container.width(), container.y() + container.height(), 0x8822BB33);
        }
    }

    @Override
    public boolean dropStack(ItemFilterScreen screen, EmiIngredient stack, int x, int y) {
        if (stack.isEmpty()) {
            return false;
        }

        for (int i = 0; i < FILTER_SIZE; i++) {
            var container = screen.getItemContainer(i);
            if (container.isInBoundingBox(x, y)) {
                return screen.acceptItemStack(stack.getEmiStacks().getFirst().getItemStack().copyWithCount(1), i);
            }
        }
        return false;
    }
}
