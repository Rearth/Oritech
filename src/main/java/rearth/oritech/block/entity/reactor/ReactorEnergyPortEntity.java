package rearth.oritech.block.entity.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.init.BlockEntitiesContent;

public class ReactorEnergyPortEntity extends BlockEntity implements EnergyProvider {

    // this block is just an energy provider so that pipes will connect. The energy is actually output from the controller
    private final DynamicEnergyStorage dummyStorage = new DynamicEnergyStorage(0, 0, 0, 0);

    public ReactorEnergyPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_ENERGY_PORT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public EnergyHandler getEnergyLookup(Direction direction) {
        return dummyStorage;
    }
}
