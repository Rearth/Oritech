package rearth.oritech.init.datagen.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.nbt.NbtCompound;
import rearth.oritech.block.entity.machines.storage.SmallFluidTankEntity;
import rearth.oritech.block.entity.machines.storage.SmallStorageBlockEntity;
import rearth.oritech.init.LootContent;

import java.util.List;

public class NbtBlockLootFunction extends ConditionalLootFunction {
    public static final String NAME = "nbt_block_loot";

    public NbtBlockLootFunction(List<LootCondition> conditions) {
        super(conditions);
    }
    
    @Override
    public ItemStack process(ItemStack stack, LootContext context) {
        var blockEntity = context.get(LootContextParameters.BLOCK_ENTITY);

        var nbt = new NbtCompound();
        if (blockEntity instanceof SmallFluidTankEntity tankEntity) {
            tankEntity.writeNbt(nbt, context.getWorld().getRegistryManager());
        } else if (blockEntity instanceof SmallStorageBlockEntity storageEntity) {
            storageEntity.writeNbt(nbt, context.getWorld().getRegistryManager());
        }

        // Any items contained in the block should be added to the drops in the Block's getDroppedStacks() method
        // Removing it here so that the inventory items are not still stored in the dropped item's data
        if (nbt.contains("Items"))
            nbt.remove("Items");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return stack;
    }

    @Override
    public LootFunctionType<NbtBlockLootFunction> getType() {
        return LootContent.NBT_BLOCK_LOOT_FUNCTION;
    }

    public static ConditionalLootFunction.Builder<?> builder() {
        return ConditionalLootFunction.builder(NbtBlockLootFunction::new);
    }

    public static final MapCodec<NbtBlockLootFunction> CODEC = RecordCodecBuilder.mapCodec(
        instance -> ConditionalLootFunction.addConditionsField(instance).apply(instance, NbtBlockLootFunction::new));
}
