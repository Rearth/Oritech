package rearth.oritech.api.transfer.item;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {

    ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction);

}
