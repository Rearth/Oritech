package rearth.oritech.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.addons.ShrunkAddonData;

import java.util.function.Supplier;

public class ComponentContent {

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Oritech.MOD_ID);


    public static final Supplier<DataComponentType<Boolean>> IS_AOE_ACTIVE = COMPONENTS.registerComponentType(
            "is_aoe_active", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final Supplier<DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType(
            "energy", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
    );

    public static final Supplier<DataComponentType<GlobalPos>> TARGET_GLOBAL_POSITION = COMPONENTS.registerComponentType(
            "target_global_position", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<SimpleFluidContent>> STORED_FLUID = COMPONENTS.registerComponentType(
            "stored_fluid", builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<ShrunkAddonData>> ADDON_DATA = COMPONENTS.registerComponentType(
            "addon_data", builder -> builder.persistent(ShrunkAddonData.CODEC).networkSynchronized(ShrunkAddonData.STREAM_CODEC)
    );
}
