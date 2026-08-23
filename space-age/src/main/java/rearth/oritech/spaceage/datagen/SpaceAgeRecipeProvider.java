package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.concurrent.CompletableFuture;

public class SpaceAgeRecipeProvider extends RecipeProvider {

    public SpaceAgeRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        shaped(net.minecraft.data.recipes.RecipeCategory.MISC, SpaceAgeBlocks.ROCKET_ASSEMBLER)
                .pattern("sas")
                .pattern("mcm")
                .pattern("sfs")
                .define('s', ItemContent.STEEL_INGOT)
                .define('a', ItemContent.ADVANCED_COMPUTING_ENGINE)
                .define('m', ItemContent.MOTOR)
                .define('c', BlockContent.ASSEMBLER)
                .define('f', BlockContent.MACHINE_FRAME)
                .unlockedBy("has_assembler", has(BlockContent.ASSEMBLER))
                .save(output);

        shaped(net.minecraft.data.recipes.RecipeCategory.MISC, SpaceAgeBlocks.ROCKET_PAD, 4)
                .pattern("sss")
                .pattern("ipi")
                .define('s', ItemContent.STEEL_INGOT)
                .define('i', Items.IRON_BLOCK)
                .define('p', BlockContent.IRON_PLATING)
                .unlockedBy("has_steel", has(ItemContent.STEEL_INGOT))
                .save(output);

        shaped(net.minecraft.data.recipes.RecipeCategory.MISC, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET)
                .pattern("sms")
                .pattern("pep")
                .pattern(" s ")
                .define('s', ItemContent.STEEL_INGOT)
                .define('m', ItemContent.MOTOR)
                .define('p', BlockContent.ENERGY_PIPE)
                .define('e', ItemContent.ADVANCED_BATTERY)
                .unlockedBy("has_motor", has(ItemContent.MOTOR))
                .save(output);

        shaped(net.minecraft.data.recipes.RecipeCategory.MISC, SpaceAgeBlocks.ION_BOOSTER_ROCKET)
                .pattern("ded")
                .pattern("ete")
                .pattern("ded")
                .define('d', ItemContent.DURATIUM_INGOT)
                .define('e', ItemContent.ENERGITE_INGOT)
                .define('t', SpaceAgeBlocks.BASIC_BOOSTER_ROCKET)
                .unlockedBy("has_basic_booster_rocket", has(SpaceAgeBlocks.BASIC_BOOSTER_ROCKET))
                .save(output);
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new SpaceAgeRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Oritech: Space Age Recipes";
        }
    }
}
