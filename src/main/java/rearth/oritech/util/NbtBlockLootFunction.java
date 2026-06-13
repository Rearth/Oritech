package rearth.oritech.util;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.block.entity.storage.SmallStorageBlockEntity;
import rearth.oritech.block.entity.storage.SmallTankEntity;
import rearth.oritech.init.ComponentContent;

import java.util.List;

public class NbtBlockLootFunction extends LootItemConditionalFunction {
    public static final String NAME = "nbt_block_loot_function";

    public NbtBlockLootFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public @NotNull ItemStack run(@NotNull ItemStack stack, LootContext context) {

        var blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof SmallTankEntity tankEntity && tankEntity.hasStoredFluidForDrops()) {
            stack.set(ComponentContent.STORED_FLUID.get(), SimpleFluidContent.copyOf(tankEntity.getStoredFluidForDrops()));
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else if (blockEntity instanceof SmallStorageBlockEntity storageEntity && storageEntity.energyStorage.energy > 0) {
            stack.set(ComponentContent.ENERGY.get(), (int) storageEntity.energyStorage.energy);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else if (blockEntity instanceof CombiAddonEntity combiAddon && combiAddon.storedData != null) {
            stack.set(ComponentContent.ADDON_DATA.get(), combiAddon.storedData);
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }

        return stack;
    }

    public static Builder<?> builder() {
        return LootItemConditionalFunction.simpleBuilder(NbtBlockLootFunction::new);
    }

    @Override
    public MapCodec<NbtBlockLootFunction> codec() {
        return CODEC;
    }

    public static final MapCodec<NbtBlockLootFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> LootItemConditionalFunction.commonFields(instance).apply(instance, NbtBlockLootFunction::new));
}
