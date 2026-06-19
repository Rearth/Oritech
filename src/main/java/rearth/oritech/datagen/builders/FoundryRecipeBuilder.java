package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class FoundryRecipeBuilder extends OritechRecipeBuilder {

    public FoundryRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.FOUNDRY, "foundry/alloy", registryAccess);
        this.time = 80;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.size() < 2) || (results == null || results.isEmpty()))
            throw new IllegalStateException("wrong number of inputs and results for recipe " + id + " (type " + type + ")");
    }
}
