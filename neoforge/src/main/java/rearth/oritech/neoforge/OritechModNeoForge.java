package rearth.oritech.neoforge;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import rearth.oritech.Oritech;

@Mod(Oritech.MOD_ID)
public final class OritechModNeoForge {
    public OritechModNeoForge(IEventBus eventBus) {
        // Run our common setup.
        
        eventBus.register(new EventHandler());
        EventHandler.COMPONENT_REGISTRAR.register(eventBus);
        
        Oritech.initialize();
        
    }
    
    static class EventHandler {
        
        // see ComponentContent.java for why this is incredibly stupid but required
        public static final DeferredRegister.DataComponents COMPONENT_REGISTRAR = DeferredRegister.createDataComponents(RegistryKeys.DATA_COMPONENT_TYPE, Oritech.MOD_ID);
        
        public static final DeferredHolder<ComponentType<?>, ComponentType<Boolean>> IS_AOE_ACTIVE = COMPONENT_REGISTRAR.registerComponentType(
          "is_aoe_active",
          builder -> builder.codec(PrimitiveCodec.BOOL).packetCodec(PacketCodecs.BOOL)
        );
        
        public static final DeferredHolder<ComponentType<?>, ComponentType<BlockPos>> TARGET_POSITION = COMPONENT_REGISTRAR.registerComponentType(
          "target_position",
          builder -> builder.codec(BlockPos.CODEC).packetCodec(BlockPos.PACKET_CODEC)
        );
        
        public static final DeferredHolder<ComponentType<?>, ComponentType<FluidStack>> STORED_FLUID = COMPONENT_REGISTRAR.registerComponentType(
          "stored_fluid",
          builder -> builder.codec(FluidStack.CODEC).packetCodec(FluidStack.STREAM_CODEC)
        );
        
        @SubscribeEvent
        public void register(RegisterEvent event) {
            
            var id = event.getRegistryKey().getValue();
            
            if (Oritech.EVENT_MAP.containsKey(id)) {
                Oritech.LOGGER.debug(event.getRegistryKey().toString());
                Oritech.EVENT_MAP.get(id).forEach(Runnable::run);
            }
            
        }
        
    }
}
