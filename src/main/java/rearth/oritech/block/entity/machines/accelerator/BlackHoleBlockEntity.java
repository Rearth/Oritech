package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.init.BlockEntitiesContent;

public class BlackHoleBlockEntity extends BlockEntity implements BlockEntityTicker<BlackHoleBlockEntity> {
    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BLACK_HOLE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, BlackHoleBlockEntity blockEntity) {
    
    }
}
