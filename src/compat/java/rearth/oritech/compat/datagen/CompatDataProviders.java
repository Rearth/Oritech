package rearth.oritech.compat.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import rearth.oritech.compat.datagen.recipe.AE2CompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.ATOCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.CommonCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.EnderIOCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.ExtendedAECompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.FTBMaterialsCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.GeOreCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.PowahCompatRecipeProvider;
import rearth.oritech.compat.datagen.recipe.ProductiveMetalworksCompatRecipeProvider;
import rearth.oritech.compat.datagen.tag.CompatItemTagsProvider;

@EventBusSubscriber(modid = CompatDataProviders.MOD_ID)
public class CompatDataProviders {
    public static final String MOD_ID = "oritech_compat";
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        final var generator = event.getGenerator();
        final var packOutput = generator.getPackOutput();
        final var lookupProvider = event.getLookupProvider();

        event.createProvider(CommonCompatRecipeProvider.Runner::new);
        event.createProvider(AE2CompatRecipeProvider.Runner::new);
        event.createProvider(ATOCompatRecipeProvider.Runner::new);
        event.createProvider(ExtendedAECompatRecipeProvider.Runner::new);
        event.createProvider(EnderIOCompatRecipeProvider.Runner::new);
        event.createProvider(FTBMaterialsCompatRecipeProvider.Runner::new);
        event.createProvider(GeOreCompatRecipeProvider.Runner::new);
        event.createProvider(PowahCompatRecipeProvider.Runner::new);
        event.createProvider(ProductiveMetalworksCompatRecipeProvider.Runner::new);

        generator.addProvider(true, new CompatItemTagsProvider(packOutput, lookupProvider));
    }
}
