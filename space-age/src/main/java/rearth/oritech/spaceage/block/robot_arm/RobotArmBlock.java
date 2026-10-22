package rearth.oritech.spaceage.block.robot_arm;

import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.block.base.block.MultiblockMachine;

public final class RobotArmBlock extends MultiblockMachine {

    public RobotArmBlock(Properties properties) {

        super(properties);
    }

    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {

        return RobotArmBlockEntity.class;
    }
}
