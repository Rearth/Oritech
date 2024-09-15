package rearth.oritech.init.compat.jade;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.util.MultiblockMachineController;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum OritechMachineCoreControllerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final Identifier ID = Oritech.id("machine_core_controller");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains("controller")) {
            tooltip.add(Text.translatable(accessor.getServerData().getString("controller")).formatted(Formatting.WHITE).formatted(Formatting.ITALIC));
        }
    }

    @Override
    public void appendServerData(NbtCompound data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MachineCoreEntity coreEntity) {
            var controllerEntity = coreEntity.getCachedController();
            if (controllerEntity != null) {
                var controller = accessor.getLevel().getBlockState(controllerEntity.getMachinePos()).getBlock();
                data.putString("controller", controller.getTranslationKey());
            }
        }
    }

    @Override
    public Identifier getUid() {
        return ID;
    }
        
}
