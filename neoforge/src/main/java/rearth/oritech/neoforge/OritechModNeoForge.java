package rearth.oritech.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.item.tools.util.ArmorEventHandler;

@Mod(Oritech.MOD_ID)
public final class OritechModNeoForge {
    
    private final NeoforgeEnergyApiImpl energyApiInstance;
    private final NeoforgeFluidApiImpl fluidApiInstance;
    private final NeoforgeItemApiImpl itemApiInstance;
    
    public OritechModNeoForge(IEventBus eventBus) {
        
        eventBus.register(new EventHandler());
        EventHandler.COMPONENT_REGISTRAR.register(eventBus);
        
        OritechPlatformNeoForge.ATTACHMENT_TYPES.register(eventBus);
        
        fluidApiInstance = new NeoforgeFluidApiImpl();
        FluidApi.BLOCK = fluidApiInstance;
        FluidApi.ITEM = fluidApiInstance;
        
        itemApiInstance = new NeoforgeItemApiImpl();
        ItemApi.BLOCK = itemApiInstance;
        
        energyApiInstance = new NeoforgeEnergyApiImpl();
        EnergyApi.BLOCK = energyApiInstance;
        EnergyApi.ITEM = energyApiInstance;
        
        NetworkManager.FLUID_STACK_CODEC = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC.xmap(FluidStackHooksForge::fromForge, FluidStackHooksForge::toForge);
        NetworkManager.FLUID_STACK_STREAM_CODEC = net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_STREAM_CODEC.map(FluidStackHooksForge::fromForge, FluidStackHooksForge::toForge);
        
        Oritech.initialize();
        
    }
    
    // No idea why this needs to be another class, but oh well.
    @EventBusSubscriber(modid = Oritech.MOD_ID)
    static class CustomEvents {
        
        @SubscribeEvent
        public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
            ArmorEventHandler.processEvent(event.getEntity(), event.getSlot(), event.getFrom(), event.getTo());
        }
    }
    
    class EventHandler {
        
        // see ComponentContent.java for why this is incredibly stupid but required
        public static final DeferredRegister.DataComponents COMPONENT_REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Oritech.MOD_ID);
        
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_AOE_ACTIVE = COMPONENT_REGISTRAR.registerComponentType(
          "is_aoe_active",
          builder -> builder.persistent(PrimitiveCodec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
        );
        
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> TARGET_POSITION = COMPONENT_REGISTRAR.registerComponentType(
          "target_position",
          builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC)
        );
        
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidStack>> STORED_FLUID = COMPONENT_REGISTRAR.registerComponentType(
          "stored_fluid",
          builder -> builder.persistent(FluidStack.CODEC).networkSynchronized(FluidStack.STREAM_CODEC)
        );
        
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NEO_ENERGY_COMPONENT = COMPONENT_REGISTRAR.registerComponentType(
          "energy",
          builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
        );
        
        @SubscribeEvent
        public void registerCapabilities(RegisterCapabilitiesEvent event) {
            itemApiInstance.registerEvent(event);
            fluidApiInstance.registerEvent(event);
            energyApiInstance.registerEvent(event);
        }
        
        @SubscribeEvent
        public void register(RegisterEvent event) {
            
            var id = event.getRegistryKey().location();
            
            if (Oritech.EVENT_MAP.containsKey(id)) {
                Oritech.LOGGER.debug(event.getRegistryKey().toString());
                Oritech.EVENT_MAP.get(id).forEach(Runnable::run);
            }
            
        }
        
        @SubscribeEvent
        public void register(final RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            
            for (var toInit : OritechPlatformNeoForge.PENDING_S2C_INITS) {
                toInit.accept(registrar);
            }
            OritechPlatformNeoForge.PENDING_S2C_INITS.clear();
            
            for (var toInit : OritechPlatformNeoForge.PENDING_C2S_INITS) {
                toInit.accept(registrar);
            }
            OritechPlatformNeoForge.PENDING_C2S_INITS.clear();
            
        }
        
    }
}
