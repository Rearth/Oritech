package rearth.oritech.init.compat.jade;

import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.MachineCoreBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class OritechJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        Oritech.LOGGER.info("Registering Jade providers");
        registration.registerBlockDataProvider(OritechMachineCoreControllerProvider.INSTANCE, MachineCoreBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        Oritech.LOGGER.info("Registering Jade client providers");
        registration.registerBlockComponent(OritechMachineCoreControllerProvider.INSTANCE, MachineCoreBlock.class);
    }
}