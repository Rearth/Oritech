package rearth.oritech.block.blocks.machines.interaction;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.FrameInteractionBlock;
import rearth.oritech.block.entity.machines.interaction.PlacerBlockEntity;
import rearth.oritech.util.Geometry;

public class PlacerBlock extends FrameInteractionBlock {

    private static final VoxelShape[] BOUNDING_SHAPES;

    public PlacerBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlacerBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BOUNDING_SHAPES[state.get(FACING).ordinal()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    static {
        BOUNDING_SHAPES = new VoxelShape[Direction.values().length];
        for (var facing : Direction.values()) {
            if (!facing.getAxis().isHorizontal()) continue;
            BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.union(
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.3125625, 0.875, 0.6875, 0.6875625, 1.3125), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.4375, 0.4375, 0.75, 0.75, 0.875), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.25, 0.6875, 1, 0.5, 0.8125), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.061875, -0.000625, 0.061875, 0.938125, 0.125625, 0.938125), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.3125, 0.875), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.3125, 0.0625, 0.9375, 0.4375, 0.9375), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0.375, 0.125, 0.6875, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.875, 0, 0.375, 1, 0.6875, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.6875, 0.375, 1, 0.8125, 0.625), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.4375, 0.1875, 0.375, 0.625, 0.25), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.5625, 0.4375, 0.1875, 0.625, 0.625, 0.25), facing, BlockFace.FLOOR),
                Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8125, 0.4375, 0.1875, 0.875, 0.625, 0.25), facing, BlockFace.FLOOR));
        }
    }
}
