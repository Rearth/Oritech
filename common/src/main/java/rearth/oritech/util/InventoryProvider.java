package rearth.oritech.util;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.util.math.Direction;

public interface InventoryProvider {
    
    Storage<ItemVariant> getInventory(Direction direction);
    
}
