package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.machines.generators.BasicGeneratorEntity;
import rearth.oritech.util.Geometry;

public class BasicGeneratorBlock extends UpgradableMachineBlock {

    private static final VoxelShape[] BOUNDING_SHAPES;

    public BasicGeneratorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return BasicGeneratorEntity.class;
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
            if (!facing.getAxis().isHorizontal()) {
                BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.fullCube();
            } else {
                BOUNDING_SHAPES[facing.ordinal()] = VoxelShapes.union(
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.25, 0.375, 0.25, 0.75, 0.6875, 0.75), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0, 1, 0.125, 1), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.000625, 0.3125, 0.000625, 0.999375, 0.375, 0.999375), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.125, 0.0625, 0.9375, 0.3125, 0.9375), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, 0.8734375, 0.625, 0.625, 1.0000625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(-0.0000625, 0.375, 0.375, 0.1265625, 0.625, 0.625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.8734375, 0.375, 0.375, 1.00006245, 0.625, 0.625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.375, 0.875, 1, 0.5, 0.99875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.000625, 0.375, 0.125, 0.125, 0.5, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.875, 0.375, 0.125, 0.999375, 0.5, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.375, 0.000625, 1, 0.5, 0.125), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.375, 0.375, -0.0000625, 0.625, 0.625, 0.1265625), facing, BlockFace.FLOOR));
            }
        }
    }
}
