package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;

public class OritechEMIParticleCollisionRecipe extends BasicEmiRecipe {
    
    public static final Identifier PARTICLE_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/particle_recipe_overlay.png");
    
    private final OritechRecipe recipe;
    
    public OritechEMIParticleCollisionRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category) {
        super(category, entry.id(), 160, 60);
        
        recipe = entry.value();
        recipe.getInputs().forEach(ingredient -> this.inputs.add(EmiIngredient.of(ingredient)));
        recipe.getResults().forEach(stack -> this.outputs.add(EmiStack.of(stack)));
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        
        widgets.addTexture(PARTICLE_RECIPE_OVERLAY, 60, 17, 36, 24, 0, 0, 36, 24, 36, 24);

        widgets.addSlot(this.inputs.get(0), 42, 20);
        widgets.addSlot(this.inputs.get(1), 96, 20);
        
        
        widgets.addSlot(this.outputs.get(0), 69, 20).recipeContext(this).drawBack(false);
        widgets.addText(Text.translatable("emi.title.oritech.collisionspeed", this.recipe.getTime()), 0, (int) (getDisplayHeight() * 0.88), 0xFFFFFF, true);
        
    }
}
