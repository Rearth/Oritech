package rearth.oritech.spaceage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import rearth.oritech.spaceage.client.GroundBeamRenderer;
import rearth.oritech.spaceage.client.RocketRenderer;
import rearth.oritech.spaceage.datagen.SpaceAgeDataGenerators;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeDataMaps;
import rearth.oritech.spaceage.init.SpaceAgeMenus;
import rearth.oritech.spaceage.robot.client.RobotArmRenderer;
import rearth.oritech.spaceage.robot.client.RobotWorkArea;
import rearth.oritech.spaceage.simulation.BeamAperture;
import rearth.oritech.util.TooltipHelper;

import java.util.Locale;

@Mod(value = OritechSpaceAge.MOD_ID, dist = Dist.CLIENT)
public final class OritechSpaceAgeClient {

    public OritechSpaceAgeClient(IEventBus modEventBus, ModContainer modContainer) {

        OritechSpaceAge.LOGGER.info("Initializing Oritech: Space Age client");

        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            if (event.getItemStack().getItem() instanceof BlockItem item) {
                var quality = item.getBlock().builtInRegistryHolder().getData(SpaceAgeDataMaps.BEAM_APERTURE_MATERIALS);
                if (quality != null && quality > 0) event.getToolTip().add(Component.translatable("tooltip.oritech_space_age.aperture_glass",
                        String.format(Locale.ROOT, "%.2f", quality), TooltipHelper.getEnergyText((long) (quality * BeamAperture.RF_PER_GLASS))));
            }
            if (event.getItemStack().is(SpaceAgeBlocks.BEAM_APERTURE.get().asItem())) {
                event.getToolTip().add(Component.translatable("tooltip.oritech_space_age.beam_aperture.structure"));
                event.getToolTip().add(Component.translatable("tooltip.oritech_space_age.beam_aperture.operation"));
            }
        });
        NeoForge.EVENT_BUS.addListener(RocketRenderer::onExtractRenderState);
        NeoForge.EVENT_BUS.addListener(RocketRenderer::onSubmitGeometry);
        NeoForge.EVENT_BUS.addListener(GroundBeamRenderer::extract);
        NeoForge.EVENT_BUS.addListener(GroundBeamRenderer::submit);
        NeoForge.EVENT_BUS.addListener(GroundBeamRenderer::clear);
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(SpaceAgeBlockEntities.ROBOT_ARM.get(), RobotArmRenderer::new));
        NeoForge.EVENT_BUS.addListener(RobotWorkArea::extract);
        NeoForge.EVENT_BUS.addListener(RobotWorkArea::submit);
        modEventBus.addListener(SpaceAgeDataGenerators::gatherData);
        modEventBus.addListener(SpaceAgeMenus::registerScreens);
    }
}
