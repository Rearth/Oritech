package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class AtomicForgeRecipeBuilder extends OritechRecipeBuilder {
    public AtomicForgeRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.ATOMIC_FORGE, "atomicforge", registryAccess);
    }

    public void validate(Identifier id) throws IllegalStateException {

    }
}
