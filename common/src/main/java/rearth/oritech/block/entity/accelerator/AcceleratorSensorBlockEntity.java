package rearth.oritech.block.entity.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ComparatorOutputProvider;

public class AcceleratorSensorBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorSensorBlockEntity>, ComparatorOutputProvider {
    
    private float measuredSpeed;
    private long measuredTime;
    
    private boolean dirty = false;
    
    public AcceleratorSensorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_SENSOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, AcceleratorSensorBlockEntity blockEntity) {
        if (world.isClientSide) return;
        
        if (measuredSpeed != 0) {
            var age = world.getGameTime() - measuredTime;
            
            if (age > 8) {
                measuredSpeed = 0;
                dirty = true;
            }
        }
        
        if (dirty) {
            dirty = false;
            world.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }
    
    public void measureParticle(AcceleratorParticleLogic.ActiveParticle particle) {
        this.measuredSpeed = particle.velocity;
        this.measuredTime = level.getGameTime();
        dirty = true;
    }

    @Override
    public int getComparatorOutput() {
        if (measuredSpeed <= 0) {
            return 0;
        } else if (measuredSpeed <= 10) {
            return 1;
        } else if (measuredSpeed <= 50) {
            return 2;
        } else if (measuredSpeed <= 75) {
            return 3;
        } else if (measuredSpeed <= 100) {
            return 4;
        } else if (measuredSpeed <= 150) {
            return 5;
        } else if (measuredSpeed <= 250) {
            return 6;
        } else if (measuredSpeed <= 500) {
            return 7;
        } else if (measuredSpeed <= 750) {
            return 8;
        } else if (measuredSpeed <= 1000) {
            return 9;
        } else if (measuredSpeed <= 2500) {
            return 10;
        } else if (measuredSpeed <= 5000) {
            return 11;
        } else if (measuredSpeed <= 7500) {
            return 12;
        } else if (measuredSpeed <= 10000) {
            return 13;
        } else if (measuredSpeed <= 15000) {
            return 14;
        }
        
        return 15;
    }
}
