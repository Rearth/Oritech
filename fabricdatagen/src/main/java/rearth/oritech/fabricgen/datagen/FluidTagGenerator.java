package rearth.oritech.fabricgen.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalFluidTags;
import net.minecraft.core.HolderLookup.Provider;
import rearth.oritech.Oritech;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.TagContent;

import static rearth.oritech.util.TagUtils.*;

import java.util.concurrent.CompletableFuture;

public class FluidTagGenerator extends FabricTagProvider.FluidTagProvider {
    
    public FluidTagGenerator(FabricDataOutput output, CompletableFuture<Provider> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void addTags(Provider wrapperLookup) {
        
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
        
        tag(cFluidTag("biodiesel")).add(FluidContent.STILL_BIOFUEL.get());
        tag(cFluidTag("high_power_biodiesel")).add(FluidContent.STILL_FUEL.get());
        
        tag(ConventionalFluidTags.GASEOUS)
            .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        tag(cFluidTag("steam"))
            .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        
        tag(TagContent.OIL)
            .add(FluidContent.STILL_OIL.get());
        
        tag(TagContent.BIOFUEL)
            .add(FluidContent.STILL_BIOFUEL.get());
        
        tag(TagContent.TURBOFUEL)
            .add(FluidContent.STILL_FUEL.get());
    }
}
