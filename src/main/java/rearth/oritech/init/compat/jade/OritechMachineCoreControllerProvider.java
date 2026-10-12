package rearth.oritech.init.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum OritechMachineCoreControllerProvider implements IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final Identifier ID = Oritech.id("machine_core_controller");
    static final String CONTROLLER_NAME = "oritech:controller_name";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MachineCoreEntity coreEntity) {
            var controllerEntity = coreEntity.getCachedController();
            if (controllerEntity != null) {
                var controller = accessor.getLevel().getBlockState(controllerEntity.getPosForMultiblock()).getBlock();
                data.putString(CONTROLLER_NAME, controller.getDescriptionId());
            }
        }
    }

    @Override
    public Identifier getUid() {
        return ID;
    }
}
