package rearth.oritech.generator;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;
import rearth.oritech.init.TagContent;

import java.util.concurrent.CompletableFuture;

public class EntityTagGenerator extends FabricTagProvider.EntityTypeTagProvider {
    public EntityTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(TagContent.SPAWNER_BLACKLIST)
          .add(EntityType.ENDER_DRAGON);    // this should never actually be loaded, but it's here as a failsafe
    }
}
