package rearth.oritech.util;

import net.minecraft.core.BlockPos;

public interface MachineAddonProvider {
    
    void setControllerPos(BlockPos pos);
    BlockPos getControllerPos();
    
}
