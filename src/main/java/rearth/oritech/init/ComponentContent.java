package rearth.oritech.init;

import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

import java.util.function.Supplier;

public class ComponentContent {
    
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Oritech.MOD_ID);
    
    
    public static final Supplier<DataComponentType<Boolean>> IS_AOE_ACTIVE = COMPONENTS.registerComponentType(
      "is_aoe_active", builder -> builder.persistent(PrimitiveCodec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );
    
    public static final Supplier<DataComponentType<Long>> ENERGY = COMPONENTS.registerComponentType(
      "energy", builder -> builder.persistent(PrimitiveCodec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );
    
    public static final Supplier<DataComponentType<BlockPos>> TARGET_POSITION = COMPONENTS.registerComponentType(
      "target_position", builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC)
    );
    
    public static final Supplier<DataComponentType<FluidStack>> STORED_FLUID = COMPONENTS.registerComponentType(
      "stored_fluid", builder -> builder.persistent(FluidStack.OPTIONAL_CODEC).networkSynchronized(FluidStack.OPTIONAL_STREAM_CODEC)
    );
    
    public static final Supplier<DataComponentType<ShrinkerBlockEntity.ShrunkAddonData>> ADDON_DATA = COMPONENTS.registerComponentType(
      "addon_data", builder -> builder.persistent(ShrinkerBlockEntity.ShrunkAddonData.CODEC).networkSynchronized(ShrinkerBlockEntity.ShrunkAddonData.STREAM_CODEC)
    );

}
