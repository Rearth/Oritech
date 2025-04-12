package rearth.oritech.api.recipe;

import net.minecraft.util.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class DeepDrillRecipeBuilder extends OritechRecipeBuilder {

    protected DeepDrillRecipeBuilder() {
        super(RecipeContent.DEEP_DRILL, "deepdrill");
        // Set the default time for deep drill recipes
        this.time = 1;
    }

    public static OritechRecipeBuilder build() {
        return new DeepDrillRecipeBuilder();
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 1)
            throw new IllegalStateException("Exactly 1 input required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
