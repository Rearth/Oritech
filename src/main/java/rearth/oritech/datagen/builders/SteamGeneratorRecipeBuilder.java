package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class SteamGeneratorRecipeBuilder extends OritechRecipeBuilder {

    public SteamGeneratorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.STEAM_ENGINE, "steamgen", registryAccess);
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (fluidInput == null)
            throw new IllegalStateException("fluid input required for recipe " + id + " (type " + type + ")");
    }
}
