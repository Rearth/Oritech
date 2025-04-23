package rearth.oritech.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.block.entity.storage.SmallStorageBlockEntity;
import rearth.oritech.init.LootContent;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;

import java.util.List;

public class NbtBlockLootFunction extends ConditionalLootFunction {
    public static final String NAME = "nbt_block_loot";
    
    public NbtBlockLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }
    
    @Override
    public ItemStack process(ItemStack stack, LootContext context) {
        var blockEntity = context.get(LootContextParameters.BLOCK_ENTITY);
        
        if (blockEntity instanceof SmallTankEntity tankEntity && tankEntity.fluidStorage.getAmount() > 0) {
            stack.set(FluidApi.ITEM.getFluidComponent(), tankEntity.fluidStorage.getStack());
        } else if (blockEntity instanceof SmallStorageBlockEntity storageEntity && storageEntity.energyStorage.amount > 0) {
            stack.set(EnergyApi.ITEM.getEnergyComponent(), storageEntity.energyStorage.amount);
        }
        
        return stack;
    }
    
    @Override
    public LootFunctionType<NbtBlockLootFunction> getType() {
        return LootContent.NBT_BLOCK_LOOT_FUNCTION;
    }
    
    public static Builder<?> builder() {
        return ConditionalLootFunction.builder(NbtBlockLootFunction::new);
    }
    
    public static final MapCodec<NbtBlockLootFunction> CODEC = RecordCodecBuilder.mapCodec(
      instance -> ConditionalLootFunction.addConditionsField(instance).apply(instance, NbtBlockLootFunction::new));
}
