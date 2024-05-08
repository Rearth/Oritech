package rearth.oritech.block.blocks.decorative;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class TechRedstoneButton extends ButtonBlock {
    
    protected static final VoxelShape CEILING_X_SHAPE = Block.createCuboidShape(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape CEILING_Z_SHAPE = Block.createCuboidShape(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape FLOOR_X_SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape FLOOR_Z_SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(2.0, 2.0, 12.0, 14.0, 14.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(2.0, 2.0, 0.0, 14.0, 14.0, 4.0);
    protected static final VoxelShape WEST_SHAPE = Block.createCuboidShape(12.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    protected static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0.0, 2.0, 2.0, 4.0, 14.0, 14.0);
    
    public TechRedstoneButton(BlockSetType blockSetType, int pressTicks, Settings settings) {
        super(blockSetType, pressTicks, settings);
    }
    
    // copied from lever block
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACE)) {
            case FLOOR -> switch (state.get(FACING).getAxis()) {
                case X -> FLOOR_X_SHAPE;
                default -> FLOOR_Z_SHAPE;
            };
            case WALL -> switch (state.get(FACING)) {
                case EAST -> EAST_SHAPE;
                case WEST -> WEST_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                default -> NORTH_SHAPE;
            };
            default -> switch (state.get(FACING).getAxis()) {
                case X -> CEILING_X_SHAPE;
                default -> CEILING_Z_SHAPE;
            };
        };
    }
}
