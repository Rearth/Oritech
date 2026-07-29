package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class BedrockExtractorRecipeBuilder extends OritechRecipeBuilder {

    public BedrockExtractorRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.BEDROCK_EXTRACTOR, "deepdrill", registryAccess);
        // Set the default time for deep drill recipes
        this.time = 1;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 1)
            throw new IllegalStateException("Exactly 1 input required for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Exactly 1 result required for recipe " + id + " (type " + type + ")");
    }
}
