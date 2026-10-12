package rearth.oritech.datagen.builders;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.recipes.RecipeContent;

public class PulverizerRecipeBuilder extends OritechRecipeBuilder {

    public PulverizerRecipeBuilder(HolderLookup.Provider registryAccess) {
        super(RecipeContent.PULVERIZER, "pulverizer", registryAccess);
        this.time = 100;
    }

    @Override
    public void validate(Identifier id) throws IllegalStateException {
        if ((inputs == null || inputs.isEmpty()) || (results == null || results.isEmpty()))
            throw new IllegalStateException("inputs and results required for recipe " + id + " (type " + type + ")");
    }

    @Override
    public void export(RecipeOutput exporter, @Nullable String prefix, String suffix, String namespace) {
        super.export(exporter, prefix, suffix, namespace);

        if (addToFragmentForge)
            // Fragment Forge defaults to 20% more time, rounded to the nearest half second.
            // For a different time, build the Fragment Forge recipe separately.
            new FragmentForgeRecipeBuilder(registryAccess).input(inputs).resultTemplates(results).time((int) (Math.round(this.time * 1.2 / 10.0) * 10)).timeMultiplier(timeMultiplier).export(exporter, prefix, suffix, namespace);
    }
}
