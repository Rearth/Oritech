package rearth.oritech.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.RegistryReflectionUtil;

import java.util.concurrent.CompletableFuture;

public class OritechDataMapProvider extends DataMapProvider {

    public OritechDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var compostables = builder(NeoForgeDataMaps.COMPOSTABLES);

        RegistryReflectionUtil.IterateFields(ItemContent.class, DeferredItem.class, (field, identifier, value) -> {
            var annotation = field.getAnnotation(ItemContent.Compostable.class);
            if (annotation != null) {
                compostables.add(value.getId(), new Compostable(annotation.value()), false);
            }
        });

        RegistryReflectionUtil.IterateFields(BlockContent.class, DeferredBlock.class, (field, identifier, value) -> {
            var annotation = field.getAnnotation(ItemContent.Compostable.class);
            if (annotation != null) {
                compostables.add(value.getId(), new Compostable(annotation.value()), false);
            }
        });
    }
}
