package rearth.oritech.init.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum OritechMachineCoreControllerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final ResourceLocation ID = Oritech.id("machine_core_controller");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getServerData().contains("controller")) {
            tooltip.add(Component.translatable(accessor.getServerData().getString("controller")).withStyle(ChatFormatting.WHITE).withStyle(ChatFormatting.ITALIC));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof MachineCoreEntity coreEntity) {
            var controllerEntity = coreEntity.getCachedController();
            if (controllerEntity != null) {
                var controller = accessor.getLevel().getBlockState(controllerEntity.getPosForMultiblock()).getBlock();
                data.putString("controller", controller.getDescriptionId());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
        
}
