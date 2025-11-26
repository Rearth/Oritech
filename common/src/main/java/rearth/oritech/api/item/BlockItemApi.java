package rearth.oritech.api.item;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockItemApi {
    
    void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier);
    
    ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction);
    
    ItemApi.InventoryStorage find(Level world, BlockPos pos, @Nullable Direction direction);
}
