package rearth.oritech.api.screen.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.UIComponent;

/**
 * Renders the visual frame for an inventory slot (the 18x18 item slot background).
 * This is purely visual — the actual slot logic is in the ScreenHandler.
 * Place this at the same position as the corresponding menu slot.
 */
public class ItemSlotWidget extends UIComponent {

    public static final Identifier ITEM_SLOT_TEXTURE = Oritech.id("textures/gui/modular/itemslot.png");

    public ItemSlotWidget(int x, int y) {
        super(x - 1, y - 1, 18, 18); // slots are 16px content, frame is 18px with 1px border
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ITEM_SLOT_TEXTURE, x, y, 0, 0, 18, 18, 18, 18, 18, 18);
    }
}
