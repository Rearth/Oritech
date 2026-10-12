package rearth.oritech.init.compat.jade;

import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class OritechJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(OritechMachineCoreControllerProvider.INSTANCE, MachineCoreEntity.class);
        registration.registerEnergyStorage(OritechCoreEnergyProvider.INSTANCE, MachineCoreEntity.class);
        registration.registerFluidStorage(OritechCoreFluidProvider.INSTANCE, MachineCoreEntity.class);
        registration.registerItemStorage(OritechCoreItemProvider.INSTANCE, MachineCoreEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(OritechMachineCoreControllerTooltipProvider.INSTANCE, MachineCoreBlock.class);
        registration.registerEnergyStorageClient(OritechCoreEnergyProvider.Client.INSTANCE);
        registration.registerFluidStorageClient(OritechCoreFluidProvider.Client.INSTANCE);
        registration.registerItemStorageClient(OritechCoreItemProvider.Client.INSTANCE);
    }

    static boolean isConnectedCore(Accessor<?> accessor) {
        // The block state is synced already; the client does not need the controller position.
        return accessor instanceof BlockAccessor blockAccessor
                && blockAccessor.getBlock() instanceof MachineCoreBlock
                && blockAccessor.getBlockState().getValue(MachineCoreBlock.USED);
    }
}
