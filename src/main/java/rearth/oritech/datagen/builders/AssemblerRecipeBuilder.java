package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class AssemblerRecipeBuilder extends OritechRecipeBuilder {

    public AssemblerRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.ASSEMBLER, "assembler", registryAccess);
        this.time = 120;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs == null || inputs.size() != 4)
            throw new IllegalStateException("Need exactly 4 inputs for recipe " + id + " (type " + type + ")");
        if (results == null || results.size() != 1)
            throw new IllegalStateException("Need exactly 1 result for recipe " + id + " (type " + type + ")");
    }
}
