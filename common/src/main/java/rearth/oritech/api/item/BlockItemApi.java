package rearth.oritech.api.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface BlockItemApi {
    
    void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier);
    
    ItemApi.InventoryStorage find(World world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction);
    
    ItemApi.InventoryStorage find(World world, BlockPos pos, @Nullable Direction direction);
}
