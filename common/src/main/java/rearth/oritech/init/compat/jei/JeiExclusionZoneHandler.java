package rearth.oritech.init.compat.jei;

import io.wispforest.owo.mixin.ui.access.BaseOwoHandledScreenAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Surface;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class JeiExclusionZoneHandler implements IGuiContainerHandler<BaseOwoHandledScreen<FlowLayout, ?>> {
    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull BaseOwoHandledScreen<FlowLayout, ?> containerScreen) {
        var result = new ArrayList<Rect2i>();

        // basically a copy of the owo emi adapter
        if (!containerScreen.children().isEmpty() && containerScreen instanceof BaseOwoHandledScreenAccessor accessor) {
            OwoUIAdapter<?> adapter = accessor.owo$getUIAdapter();
            if (adapter != null) {
                ParentComponent rootComponent = adapter.rootComponent;
                ArrayList<Component> children = new ArrayList<>();
                rootComponent.collectDescendants(children);
                children.remove(rootComponent);
                children.forEach((component) -> {
                    if (component instanceof ParentComponent parent) {
                        if (parent.surface() == Surface.BLANK) {
                            return;
                        }
                    }

                    Size size = component.fullSize();
                    
                    if (size.height() > 0 && size.width() > 0)
                        result.add(new Rect2i(component.x(), component.y(), size.width(), size.height()));
                });
            }
        }

        return result;
    }
}
