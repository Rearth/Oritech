package rearth.oritech.block.entity.machines;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.init.BlockEntitiesContent;

public class MachineCoreEntity extends BlockEntity {
    
    private BlockPos controllerPos = BlockPos.ORIGIN;
    
    public MachineCoreEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.MACHINE_CORE_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("controller_x", controllerPos.getX());
        nbt.putInt("controller_y", controllerPos.getY());
        nbt.putInt("controller_z", controllerPos.getZ());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        controllerPos = new BlockPos(nbt.getInt("controller_x"), nbt.getInt("controller_y"), nbt.getInt("controller_z"));
    }
    
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        this.markDirty();
    }
}
