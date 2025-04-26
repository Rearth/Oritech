package rearth.oritech.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.EquipmentSlot;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;
import rearth.oritech.item.tools.util.ArmorEventHandler;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        var energyApiInstance = new FabricEnergyApiImpl();
        EnergyApi.BLOCK = energyApiInstance;
        EnergyApi.ITEM = energyApiInstance;
        
        var fluidApiInstance = new FabricFluidApiImpl();
        FluidApi.BLOCK = fluidApiInstance;
        FluidApi.ITEM = fluidApiInstance;
        
        ItemApi.BLOCK = new FabricItemApi();
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
        
        registerFabricEvents();
        
    }
    
    public static void registerFabricEvents() {
        ServerEntityEvents.EQUIPMENT_CHANGE.register(ArmorEventHandler::processEvent);
        EntityElytraEvents.CUSTOM.register(((entity, tickElytra) -> {
            var chestStack = entity.getEquippedStack(EquipmentSlot.CHEST);
            if (chestStack.getItem() instanceof JetpackElytraItem jetpackElytraItem) {
                return jetpackElytraItem.useCustomElytra(entity, chestStack, tickElytra);
            } else if (chestStack.getItem() instanceof JetpackExoElytraItem jetpackElytraItem) {
                return jetpackElytraItem.useCustomElytra(entity, chestStack, tickElytra);
            }
            
            return false;
        }));
    }
}
