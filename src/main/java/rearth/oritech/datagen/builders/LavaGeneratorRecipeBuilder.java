package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class LavaGeneratorRecipeBuilder extends OritechRecipeBuilder {

    public LavaGeneratorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.LAVA_GENERATOR, "lavagen", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (fluidInput == null)
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
