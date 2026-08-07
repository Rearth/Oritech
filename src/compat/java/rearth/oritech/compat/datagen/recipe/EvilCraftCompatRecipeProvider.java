package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.itemTag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.CentrifugeFluidRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;

import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;

public class EvilCraftCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/evilcraft";
    private final RecipeOutput modLoadedOutput;
    
    public EvilCraftCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(Reference.MOD_ID));
    }

    @Override
    protected void buildRecipes() {
        // poison
        new CentrifugeFluidRecipeBuilder(registries)
            .input(itemTag(Reference.MOD_ID, "poisonous"))
            .fluidInput(Fluids.WATER, 0.25f)
            .fluidOutput(RegistryEntries.FLUID_POISON.get(), 0.25f)
            .export(this.modLoadedOutput, PATH, "poison", Oritech.MOD_ID);

        // blood from leaves
        new CentrifugeFluidRecipeBuilder(registries)
            .input(RegistryEntries.BLOCK_UNDEAD_LEAVES.get())
            .fluidInput(Fluids.WATER, 0.1f)
            .fluidOutput(RegistryEntries.FLUID_BLOOD.get(), 0.05f)
            .result(RegistryEntries.ITEM_HARDENED_BLOOD_SHARD.get())
            .export(this.modLoadedOutput, PATH, "blood_from_leaves", Oritech.MOD_ID);

        // crushing gem
        new PulverizerRecipeBuilder(registries)
            .input(RegistryEntries.ITEM_DARK_GEM.get()).result(RegistryEntries.ITEM_DARK_GEM_CRUSHED.get())
            .addToGrinder().export(this.modLoadedOutput, PATH, "crushed_dark_gem", Oritech.MOD_ID);
        
        // dark ore processing
        new PulverizerRecipeBuilder(registries)
            .input(itemTag(Reference.MOD_ID, "dark_ores")).result(RegistryEntries.ITEM_DARK_GEM.get(), 2)
            .result(RegistryEntries.ITEM_DARK_GEM_CRUSHED.get())
            .addToGrinder().export(this.modLoadedOutput, PATH, "dark_ores", Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new EvilCraftCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "EvilCraft Oritech Compat";
        }
    }
    
}
