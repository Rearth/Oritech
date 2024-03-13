package rearth.oritech.block.entity.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.init.BlockEntitiesContent;

public class FluidPipeInterfaceEntity extends GenericPipeInterfaceEntity {
    
    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
    
    }
}
