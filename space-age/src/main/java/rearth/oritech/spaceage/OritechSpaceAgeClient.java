package rearth.oritech.spaceage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import rearth.oritech.spaceage.datagen.SpaceAgeDataGenerators;

@Mod(value = OritechSpaceAge.MOD_ID, dist = Dist.CLIENT)
public final class OritechSpaceAgeClient {

    public OritechSpaceAgeClient(IEventBus modEventBus, ModContainer modContainer) {
        OritechSpaceAge.LOGGER.info("Initializing Oritech: Space Age client");

        modEventBus.addListener(SpaceAgeDataGenerators::gatherData);
    }
}
