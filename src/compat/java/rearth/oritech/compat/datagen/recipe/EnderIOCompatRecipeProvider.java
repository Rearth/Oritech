package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import com.enderio.enderio.EnderIO;
import com.enderio.enderio.init.EIOFluids;
import com.enderio.enderio.init.EIOItems;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.CentrifugeFluidRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.datagen.builders.util.RecipeHelpers;
import rearth.oritech.init.BlockContent;

public class EnderIOCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/enderio";
    private final RecipeOutput modLoadedOutput;
    
    public EnderIOCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(EnderIO.MOD_ID));
    }

    @Override
    protected void buildRecipes() {
        // xp juice from sculk
        new CentrifugeFluidRecipeBuilder(registries)
            .input(Items.SCULK).fluidInput(Fluids.WATER, 0.25f).fluidOutput(EIOFluids.XP_JUICE.source().get(), 0.1f)
            .export(this.modLoadedOutput, PATH, "sculk_xp", Oritech.MOD_ID);

        // powdered obsidian from obsidian
        // EnderIO and Mekanism recipes are both equivalent, and both mods can use either obsidian dust. Loading both recipes shouldn't be a problem.
        new PulverizerRecipeBuilder(registries)
            .input(Tags.Items.OBSIDIANS).result(EIOItems.POWDERED_OBSIDIAN.get(), 4).time(140)
            .addToGrinder().export(this.modLoadedOutput, PATH, "dust/obsidian", Oritech.MOD_ID);
        new PulverizerRecipeBuilder(registries)
            .input(Tags.Items.GEMS_LAPIS).result(EIOItems.POWDERED_LAPIS_LAZULI.get()).time(120)
            .addToGrinder().export(this.modLoadedOutput, PATH, "dust/lapis", Oritech.MOD_ID);

        var conduitBinder = EIOItems.CONDUIT_BINDER.get();
        RecipeHelpers.createInsulatedCableRecipe(RecipeCategory.MISC,
                BlockContent.FLUID_PIPE.asItem(), 6,
                Ingredient.of(conduitBinder),
                Ingredient.of(items.get(Tags.Items.INGOTS_COPPER).get()))
            .unlockedBy(getHasName(conduitBinder), has(conduitBinder))
            .save(this.modLoadedOutput, ResourceKey.create(Registries.RECIPE, Oritech.id(PATH + "/crafting/fluid_pipe")));
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new EnderIOCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "Ender IO Oritech Compat";
        }
    }
    
}
