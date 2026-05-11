package rearth.oritech.block.entity.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.init.BlockEntitiesContent;

public class ReactorEnergyPortEntity extends BlockEntity implements EnergyApi.BlockProvider {
    
    // this block is just an energy provider so that pipes will connect. The energy is actually output from the controller
    private final SimpleEnergyStorage dummyStorage = new SimpleEnergyStorage(0, 0, 0);
    
    public ReactorEnergyPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_ENERGY_PORT_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return dummyStorage;
    }
}
