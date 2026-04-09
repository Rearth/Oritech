package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.client.ui.OritechWidgetScreen;

import java.util.ArrayList;
import java.util.List;

class JeiExclusionZoneHandler implements IGuiContainerHandler<OritechWidgetScreen<AbstractContainerMenu>> {
    @Override
    public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull OritechWidgetScreen<AbstractContainerMenu> containerScreen) {
        var result = new ArrayList<Rect2i>();
        
        if (!(containerScreen instanceof OritechWidgetScreen<?> oritechScreen)) return result;
        
        oritechScreen.getExclusionZones().forEach(elem -> result.add(new Rect2i(elem.getX(), elem.getY(), elem.getWidth(), elem.getHeight())));

        return result;
    }
}
