package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class LaserRecipeBuilder extends OritechRecipeBuilder {

    protected LaserRecipeBuilder() {
        super(RecipeContent.DEEP_DRILL, "laser");
    }

    public static OritechRecipeBuilder build() {
        return new LaserRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 1)
            throw new IllegalStateException("Exactly 1 input required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
