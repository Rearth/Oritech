package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MachineAddonProvider;

import java.util.HashSet;

public class AddonBlockEntity extends BlockEntity implements MachineAddonProvider {
    
    public static final HashSet<MachineAddonController> pendingInits = new HashSet<>();
    public static void completeInits() {
        for (var controller : pendingInits) {
            controller.initAddons();    // todo check if this can be cleaned up using server.execute()?
        }
        
        pendingInits.clear();
    }
    
    private BlockPos controllerPos = BlockPos.ZERO;
    
    public AddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ADDON_ENTITY.get(), pos, state);
    }
    
    public AddonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    public void setControllerPos(BlockPos pos) {
        if (!controllerPos.equals(pos) && level instanceof ServerLevel serverLevel)
            serverLevel.invalidateCapabilities(pos);
        controllerPos = pos;
    }
    
    @Override
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("controller", BlockPos.CODEC, controllerPos);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controllerPos = input.read("controller", BlockPos.CODEC)
                          .orElse(new BlockPos(input.getIntOr("controller_x", 0), input.getIntOr("controller_y", 0), input.getIntOr("controller_z", 0)));
    }
}
