package rearth.oritech.spaceage.block.mission_control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive ground antenna; spacecraft radio upkeep is handled by the simulation. */
public class AntennaBlock extends Block {

    public static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 1, 13),
            Block.box(5, 1, 5, 11, 3, 11),
            Block.box(3.5, .5, 3.5, 4.5, 1.5, 4.5),
            Block.box(11.5, .5, 3.5, 12.5, 1.5, 4.5),
            Block.box(3.5, .5, 11.5, 4.5, 1.5, 12.5),
            Block.box(11.5, .5, 11.5, 12.5, 1.5, 12.5),
            Block.box(7, 3, 7, 9, 16, 9),
            Block.box(0, 14, 7.75, 16, 16, 8.25),
            Block.box(0, 10, 7.75, 16, 12, 8.25),
            Block.box(0, 6, 7.75, 16, 8, 8.25));

    public AntennaBlock(Properties properties) {

        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return SHAPE;
    }
}
