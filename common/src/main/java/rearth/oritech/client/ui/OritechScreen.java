package rearth.oritech.client.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Deprecated(forRemoval = false)
public class OritechScreen<T extends OritechScreenHandler> extends OritechMachineScreen<T> {

    public OritechScreen(T handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
}
