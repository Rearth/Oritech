package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import rearth.oritech.init.recipes.RecipeContent;

public class CentrifugeFluidRecipeBuilder extends OritechRecipeBuilder {

    public CentrifugeFluidRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.CENTRIFUGE_FLUID, "centrifuge/fluid", registryAccess);
        this.time = 100;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if (inputs != null && inputs.size() > 1)
            throw new IllegalStateException("too many inputs for recipe " + id + " (type " + type + ")");
        if (fluidInput == null && fluidOutputs.isEmpty())
            throw new IllegalStateException("fluid input or output required for recipe " + id + " (type " + type + ")");
    }

}
