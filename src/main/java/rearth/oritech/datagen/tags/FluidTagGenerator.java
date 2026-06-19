package rearth.oritech.datagen.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.TagContent;

import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.cFluidTag;

public class FluidTagGenerator extends FluidTagsProvider {
    
    public FluidTagGenerator(PackOutput output, CompletableFuture<Provider> completableFuture) {
        super(output, completableFuture, Oritech.MOD_ID);
    }
    
    @Override
    protected void addTags(Provider wrapperLookup) {
                
        tag(cFluidTag("biodiesel")).add(FluidContent.STILL_BIOFUEL.get());
        tag(cFluidTag("high_power_biodiesel")).add(FluidContent.STILL_FUEL.get());
        
        tag(Tags.Fluids.GASEOUS)
            .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        
        tag(TagContent.STEAM)
            .add(FluidContent.FLOWING_STEAM.get()).add(FluidContent.STILL_STEAM.get());
        
        tag(TagContent.OIL)
            .add(FluidContent.STILL_OIL.get());
        
        tag(TagContent.BIOFUEL)
            .add(FluidContent.STILL_BIOFUEL.get());
        
        tag(TagContent.SULFURIC_ACID)
            .add(FluidContent.STILL_SULFURIC_ACID.get());
        
        tag(TagContent.NAPHTHA)
            .add(FluidContent.STILL_NAPHTHA.get());
        
        tag(TagContent.DIESEL)
            .add(FluidContent.STILL_DIESEL.get());
        
        tag(TagContent.TURBOFUEL)
            .add(FluidContent.STILL_FUEL.get());
    }
}
