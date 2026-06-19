package rearth.oritech.datagen.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import rearth.oritech.Oritech;
import rearth.oritech.init.TagContent;

import java.util.concurrent.CompletableFuture;

public class EntityTagGenerator extends EntityTypeTagsProvider {

    public EntityTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture, Oritech.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        tag(TagContent.SPAWNER_BLACKLIST)
          .add(EntityType.ENDER_DRAGON);    // this should never actually be loaded, but it's here as a failsafe
    }
}
