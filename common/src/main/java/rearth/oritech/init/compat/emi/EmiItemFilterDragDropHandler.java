package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import io.wispforest.owo.ui.container.FlowLayout;
import rearth.oritech.client.ui.ItemFilterScreen;

public class EmiItemFilterDragDropHandler implements EmiDragDropHandler<ItemFilterScreen> {
    @Override
    public boolean dropStack(ItemFilterScreen screen, EmiIngredient stack, int x, int y) {
        if (stack.isEmpty()) {
            return false;
        }

        for (int i = 0; i < 12; i++) {
            FlowLayout container = screen.getItemContainer(i);
            if (container.isInBoundingBox(x, y)) {
                return screen.acceptItemStack(stack.getEmiStacks().getFirst().getItemStack().copyWithCount(1), i);
            }
        }
        return false;
    }
}
