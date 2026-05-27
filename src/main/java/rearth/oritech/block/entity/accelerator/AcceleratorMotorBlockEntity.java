package rearth.oritech.block.entity.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.config.OritechConfig;

public class AcceleratorMotorBlockEntity extends BlockEntity implements EnergyProvider {
    
    private final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.acceleratorMotorRFCapacity.get(), OritechConfig.acceleratorMotorRFCapacity.get(), OritechConfig.acceleratorMotorRFCapacity.get(), 0, this::setChanged, false);
    
    public AcceleratorMotorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_MOTOR_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energyStorage.serialize(output);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.deserialize(input);
    }
}
