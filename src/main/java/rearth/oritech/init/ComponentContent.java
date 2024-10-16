package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.util.FluidStack;

public class ComponentContent {
    
    public static final ComponentType<Boolean> IS_AOE_ACTIVE = ComponentType.<Boolean>builder().codec(PrimitiveCodec.BOOL).packetCodec(PacketCodecs.BOOL).build();
    public static final ComponentType<BlockPos> TARGET_POSITION = ComponentType.<BlockPos>builder().codec(BlockPos.CODEC).packetCodec(BlockPos.PACKET_CODEC).build();
    public static final ComponentType<FluidStack> STORED_FLUID = ComponentType.<FluidStack>builder().codec(FluidStack.CODEC).packetCodec(FluidStack.PACKET_CODEC).build();
    
    public static void init() {
        Registry.register(Registries.DATA_COMPONENT_TYPE, Oritech.id("is_aoe_active"), IS_AOE_ACTIVE);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Oritech.id("target_position"), TARGET_POSITION);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Oritech.id("stored_fluid"), STORED_FLUID);
    }
    
}
