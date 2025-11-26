package rearth.oritech.api.recipe;

import net.minecraft.resources.ResourceLocation;
import rearth.oritech.init.recipes.RecipeContent;

public class ParticleCollisionRecipeBuilder extends OritechRecipeBuilder {

    protected ParticleCollisionRecipeBuilder() {
        super(RecipeContent.PARTICLE_COLLISION, "particle");
    }

    public static OritechRecipeBuilder build() {
        return new ParticleCollisionRecipeBuilder();
    }

    @Override
    public void validate(ResourceLocation id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 2)
            throw new IllegalStateException("Exactly 2 inputs required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
