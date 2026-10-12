package rearth.oritech.init.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum OritechMachineCoreControllerTooltipProvider implements IBlockComponentProvider {

    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        accessor.getServerData().getString(OritechMachineCoreControllerProvider.CONTROLLER_NAME).ifPresent(name ->
                tooltip.add(Component.translatable(name).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC)));
    }

    @Override
    public Identifier getUid() {
        return OritechMachineCoreControllerProvider.INSTANCE.getUid();
    }
}
