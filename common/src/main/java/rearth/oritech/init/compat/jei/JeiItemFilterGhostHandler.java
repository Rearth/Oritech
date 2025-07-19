package rearth.oritech.init.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.client.ui.ItemFilterScreen;

import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.client.ui.ItemFilterScreen.FILTER_SIZE;

class JeiItemFilterGhostHandler implements IGhostIngredientHandler<ItemFilterScreen> {

    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(@NotNull ItemFilterScreen screen, @NotNull ITypedIngredient<I> ingredient, boolean doStart) {
        var targets = new ArrayList<Target<I>>();
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return targets;
        }

        for (int i = 0; i < FILTER_SIZE; i++) {
            targets.add(new ItemFilterTarget<>(screen, i));
        }
        return targets;
    }

    @Override
    public void onComplete() {}

    static final class ItemFilterTarget<I> implements Target<I> {
        private final ItemFilterScreen screen;
        private final int index;
        private final Rect2i area;

        ItemFilterTarget(ItemFilterScreen screen, int index) {
            this.screen = screen;
            this.index = index;

            var container = screen.getItemContainer(index);
            this.area = new Rect2i(container.x(), container.y(), container.width(), container.height());
        }

        @Override
        public @NotNull Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(@NotNull I itemStack) {
            screen.acceptItemStack(((ItemStack) itemStack).copyWithCount(1), index);
        }
    }
}
