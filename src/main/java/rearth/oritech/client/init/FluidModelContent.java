package rearth.oritech.client.init;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import rearth.oritech.Oritech;
import rearth.oritech.init.FluidContent;
import rearth.oritech.util.ColorHelper;

@EventBusSubscriber(modid = Oritech.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidModelContent {
    
    private static final Material GAS_DARK = new Material(Oritech.id("block/fluid/fluid_gas_dark"));
    private static final Material STRANGE_PALE = new Material(Oritech.id("block/fluid/fluid_strange_pale_2"));
    private static final Material STEAM = new Material(Oritech.id("block/fluid/fluid_steam"));
    private static final Material MOLTEN = new Material(Oritech.id("block/fluid/fluid_molten"));
    private static final Material MOLTEN_METAL = new Material(Oritech.id("block/fluid/molten_metal"));
    private static final Material ROILING_PLASMA = new Material(Oritech.id("block/fluid/fluid_roiling_plasma"));
    private static final Material STRANGE_MIXTURE = new Material(Oritech.id("block/fluid/fluid_strange_mixture"));
    
    private FluidModelContent() {
    }
    
    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(GAS_DARK, GAS_DARK, null, FluidTintSources.constant(ColorHelper.argb(0.478f, 0.478f, 0.478f))), FluidContent.STILL_OIL, FluidContent.FLOWING_OIL);
        event.register(new FluidModel.Unbaked(STRANGE_PALE, STRANGE_PALE, null, FluidTintSources.constant(ColorHelper.argb(0.176f, 0.239f, 0.282f))), FluidContent.STILL_FUEL, FluidContent.FLOWING_FUEL);
        event.register(new FluidModel.Unbaked(STRANGE_PALE, STRANGE_PALE, null, FluidTintSources.constant(ColorHelper.argb(0.25f, 0.316f, 0.086f))), FluidContent.STILL_BIOFUEL, FluidContent.FLOWING_BIOFUEL);
        event.register(new FluidModel.Unbaked(STEAM, STEAM, null, FluidTintSources.constant(ColorHelper.WHITE)), FluidContent.STILL_STEAM, FluidContent.FLOWING_STEAM);
        event.register(new FluidModel.Unbaked(MOLTEN, MOLTEN, null, FluidTintSources.constant(ColorHelper.argb(0.135f, 0.135f, 0.135f))), FluidContent.STILL_HEAVY_OIL, FluidContent.FLOWING_HEAVY_OIL);
        event.register(new FluidModel.Unbaked(STEAM, STEAM, null, FluidTintSources.constant(ColorHelper.argb(0.735f, 0.735f, 0.235f))), FluidContent.STILL_DIESEL, FluidContent.FLOWING_DIESEL);
        event.register(new FluidModel.Unbaked(MOLTEN, MOLTEN, null, FluidTintSources.constant(ColorHelper.argb(0.949f, 0.929f, 0.745f))), FluidContent.STILL_NAPHTHA, FluidContent.FLOWING_NAPHTHA);
        event.register(new FluidModel.Unbaked(STEAM, STEAM, null, FluidTintSources.constant(ColorHelper.argb(0.398f, 1f, 0.3f))), FluidContent.STILL_SULFURIC_ACID, FluidContent.FLOWING_SULFURIC_ACID);
        event.register(new FluidModel.Unbaked(STEAM, STEAM, null, FluidTintSources.constant(ColorHelper.argb(0.7f, 1f, 0.7f))), FluidContent.STILL_SILICON_WASH, FluidContent.FLOWING_SILICON_WASH);
        event.register(new FluidModel.Unbaked(MOLTEN_METAL, MOLTEN_METAL, null, FluidTintSources.constant(ColorHelper.argb(0.627f, 0.849f, 1f))), FluidContent.STILL_MINERAL_SLURRY, FluidContent.FLOWING_MINERAL_SLURRY);
        event.register(new FluidModel.Unbaked(ROILING_PLASMA, ROILING_PLASMA, null, FluidTintSources.constant(ColorHelper.argb(1f, 0.7f, 0.7f))), FluidContent.STILL_SHEOL_FIRE, FluidContent.FLOWING_SHEOL_FIRE);
        event.register(new FluidModel.Unbaked(STRANGE_MIXTURE, STRANGE_MIXTURE, null, FluidTintSources.constant(ColorHelper.argb(1f, 1f, 1f))), FluidContent.STILL_STRANGE_MATTER, FluidContent.FLOWING_STRANGE_MATTER);
    }
}
