package rearth.oritech.spaceage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import rearth.oritech.spaceage.client.RocketRenderer;
import rearth.oritech.spaceage.datagen.SpaceAgeDataGenerators;

@Mod(value = OritechSpaceAge.MOD_ID, dist = Dist.CLIENT)
public final class OritechSpaceAgeClient {

    public OritechSpaceAgeClient(IEventBus modEventBus, ModContainer modContainer) {
        OritechSpaceAge.LOGGER.info("Initializing Oritech: Space Age client");

        NeoForge.EVENT_BUS.addListener(RocketRenderer::onExtractRenderState);
        NeoForge.EVENT_BUS.addListener(RocketRenderer::onSubmitGeometry);
        modEventBus.addListener(SpaceAgeDataGenerators::gatherData);
    }
}
