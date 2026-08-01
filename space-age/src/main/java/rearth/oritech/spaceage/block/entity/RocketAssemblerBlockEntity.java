package rearth.oritech.spaceage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;

public class RocketAssemblerBlockEntity extends BlockEntity {

    public RocketAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceAgeBlockEntities.ROCKET_ASSEMBLER.get(), pos, state);
    }
}
