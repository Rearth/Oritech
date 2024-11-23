package rearth.oritech.util;

import earth.terrarium.common_storage_lib.context.ItemContext;
import earth.terrarium.common_storage_lib.data.DataManager;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import rearth.oritech.Oritech;

// This class is just a workaround for this bug: https://github.com/terrarium-earth/Common-Storage-Lib/issues/39
public class SimpleItemEnergyStorage extends SimpleEnergyStorage {
    
    public ItemStack context;
    
    public SimpleItemEnergyStorage(long capacity, long maxInsert, long maxExtract) {
        super(capacity, maxInsert, maxExtract);
    }
    
    public SimpleItemEnergyStorage(ItemContext context, ComponentType<Long> componentType, long capacity, long maxInsert, long maxExtract, ItemStack stack) {
        super(context, componentType, capacity, maxInsert, maxExtract);
        this.context = stack;
    }
    
    public SimpleItemEnergyStorage(Object entityOrBlockEntity, DataManager<Long> dataManager, long capacity, long maxInsert, long maxExtract) {
        super(entityOrBlockEntity, dataManager, capacity, maxInsert, maxExtract);
    }
    
    @Override
    public void update() {
        // skip running onUpdate due to CSL bug
        // no idea if that has terrible consequences with other mods
        
        if (context != null) {
            context.set(Oritech.ENERGY_CONTENT.componentType(), this.getStoredAmount());
        }
    }
}
