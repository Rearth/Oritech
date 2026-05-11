package rearth.oritech.block.blocks.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TechLever extends LeverBlock {
    
    protected static final VoxelShape CEILING_X_SHAPE = Block.box(2.0, 10.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape CEILING_Z_SHAPE = Block.box(2.0, 10.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape FLOOR_X_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
    protected static final VoxelShape FLOOR_Z_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
    protected static final VoxelShape NORTH_SHAPE = Block.box(2.0, 2.0, 10.0, 14.0, 14.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 6.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(10.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 2.0, 2.0, 6.0, 14.0, 14.0);
    
    public TechLever(Properties settings) {
        super(settings);
    }
    
    // copied from lever block
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> switch (state.getValue(FACING).getAxis()) {
                case X -> FLOOR_X_SHAPE;
                default -> FLOOR_Z_SHAPE;
            };
            case WALL -> switch (state.getValue(FACING)) {
                case EAST -> EAST_SHAPE;
                case WEST -> WEST_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                default -> NORTH_SHAPE;
            };
            default -> switch (state.getValue(FACING).getAxis()) {
                case X -> CEILING_X_SHAPE;
                default -> CEILING_Z_SHAPE;
            };
        };
    }
}
