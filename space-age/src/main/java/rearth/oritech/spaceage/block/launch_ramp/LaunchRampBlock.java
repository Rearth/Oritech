package rearth.oritech.spaceage.block.launch_ramp;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.block.MultiblockMachine;

/** The launcher points along the player's placement direction. */
public class LaunchRampBlock extends MultiblockMachine {

    public LaunchRampBlock(Properties properties) {

        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {

        return LaunchRampBlockEntity.class;
    }
}
