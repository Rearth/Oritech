package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class LaserRecipeBuilder extends OritechRecipeBuilder {

    protected LaserRecipeBuilder() {
        super(RecipeContent.LASER, "laser");
        // Set the default time for laser recipes
        this.time = 1;
    }

    public static OritechRecipeBuilder build() {
        return new LaserRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 1)
            throw new IllegalStateException("Exactly 1 input required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
