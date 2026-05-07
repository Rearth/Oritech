package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.util.registry.OritechDataComponentRegistry;

public class ComponentContent {
    
    public static final OritechDataComponentRegistry COMPONENTS = new OritechDataComponentRegistry();
    
    public static final RegistrySupplier<DataComponentType<Boolean>> IS_AOE_ACTIVE =
      COMPONENTS.registerPersistentSynchronized("is_aoe_active", PrimitiveCodec.BOOL, ByteBufCodecs.BOOL);

    public static final RegistrySupplier<DataComponentType<Long>> ENERGY =
      COMPONENTS.registerPersistentSynchronized("energy", PrimitiveCodec.LONG, ByteBufCodecs.VAR_LONG);
    
    public static final RegistrySupplier<DataComponentType<BlockPos>> TARGET_POSITION =
      COMPONENTS.register("target_position", () -> DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC).build());
    
    public static final RegistrySupplier<DataComponentType<FluidStack>> STORED_FLUID =
      COMPONENTS.register("stored_fluid", () -> DataComponentType.<FluidStack>builder().persistent(NetworkManager.FLUID_STACK_CODEC).networkSynchronized(NetworkManager.FLUID_STACK_STREAM_CODEC).build());
    
    public static final RegistrySupplier<DataComponentType<ShrinkerBlockEntity.ShrunkAddonData>> ADDON_DATA =
      COMPONENTS.register("addon_data", () -> DataComponentType.<ShrinkerBlockEntity.ShrunkAddonData>builder()
                                                .persistent(ShrinkerBlockEntity.ShrunkAddonData.CODEC)
                                                .networkSynchronized(ShrinkerBlockEntity.ShrunkAddonData.STREAM_CODEC)
                                                .build());

    public static void register() {
        COMPONENTS.register();
    }

}
