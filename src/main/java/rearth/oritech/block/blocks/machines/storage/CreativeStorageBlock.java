package rearth.oritech.block.blocks.machines.storage;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.storage.CreativeStorageBlockEntity;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class CreativeStorageBlock extends SmallStorageBlock  {

    public CreativeStorageBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeStorageBlockEntity(pos, state);
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ((CreativeStorageBlockEntity) world.getBlockEntity(pos)).getComparatorOutput();
    }

    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var droppedStacks = super.getDroppedStacks(state, builder);

        var blockEntity = (BlockEntity)builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (blockEntity instanceof CreativeStorageBlockEntity storageEntity)
            droppedStacks.addAll(storageEntity.inventory.getHeldStacks());

        return droppedStacks;
    }

    @NotNull
    private static ItemStack getStackWithData(WorldView world, BlockPos pos) {
        var stack = new ItemStack(BlockContent.CREATIVE_STORAGE_BLOCK.asItem());
        var storageEntity = (CreativeStorageBlockEntity) world.getBlockEntity(pos);

        if (storageEntity.getStorage(null).getAmount() > 0) {
            var nbt = new NbtCompound();
            storageEntity.writeNbt(nbt, world.getRegistryManager());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }

        return stack;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return getStackWithData(world, pos);
    }

}
