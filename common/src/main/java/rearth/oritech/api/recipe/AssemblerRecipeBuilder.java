package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class AssemblerRecipeBuilder extends OritechRecipeBuilder {

    protected AssemblerRecipeBuilder() {
        super(RecipeContent.ASSEMBLER, "assembler");
        this.time = 120;
    }

    public static OritechRecipeBuilder build() {
        return new AssemblerRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 4)
            throw new IllegalStateException("Need exactly 4 inputs for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Need exactly 1 result for recipe " + id + " (type " + type + ")");
    }        
}
