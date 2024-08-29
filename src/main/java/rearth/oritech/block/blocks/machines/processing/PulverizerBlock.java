package rearth.oritech.block.blocks.machines.processing;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.UpgradableMachineBlock;
import rearth.oritech.block.entity.machines.processing.PulverizerBlockEntity;
import rearth.oritech.util.Geometry;

public class PulverizerBlock extends UpgradableMachineBlock implements BlockEntityProvider {

    private static final VoxelShape[] BOUNDING_SHAPES;
    
    public PulverizerBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return PulverizerBlockEntity.class;
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
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0, 0.0625, 0.9375, 0.125, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0.875, 1, 1, 1), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.6875, 0.125, 0.75, 0.8125, 0.8125, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.1875, 0.125, 0.75, 0.3125, 0.8125, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.125, 0.8125, 0.5625, 0.875, 0.9375, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.0625, 0.125, 0.3125, 0.9375, 0.25, 0.75), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.3125, 0.875, 0.3125, 0.6875, 1, 0.875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.453125, 0.125, 0.328125, 0.546875, 0.875, 0.390625), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.328125, 0.25, 0.453125, 0.390625, 0.875, 0.546875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.453125, 0.125, 0.609375, 0.5625, 0.875, 0.671875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.609375, 0.25, 0.453125, 0.671875, 0.875, 0.546875), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.40625, 0.125, 0.40625, 0.59375, 0.875, 0.59375), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.0625, 0, 1, 0.1875, 0.125), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0.0625, 0.125, 0.125, 0.1875, 0.3125), facing, BlockFace.FLOOR),
                    Geometry.rotateVoxelShape(VoxelShapes.cuboid(0.875, 0.0625, 0.125, 1, 0.1875, 0.3125), facing, BlockFace.FLOOR));
            }
        }
    }
}
