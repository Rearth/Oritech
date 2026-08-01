package rearth.oritech.spaceage.datagen;

import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import rearth.oritech.spaceage.datagen.tags.SpaceAgeBlockTagProvider;

import java.util.List;
import java.util.Set;

public final class SpaceAgeDataGenerators {

    private SpaceAgeDataGenerators() {
    }

    public static void gatherData(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();

        event.createProvider(SpaceAgeModelProvider::new);
        generator.addProvider(true, new SpaceAgeLanguageProvider(output));
        generator.addProvider(true, new SpaceAgeRecipeProvider.Runner(output, lookupProvider));
        generator.addProvider(true, new SpaceAgeBlockTagProvider(output, lookupProvider));
        generator.addProvider(true, new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(SpaceAgeBlockLootProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider
        ));
    }
}
