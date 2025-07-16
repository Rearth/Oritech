package rearth.oritech.init.compat.rei;

import io.wispforest.owo.ui.container.FlowLayout;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import rearth.oritech.client.ui.ItemFilterScreen;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ReiItemFilterDraggableStackVisitor implements DraggableStackVisitor<ItemFilterScreen> {

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<ItemFilterScreen> context, DraggableStack stack) {
        var cursor = context.getCurrentPosition();
        if (cursor == null || !(stack.getStack().getValue() instanceof ItemStack itemStack)) {
            return DraggedAcceptorResult.PASS;
        }

        var screen = context.getScreen();
        for (int i = 0; i < 12; i++) {
            var container = screen.getItemContainer(i);
            if (container.isInBoundingBox(cursor.x, cursor.y)) {
                return screen.acceptItemStack(itemStack.copyWithCount(1), i)
                        ? DraggedAcceptorResult.ACCEPTED
                        : DraggedAcceptorResult.PASS;
            }
        }
        return DraggedAcceptorResult.PASS;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<ItemFilterScreen> context, DraggableStack stack) {
        return IntStream.range(0, 12)
                .mapToObj(i -> context.getScreen().getItemContainer(i))
                .map(container -> BoundsProvider.ofRectangle(new Rectangle(container.x(), container.y(), container.width(), container.height())));
    }

    @Override
    public <R extends Screen> boolean isHandingScreen(R r) {
        return r instanceof ItemFilterScreen;
    }
}
