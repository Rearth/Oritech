package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.common.EAESingletons;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.CentrifugeRecipeBuilder;
import rearth.oritech.datagen.builders.LaserRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

import static rearth.oritech.util.TagUtils.cItemTag;

public class ExtendedAECompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/extendedae";
    private final RecipeOutput modLoadedOutput;
    
    public ExtendedAECompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(ExtendedAE.MODID));
    }

    @Override
    protected void buildRecipes() {
        new PulverizerRecipeBuilder(registries)
            .input(cItemTag("gems/entro")).result(EAESingletons.ENTRO_DUST)
            .addToGrinder().export(this.modLoadedOutput, PATH, "entro_dust", Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ExtendedAECompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "Extended AE Oritech Compat";
        }
    }
    
}
