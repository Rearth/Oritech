package rearth.oritech.block.blocks.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TechRedstoneButton extends ButtonBlock {
    
    protected static final VoxelShape CEILING_X_SHAPE = Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape CEILING_Z_SHAPE = Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0);
    protected static final VoxelShape FLOOR_X_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape FLOOR_Z_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    protected static final VoxelShape NORTH_SHAPE = Block.box(2.0, 2.0, 12.0, 14.0, 14.0, 16.0);
    protected static final VoxelShape SOUTH_SHAPE = Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 4.0);
    protected static final VoxelShape WEST_SHAPE = Block.box(12.0, 2.0, 2.0, 16.0, 14.0, 14.0);
    protected static final VoxelShape EAST_SHAPE = Block.box(0.0, 2.0, 2.0, 4.0, 14.0, 14.0);
    
    public TechRedstoneButton(BlockSetType blockSetType, int pressTicks, Properties settings) {
        super(blockSetType, pressTicks, settings);
    }
    
    // copied from lever block
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> switch (state.getValue(FACING).getAxis()) {
                case X -> FLOOR_AABB_X;
                default -> FLOOR_AABB_Z;
            };
            case WALL -> switch (state.getValue(FACING)) {
                case EAST -> EAST_AABB;
                case WEST -> WEST_AABB;
                case SOUTH -> SOUTH_AABB;
                default -> NORTH_AABB;
            };
            default -> switch (state.getValue(FACING).getAxis()) {
                case X -> CEILING_AABB_X;
                default -> CEILING_AABB_Z;
            };
        };
    }
}
