package rearth.oritech.spaceage.block.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlanetBlock extends Block {

    private final VoxelShape shape;

    public PlanetBlock(Properties properties) {

        this(properties, 32);
    }

    public PlanetBlock(Properties properties, int size) {

        super(properties);
        shape = Block.box(8 - size / 2.0, 8 - size / 2.0, 8 - size / 2.0,
                8 + size / 2.0, 8 + size / 2.0, 8 + size / 2.0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {

        return shape;
    }
}
