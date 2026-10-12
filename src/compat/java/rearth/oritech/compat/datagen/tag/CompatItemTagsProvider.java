package rearth.oritech.compat.datagen.tag;

import java.util.concurrent.CompletableFuture;

import appeng.core.definitions.AEItems;
import com.enderio.enderio.init.EIOItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import rearth.oritech.compat.datagen.CompatDataProviders;

public class CompatItemTagsProvider extends ItemTagsProvider {

    public CompatItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CompatDataProviders.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        tag(CompatTags.Items.C_ENDER_DUST)
            .addOptional(AEItems.ENDER_DUST.get())
            .addOptional(EIOItems.POWDERED_ENDER_PEARL.get());
    }    
}
