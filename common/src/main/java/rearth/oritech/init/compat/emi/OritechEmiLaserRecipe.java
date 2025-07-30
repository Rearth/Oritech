package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;

public class OritechEmiLaserRecipe extends BasicEmiRecipe {
    
    public static final ResourceLocation LASER_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/laser_recipe_background.png");
    
    public OritechEmiLaserRecipe(RecipeHolder<OritechRecipe> entry, EmiRecipeCategory category) {
        super(category, entry.id(), 160, 80);
        
        var recipe = entry.value();
        recipe.getInputs().forEach(ingredient -> this.inputs.add(EmiIngredient.of(ingredient)));
        recipe.getResults().forEach(stack -> this.outputs.add(EmiStack.of(stack)));
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        
        widgets.addSlot(this.inputs.get(0), 80, 15).drawBack(false);
        widgets.addFillingArrow(105, 15, 3000);
        widgets.addSlot(this.outputs.get(0), 135, 15).recipeContext(this).drawBack(true);
        
        widgets.addTexture(LASER_RECIPE_OVERLAY, 10, 5, 80, 80, 0, 0, 300, 300, 300, 300);
        
    }
}
