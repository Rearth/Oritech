package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("SwitchStatementWithTooFewBranches")
public class WallMountedLight extends FaceAttachedHorizontalDirectionalBlock {
    
    protected VoxelShape NORTH_WALL_SHAPE;
    protected VoxelShape SOUTH_WALL_SHAPE;
    protected VoxelShape WEST_WALL_SHAPE;
    protected VoxelShape EAST_WALL_SHAPE;
    protected VoxelShape FLOOR_Z_AXIS_SHAPE;
    protected VoxelShape FLOOR_X_AXIS_SHAPE;
    protected VoxelShape CEILING_Z_AXIS_SHAPE;
    protected VoxelShape CEILING_X_AXIS_SHAPE;
    
    public WallMountedLight(Properties settings, int height) {
        super(settings);
        this.registerDefaultState(defaultBlockState()
                               .setValue(FACING, Direction.NORTH)
                               .setValue(FACE, AttachFace.FLOOR)
        );
        
        SOUTH_WALL_SHAPE = Block.box(5.0, 0, 0.0, 11.0, 16.0, height);
        NORTH_WALL_SHAPE = Block.box(5.0, 0, 16 - height, 11.0, 16.0, 16.0);
        WEST_WALL_SHAPE = Block.box(16 - height, 0, 5.0, 16.0, 16.0, 11.0);
        EAST_WALL_SHAPE = Block.box(0.0, 0, 5.0, height, 16.0, 11.0);
        FLOOR_Z_AXIS_SHAPE = Block.box(5.0, 0.0, 0, 11.0, height, 16.0);
        FLOOR_X_AXIS_SHAPE = Block.box(0.0, 0.0, 5.0, 16.0, height, 11.0);
        CEILING_Z_AXIS_SHAPE = Block.box(5.0, 16 - height, 0, 11.0, 16.0, 16.0);
        CEILING_X_AXIS_SHAPE = Block.box(0.0, 16 - height, 5.0, 16.0, 16.0, 11.0);
    }
    
    // copied from LeverBlock
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        switch (state.getValue(FACE)) {
            case FLOOR: {
                return switch (state.getValue(FACING).getAxis()) {
                    case X -> FLOOR_X_AXIS_SHAPE;
                    default -> FLOOR_Z_AXIS_SHAPE;
                };
            }
            case WALL: {
                return switch (state.getValue(FACING)) {
                    case EAST -> EAST_WALL_SHAPE;
                    case WEST -> WEST_WALL_SHAPE;
                    case SOUTH -> SOUTH_WALL_SHAPE;
                    default -> NORTH_WALL_SHAPE;
                };
            }
        }
        return switch (state.getValue(FACING).getAxis()) {
            case X -> CEILING_X_AXIS_SHAPE;
            default -> CEILING_Z_AXIS_SHAPE;
        };
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FACE);
    }
    
    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return null;
    }
}
