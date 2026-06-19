package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class ParticleCollisionRecipeBuilder extends OritechRecipeBuilder {

    public ParticleCollisionRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.PARTICLE_COLLISION, "particle", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 2)
            throw new IllegalStateException("Exactly 2 inputs required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
