package rearth.oritech.util;

import net.minecraft.util.math.BlockPos;

public interface MachineAddonProvider {
    
    void setControllerPos(BlockPos pos);
    BlockPos getControllerPos();
    
}
