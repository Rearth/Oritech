package rearth.oritech.init.compat.jei;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.List;

/**
 * Opts the recipe types displayed by JEI into NeoForge's server-to-client recipe sync.
 */
@EventBusSubscriber(modid = Oritech.MOD_ID)
public final class OritechJeiRecipeServerSync {

    private OritechJeiRecipeServerSync() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(List.of(
                RecipeContent.PULVERIZER.get(),
                RecipeContent.FRAGMENT_FORGE.get(),
                RecipeContent.ASSEMBLER.get(),
                RecipeContent.REFINERY.get(),
                RecipeContent.FOUNDRY.get(),
                RecipeContent.CENTRIFUGE.get(),
                RecipeContent.CENTRIFUGE_FLUID.get(),
                RecipeContent.ATOMIC_FORGE.get(),
                RecipeContent.BIO_GENERATOR.get(),
                RecipeContent.FUEL_GENERATOR.get(),
                RecipeContent.LAVA_GENERATOR.get(),
                RecipeContent.STEAM_ENGINE.get(),
                RecipeContent.BEDROCK_EXTRACTOR.get(),
                RecipeContent.PARTICLE_COLLISION.get(),
                RecipeContent.INDUSTRIAL_CHILLER.get(),
                RecipeContent.REACTOR.get(),
                RecipeContent.LASER.get()
        ));
    }
}
