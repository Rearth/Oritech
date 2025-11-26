package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class AtomicForgeRecipeBuilder extends OritechRecipeBuilder {
    private AtomicForgeRecipeBuilder() {
        super(RecipeContent.ATOMIC_FORGE, "atomicforge");
    }

    public static OritechRecipeBuilder build() {
        return new AtomicForgeRecipeBuilder();
    }

    public void validate(ResourceLocation id) throws IllegalStateException {

    }
}