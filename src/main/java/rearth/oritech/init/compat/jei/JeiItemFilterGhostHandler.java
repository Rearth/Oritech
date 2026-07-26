package rearth.oritech.init.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.client.ui.ItemFilterScreen;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.client.ui.ItemFilterScreen.FILTER_SIZE;

final class JeiItemFilterGhostHandler implements IGhostIngredientHandler<ItemFilterScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(ItemFilterScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return List.of();
        }

        var targets = new ArrayList<Target<I>>(FILTER_SIZE);
        for (int i = 0; i < FILTER_SIZE; i++) {
            targets.add(new ItemFilterTarget<>(screen, i));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private record ItemFilterTarget<I>(ItemFilterScreen screen, int index, Rect2i area) implements Target<I> {

        private ItemFilterTarget(ItemFilterScreen screen, int index) {
            this(screen, index, areaFor(screen, index));
        }

        private static Rect2i areaFor(ItemFilterScreen screen, int index) {
            var bounds = screen.getItemContainer(index);
            return new Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            screen.acceptItemStack(((ItemStack) ingredient).copyWithCount(1), index);
        }
    }
}
