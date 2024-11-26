package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;

import java.util.function.Supplier;

// this can't be an auto container because as always, neoforge is annoying
// because of stupid neoforge we have to register components separatelly there, because $REASONS
// when adding/changing this, make sure to update the components in the neo entry class aswell
@SuppressWarnings("unchecked")
public class ComponentContent {
    
    public static final DeferredRegister<ComponentType<?>> COMPONENTS = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.DATA_COMPONENT_TYPE);
    
    public static final RegistrySupplier<ComponentType<Boolean>> IS_AOE_ACTIVE_REG = COMPONENTS.register("is_aoe_active", () -> ComponentType.<Boolean>builder().codec(PrimitiveCodec.BOOL).packetCodec(PacketCodecs.BOOL).build());
    public static final RegistrySupplier<ComponentType<BlockPos>> TARGET_POSITION_REG = COMPONENTS.register("target_position", () -> ComponentType.<BlockPos>builder().codec(BlockPos.CODEC).packetCodec(BlockPos.PACKET_CODEC).build());
    public static final RegistrySupplier<ComponentType<FluidStack>> STORED_FLUID_REG = COMPONENTS.register("stored_fluid", () -> ComponentType.<FluidStack>builder().codec(FluidStack.CODEC).packetCodec(FluidStack.STREAM_CODEC).build());

    public static final Supplier<ComponentType<Boolean>> IS_AOE_ACTIVE = () -> (ComponentType<Boolean>) Registries.DATA_COMPONENT_TYPE.get(Oritech.id("is_aoe_active"));
    public static final Supplier<ComponentType<BlockPos>> TARGET_POSITION = () -> (ComponentType<BlockPos>) Registries.DATA_COMPONENT_TYPE.get(Oritech.id("target_position"));
    public static final Supplier<ComponentType<FluidStack>> STORED_FLUID = () -> (ComponentType<FluidStack>) Registries.DATA_COMPONENT_TYPE.get(Oritech.id("stored_fluid"));
    
    // because I can't seem to find a default component for this on neoforge
    public static final Supplier<ComponentType<Long>> NEO_ENERGY_COMPONENT = () -> (ComponentType<Long>) Registries.DATA_COMPONENT_TYPE.get(Oritech.id("energy"));
    
    public static void registerComponents() {
        COMPONENTS.register();
    }
    
}
