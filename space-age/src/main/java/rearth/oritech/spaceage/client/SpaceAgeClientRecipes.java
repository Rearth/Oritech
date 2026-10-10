package rearth.oritech.spaceage.client;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.Collection;

/**
 * Vanilla no longer exposes full client recipes. This cache also works when JEI is absent.
 */
@EventBusSubscriber(modid = OritechSpaceAge.MOD_ID, value = Dist.CLIENT)
public final class SpaceAgeClientRecipes {

    public static int revision;
    private static RecipeMap recipes = RecipeMap.EMPTY;

    private SpaceAgeClientRecipes() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void received(RecipesReceivedEvent event) {

        recipes = event.getRecipeMap();
        revision++;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {

        recipes = RecipeMap.EMPTY;
        revision++;
    }

    public static Collection<RecipeHolder<VacuumRecipe>> vacuum() {

        return recipes.byType(SpaceAgeRecipes.VACUUM_CRAFTING.get());
    }
}
