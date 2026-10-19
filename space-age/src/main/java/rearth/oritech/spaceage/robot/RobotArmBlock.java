package rearth.oritech.spaceage.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.block.MultiblockMachine;

public final class RobotArmBlock extends MultiblockMachine {

    public RobotArmBlock(Properties properties) {

        super(properties);
    }

    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {

        return RobotArmBlockEntity.class;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {

        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {

        return level.getBlockEntity(pos) instanceof RobotArmBlockEntity arm && arm.pulseTicks > 0 ? 15 : 0;
    }
}
