package rearth.oritech.init.compat.jei;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;

import java.util.Collection;

/**
 * Requests Oritech's server-side recipes and keeps the latest client-side copy for JEI.
 * Minecraft 26.1 no longer exposes the full recipe manager through the client level.
 */
@EventBusSubscriber(modid = Oritech.MOD_ID, value = Dist.CLIENT)
public final class OritechJeiRecipeSync {

    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    private OritechJeiRecipeSync() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        clientRecipes = event.getRecipeMap();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clientRecipes = RecipeMap.EMPTY;
    }

    static Collection<RecipeHolder<OritechRecipe>> getRecipes(RecipeType<OritechRecipe> recipeType) {
        return clientRecipes.byType(recipeType);
    }
}
