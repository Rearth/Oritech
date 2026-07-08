package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.screen.UIComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders multiple blocks in a compact preview.
 */
public class BlockPreviewWidget extends UIComponent {
    public record BlockEntry(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
    }

    private final List<BlockEntry> blocks = new ArrayList<>();

    public BlockPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public BlockPreviewWidget withRotationSpeed(float speed) {
        return this;
    }

    public void addBlock(BlockState state, @Nullable BlockEntity entity, Vec3i offset) {
        blocks.add(new BlockEntry(state, entity, offset));
    }

    public void clearBlocks() {
        blocks.clear();
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (blocks.isEmpty()) return;

        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();
        // Temporary 26.1 fallback: the old 3D block-in-GUI PiP renderer is currently stubbed
        // while the block render pipeline migration is unfinished. Rendering native item icons
        // keeps the preview panel useful without submitting a blank PiP texture.
        renderIconGrid(graphics, cx, cy, cw, ch);
    }

    private void renderIconGrid(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int gap = 4;
        int cellSize = 16 + gap;
        int columns = Math.max(1, width / cellSize);
        int rows = Math.max(1, (blocks.size() + columns - 1) / columns);
        int gridWidth = Math.min(blocks.size(), columns) * cellSize - gap;
        int gridHeight = rows * cellSize - gap;
        int startX = x + Math.max(0, (width - gridWidth) / 2);
        int startY = y + Math.max(0, (height - gridHeight) / 2);

        graphics.enableScissor(x, y, x + width, y + height);
        try {
            for (int index = 0; index < blocks.size(); index++) {
                var stack = new ItemStack(blocks.get(index).state().getBlock());
                if (stack.isEmpty()) continue;

                int column = index % columns;
                int row = index / columns;
                graphics.item(stack, startX + column * cellSize, startY + row * cellSize);
            }
        } finally {
            graphics.disableScissor();
        }
    }
}
