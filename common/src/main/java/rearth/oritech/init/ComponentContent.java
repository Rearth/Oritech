package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import rearth.oritech.Oritech;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

// this can't be an auto container because as always, neoforge is annoying
// because of stupid neoforge we have to register components separately there, because $REASONS
// when adding/changing this, make sure to update the components in the neo entry class aswell
@SuppressWarnings("unchecked")
public class ComponentContent {
    
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Oritech.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    
    public static final RegistrySupplier<DataComponentType<Boolean>> IS_AOE_ACTIVE_REG = COMPONENTS.register("is_aoe_active", () -> DataComponentType.<Boolean>builder().persistent(PrimitiveCodec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static final RegistrySupplier<DataComponentType<BlockPos>> TARGET_POSITION_REG = COMPONENTS.register("target_position", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());
    public static final RegistrySupplier<DataComponentType<FluidStack>> STORED_FLUID_REG = COMPONENTS.register("stored_fluid", () -> DataComponentType.<FluidStack>builder().persistent(FluidStack.CODEC).networkSynchronized(FluidStack.STREAM_CODEC).build());

    public static final Supplier<DataComponentType<Boolean>> IS_AOE_ACTIVE = () -> (DataComponentType<Boolean>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Oritech.id("is_aoe_active"));
    public static final Supplier<DataComponentType<BlockPos>> TARGET_POSITION = () -> (DataComponentType<BlockPos>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Oritech.id("target_position"));
    public static final Supplier<DataComponentType<FluidStack>> STORED_FLUID = () -> (DataComponentType<FluidStack>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Oritech.id("stored_fluid"));
    
    // because I can't seem to find a default component for this on neoforge
    public static final Supplier<DataComponentType<Long>> NEO_ENERGY_COMPONENT = () -> (DataComponentType<Long>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Oritech.id("energy"));
    
    public static void registerComponents() {
        COMPONENTS.register();
    }
    
}
