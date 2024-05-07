package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class WallMountedLight extends WallMountedBlock {
    
    protected VoxelShape NORTH_WALL_SHAPE;
    protected VoxelShape SOUTH_WALL_SHAPE;
    protected VoxelShape WEST_WALL_SHAPE;
    protected VoxelShape EAST_WALL_SHAPE;
    protected VoxelShape FLOOR_Z_AXIS_SHAPE;
    protected VoxelShape FLOOR_X_AXIS_SHAPE;
    protected VoxelShape CEILING_Z_AXIS_SHAPE;
    protected VoxelShape CEILING_X_AXIS_SHAPE;
    
    public WallMountedLight(Settings settings, int height) {
        super(settings);
        this.setDefaultState(getDefaultState()
                               .with(FACING, Direction.NORTH)
                               .with(FACE, BlockFace.FLOOR)
        );
        
        SOUTH_WALL_SHAPE = Block.createCuboidShape(5.0, 0, 0.0, 11.0, 16.0, height);
        NORTH_WALL_SHAPE = Block.createCuboidShape(5.0, 0, 16 - height, 11.0, 16.0, 16.0);
        WEST_WALL_SHAPE = Block.createCuboidShape(16 - height, 0, 5.0, 16.0, 16.0, 11.0);
        EAST_WALL_SHAPE = Block.createCuboidShape(0.0, 0, 5.0, height, 16.0, 11.0);
        FLOOR_Z_AXIS_SHAPE = Block.createCuboidShape(5.0, 0.0, 0, 11.0, height, 16.0);
        FLOOR_X_AXIS_SHAPE = Block.createCuboidShape(0.0, 0.0, 5.0, 16.0, height, 11.0);
        CEILING_Z_AXIS_SHAPE = Block.createCuboidShape(5.0, 16 - height, 0, 11.0, 16.0, 16.0);
        CEILING_X_AXIS_SHAPE = Block.createCuboidShape(0.0, 16 - height, 5.0, 16.0, 16.0, 11.0);
    }
    
    // copied from LeverBlock
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACE)) {
            case FLOOR: {
                return switch (state.get(FACING).getAxis()) {
                    case X -> FLOOR_X_AXIS_SHAPE;
                    default -> FLOOR_Z_AXIS_SHAPE;
                };
            }
            case WALL: {
                return switch (state.get(FACING)) {
                    case EAST -> EAST_WALL_SHAPE;
                    case WEST -> WEST_WALL_SHAPE;
                    case SOUTH -> SOUTH_WALL_SHAPE;
                    default -> NORTH_WALL_SHAPE;
                };
            }
        }
        return switch (state.get(FACING).getAxis()) {
            case X -> CEILING_X_AXIS_SHAPE;
            default -> CEILING_Z_AXIS_SHAPE;
        };
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FACE);
    }
    
    @Override
    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return null;
    }
}
