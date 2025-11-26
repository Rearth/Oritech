package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import rearth.oritech.Oritech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

public class ComponentContent {
    
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Oritech.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    
    public static final RegistrySupplier<DataComponentType<Boolean>> IS_AOE_ACTIVE =
      COMPONENTS.register("is_aoe_active", () -> DataComponentType.<Boolean>builder().persistent(PrimitiveCodec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    
    public static final RegistrySupplier<DataComponentType<BlockPos>> TARGET_POSITION =
      COMPONENTS.register("target_position", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());
    
    public static final RegistrySupplier<DataComponentType<FluidStack>> STORED_FLUID =
      COMPONENTS.register("stored_fluid", () -> DataComponentType.<FluidStack>builder().persistent(FluidStack.CODEC).networkSynchronized(FluidStack.STREAM_CODEC).build());
    
    public static final RegistrySupplier<DataComponentType<ShrinkerBlockEntity.ShrunkAddonData>> ADDON_DATA =
      COMPONENTS.register("addon_data", () -> DataComponentType.<ShrinkerBlockEntity.ShrunkAddonData>builder()
                                                .persistent(ShrinkerBlockEntity.ShrunkAddonData.CODEC)
                                                .networkSynchronized(ShrinkerBlockEntity.ShrunkAddonData.STREAM_CODEC)
                                                .build());
    

}
