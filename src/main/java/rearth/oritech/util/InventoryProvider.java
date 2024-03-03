package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.util.math.Direction;

public interface InventoryProvider {
    
    InventoryStorage getInventory(Direction direction);
    
}
