package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.init.BlockEntitiesContent;

public class AcceleratorSensorBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorSensorBlockEntity> {
    
    private float measuredSpeed;
    private long measuredTime;
    
    private boolean dirty = false;
    
    public AcceleratorSensorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_SENSOR_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, AcceleratorSensorBlockEntity blockEntity) {
        if (world.isClient) return;
        
        if (measuredSpeed != 0) {
            var age = world.getTime() - measuredTime;
            
            if (age > 3) {
                measuredSpeed = 0;
                dirty = true;
            }
        }
        
        if (dirty) {
            dirty = false;
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }
    
    public void measureParticle(AcceleratorParticleLogic.ActiveParticle particle) {
        this.measuredSpeed = particle.velocity;
        this.measuredTime = world.getTime();
        dirty = true;
    }
    
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
