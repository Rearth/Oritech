package rearth.oritech.block.entity.reactor;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.energy.EnergyApi;
import rearth.oritech.util.energy.containers.SimpleEnergyStorage;

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
