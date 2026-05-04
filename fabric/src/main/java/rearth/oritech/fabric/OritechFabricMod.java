package rearth.oritech.fabric;

import dev.architectury.fluid.FluidStack;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.fml.config.ModConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.item.tools.armor.JetpackElytraItem;
import rearth.oritech.item.tools.armor.JetpackExoElytraItem;
import rearth.oritech.item.tools.util.ArmorEventHandler;

public final class OritechFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        
        NetworkManager.FLUID_STACK_CODEC = FluidStack.CODEC;
        NetworkManager.FLUID_STACK_STREAM_CODEC = FluidStack.STREAM_CODEC;
        
        // Register config specs
        NeoForgeConfigRegistry.INSTANCE.register(Oritech.MOD_ID, ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(Oritech.MOD_ID, ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC);
        NeoForgeConfigRegistry.INSTANCE.register(Oritech.MOD_ID, ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);
        
        // Run our common setup.
        Oritech.runAllRegistries();
        Oritech.initialize();
        
        registerFabricEvents();
        
        for (var pair : Oritech.COMPOSTABLES_DATA) {
            ComposterBlock.add(pair.getB(), pair.getA());
        }
        
    }
    
    public static void registerFabricEvents() {
        ServerEntityEvents.EQUIPMENT_CHANGE.register(ArmorEventHandler::processEvent);
        
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // delayed via execute by a bit to prevent race conditions with augment loading (which fabric internally also does via this callback)
            newPlayer.server.execute(() -> PlayerAugments.refreshActiveAugments(newPlayer));

        });
        
        EntityElytraEvents.CUSTOM.register(((entity, tickElytra) -> {
            var chestStack = entity.getItemBySlot(EquipmentSlot.CHEST);
            if (chestStack.getItem() instanceof JetpackElytraItem jetpackElytraItem) {
                return jetpackElytraItem.useCustomElytra(entity, chestStack, tickElytra);
            } else if (chestStack.getItem() instanceof JetpackExoElytraItem jetpackElytraItem) {
                return jetpackElytraItem.useCustomElytra(entity, chestStack, tickElytra);
            }
            
            return false;
        }));
    }
}
