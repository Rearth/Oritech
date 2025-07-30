package rearth.oritech.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.block.entity.storage.SmallStorageBlockEntity;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.LootContent;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NbtBlockLootFunction extends LootItemConditionalFunction {
    public static final String NAME = "nbt_block_loot";
    
    public NbtBlockLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }
    
    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        var blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        
        if (blockEntity instanceof SmallTankEntity tankEntity && tankEntity.fluidStorage.getAmount() > 0) {
            stack.set(FluidApi.ITEM.getFluidComponent(), tankEntity.fluidStorage.getStack());
        } else if (blockEntity instanceof SmallStorageBlockEntity storageEntity && storageEntity.energyStorage.amount > 0) {
            stack.set(EnergyApi.ITEM.getEnergyComponent(), storageEntity.energyStorage.amount);
        }
        
        return stack;
    }
    
    @Override
    public LootItemFunctionType<NbtBlockLootFunction> getType() {
        return LootContent.NBT_BLOCK_LOOT_FUNCTION;
    }
    
    public static net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction.Builder<?> builder() {
        return LootItemConditionalFunction.simpleBuilder(NbtBlockLootFunction::new);
    }
    
    public static final MapCodec<NbtBlockLootFunction> CODEC = RecordCodecBuilder.mapCodec(
      instance -> LootItemConditionalFunction.commonFields(instance).apply(instance, NbtBlockLootFunction::new));
}
