package rearth.oritech.fabricgen.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.init.FluidContent;

import java.util.concurrent.CompletableFuture;

public class FluidTagGenerator extends FabricTagProvider.FluidTagProvider {
    
    public FluidTagGenerator(FabricDataOutput output, CompletableFuture<WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void configure(WrapperLookup wrapperLookup) {
        
        // this is disabled and manually placed in the fabric instance, as it's only needed there
        // add custom fluids to water tag for basic fluid physics
//        getOrCreateTagBuilder(getFluidTag("minecraft", "water"))
//            .add(FluidContent.FLOWING_BIOFUEL.get()).add(FluidContent.STILL_BIOFUEL.get())
//            .add(FluidContent.FLOWING_FUEL.get()).add(FluidContent.STILL_FUEL.get())
//            .add(FluidContent.FLOWING_OIL.get()).add(FluidContent.STILL_OIL.get())
//            .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get())
//            .addOptional(Oritech.id("flowing_molten_adamant")).addOptional(Oritech.id("still_molten_adamant"))
//            .addOptional(Oritech.id("flowing_molten_biosteel")).addOptional(Oritech.id("still_molten_biosteel"))
//            .addOptional(Oritech.id("flowing_molten_duratium")).addOptional(Oritech.id("still_molten_duratium"))
//            .addOptional(Oritech.id("flowing_molten_energite")).addOptional(Oritech.id("still_molten_energite"))
//            .addOptional(Oritech.id("flowing_molten_fluxite")).addOptional(Oritech.id("still_molten_fluxite"));
        
        getOrCreateTagBuilder(getCTag("biodiesel")).add(FluidContent.STILL_BIOFUEL.get());
        getOrCreateTagBuilder(getCTag("high_power_biodiesel")).add(FluidContent.STILL_FUEL.get());
        
        getOrCreateTagBuilder(ConventionalFluidTags.GASEOUS)
          .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        getOrCreateTagBuilder(getCTag("steam"))
          .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        
    }
    
    private static TagKey<Fluid> getCTag(String path) {
        return getFluidTag(TagUtil.C_TAG_NAMESPACE, path);
    }
    
    private static TagKey<Fluid> getFluidTag(String namespace, String path) {
        return TagKey.of(RegistryKeys.FLUID, Identifier.of(namespace, path));
    }
}
