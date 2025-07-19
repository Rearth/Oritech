package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class GrinderRecipeBuilder extends OritechRecipeBuilder {

    protected GrinderRecipeBuilder() {
        super(RecipeContent.GRINDER, "grinder");
        this.time = 80;
    }

    public static OritechRecipeBuilder build() {
        return new GrinderRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results required for recipe " + id + " (type " + type + ")");
    }
    
}
