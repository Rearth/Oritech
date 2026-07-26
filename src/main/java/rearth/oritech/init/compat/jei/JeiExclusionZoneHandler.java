package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;
import rearth.oritech.client.ui.OritechWidgetScreen;

import java.util.List;

final class JeiExclusionZoneHandler implements IGuiContainerHandler<OritechWidgetScreen<?>> {

    @Override
    public List<Rect2i> getGuiExtraAreas(OritechWidgetScreen<?> screen) {
        return screen.getExclusionZones();
    }
}
