package rearth.oritech.block.blocks.machines.addons;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;

public class EnergyAddonBlock extends MachineAddonBlock {
    
    private final long addedCapacity;
    private final long addedInsert;
    private final boolean acceptEnergy;
    
    public EnergyAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity, long addedInsert, boolean acceptEnergy) {
        super(settings, extender, speedMultiplier, efficiencyMultiplier);
        this.addedCapacity = addedCapacity;
        this.addedInsert = addedInsert;
        this.acceptEnergy = acceptEnergy;
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        if (acceptEnergy) {
            return EnergyAcceptorAddonBlockEntity.class;
        } else {
            return super.getBlockEntityType();
        }
    }
    
    public long getAddedCapacity() {
        return addedCapacity;
    }
    
    public long getAddedInsert() {
        return addedInsert;
    }
}
