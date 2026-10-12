package rearth.oritech.spaceage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.ScrollWidget;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollWidgetCullingTest {

    @Test
    void cullsChildrenWhosePaddedBoundsMissTheViewport() {
        var scroll = new TestScrollWidget();

        assertTrue(scroll.inViewport(new TestComponent(8, 8, 10, 10), 0, 0, 20, 20));
        assertFalse(scroll.inViewport(new TestComponent(25, 8, 4, 4), 0, 0, 20, 20));
        assertFalse(scroll.inViewport(new TestComponent(-8, 8, 3, 3), 0, 0, 20, 20));
    }

    @Test
    void cullingAllowanceKeepsNearEdgeEffectsAndPaddedSurfaces() {
        var scroll = new TestScrollWidget();
        scroll.withRenderCulling(4);

        assertTrue(scroll.inViewport(new TestComponent(-3, 8, 1, 1), 0, 0, 20, 20));

        var padded = new TestComponent(-2, 8, 4, 4);
        padded.setPadding(Insets.of(0, 0, 0, 3));
        assertTrue(scroll.inViewport(padded, 0, 0, 20, 20));
    }

    private static class TestScrollWidget extends ScrollWidget {
        private TestScrollWidget() {
            super(0, 0, 20, 20);
        }

        private boolean inViewport(UIComponent child, float x, float y, int width, int height) {
            return isChildInViewport(child, x, y, width, height);
        }
    }

    private static class TestComponent extends UIComponent {
        private TestComponent(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        }
    }
}
