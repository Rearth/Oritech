package rearth.oritech.neoforge;

import com.mojang.serialization.Codec;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.item.tools.util.ArmorEventHandler;

@Mod(Oritech.MOD_ID)
public final class OritechModNeoForge {
    
    public OritechModNeoForge(IEventBus eventBus, ModContainer container) {
        
        eventBus.register(new EventHandler());
        EventHandler.COMPONENT_REGISTRAR.register(eventBus);
        
        OritechPlatformNeoForge.ATTACHMENT_TYPES.register(eventBus);
        
        // Register config specs
        container.registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.STARTUP, OritechStartupConfig.STARTUP_SPEC);
        
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

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPlayerClone(PlayerEvent.Clone event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerAugments.refreshActiveAugments(player);
            }
        }
    }
    
    class EventHandler {
        
        public static final DeferredRegister.DataComponents COMPONENT_REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Oritech.MOD_ID);
        
        public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> NEO_ENERGY_COMPONENT = COMPONENT_REGISTRAR.registerComponentType(
          "energy",
          builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
        );
        
        @SubscribeEvent
        public void registerCapabilities(RegisterCapabilitiesEvent event) {
            
            if (ItemApi.BLOCK instanceof NeoforgeItemApiImpl neoApi)
                neoApi.registerEvent(event);
            
            if (FluidApi.ITEM instanceof NeoforgeFluidApiImpl neoApi)
                neoApi.registerEvent(event);
            if (FluidApi.BLOCK instanceof NeoforgeFluidApiImpl neoApi)
                neoApi.registerEvent(event);
            
            if (EnergyApi.ITEM instanceof NeoforgeEnergyApiImpl neoApi)
                neoApi.registerEvent(event);
            if (EnergyApi.BLOCK instanceof NeoforgeEnergyApiImpl neoApi)
                neoApi.registerEvent(event);
        }
        
        @SubscribeEvent
        public void register(RegisterEvent event) {
            
            var id = event.getRegistryKey().location();
            
            if (Oritech.EVENT_MAP.containsKey(id)) {
                Oritech.LOGGER.debug(event.getRegistryKey().toString());
                Oritech.EVENT_MAP.get(id).forEach(Runnable::run);
            }
            
            if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.FLUID_TYPES)) {
                
                FluidContent.FLUID_ATTRIBUTES.forEach(attribute -> {
                    var type = attribute.getSourceFluid().getFluidType();
                    var fluidId = BuiltInRegistries.FLUID.getKey(attribute.getSourceFluid());
                    event.register(NeoForgeRegistries.Keys.FLUID_TYPES, registry -> registry.register(fluidId, type));
                });
                
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
