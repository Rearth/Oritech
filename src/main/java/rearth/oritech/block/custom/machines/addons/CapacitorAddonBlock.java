package rearth.oritech.block.custom.machines.addons;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.machines.addons.CapacitorAddonBlockEntity;

public class CapacitorAddonBlock extends MachineAddonBlock {
    
    private final long addedCapacity;
    private final long addedInsert;
    
    public CapacitorAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity, long addedInsert) {
        super(settings, extender, speedMultiplier, efficiencyMultiplier);
        this.addedCapacity = addedCapacity;
        this.addedInsert = addedInsert;
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return CapacitorAddonBlockEntity.class;
    }
    
    public long getAddedCapacity() {
        return addedCapacity;
    }
    
    public long getAddedInsert() {
        return addedInsert;
    }
}
