package rearth.oritech.spaceage.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeItems;
import rearth.oritech.spaceage.recipe.BlockIngredient;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpaceAgeRecipeProvider extends RecipeProvider {

    public SpaceAgeRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {

        super(registries, output);
    }

    @Override
    protected void buildRecipes() {

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.VACUUM_CRAFTER)
                .pattern("sps").pattern("aca").pattern("sbs")
                .define('s', ItemContent.STEEL_INGOT).define('p', ItemContent.ADVANCED_COMPUTING_ENGINE)
                .define('a', ItemContent.ENERGITE_INGOT).define('c', BlockContent.ASSEMBLER).define('b', ItemContent.ADVANCED_BATTERY)
                .unlockedBy("has_energite", has(ItemContent.ENERGITE_INGOT)).save(output);
        shaped(RecipeCategory.MISC, SpaceAgeBlocks.CARGO)
                .pattern("sis").pattern("s s").pattern("sis").define('s', ItemContent.STEEL_INGOT).define('i', Items.IRON_INGOT)
                .unlockedBy("has_steel", has(ItemContent.STEEL_INGOT)).save(output);
        blockRecipe("duratium", BlockContent.STEEL.get(), Blocks.DIAMOND_BLOCK, BlockContent.DURATIUM.get(), 1200);
        blockRecipe("energite", Blocks.REDSTONE_BLOCK, Blocks.END_STONE, BlockContent.ENERGITE.get(), 2400);
        shaped(RecipeCategory.MISC, SpaceAgeBlocks.NAVIGATION_COMPUTER)
                .pattern("sss").pattern("pcp").pattern("sss")
                .define('s', ItemContent.STEEL_INGOT).define('p', ItemContent.PROCESSING_UNIT)
                .define('c', ItemContent.ADVANCED_COMPUTING_ENGINE)
                .unlockedBy("has_processor", has(ItemContent.PROCESSING_UNIT)).save(output);
        shaped(RecipeCategory.MISC, SpaceAgeBlocks.PARACHUTE)
                .pattern("www").pattern("t t").pattern("sis")
                .define('w', Items.WHITE_WOOL).define('t', Items.STRING)
                .define('s', ItemContent.STEEL_INGOT).define('i', Items.IRON_INGOT)
                .unlockedBy("has_steel", has(ItemContent.STEEL_INGOT)).save(output);
        shaped(RecipeCategory.MISC, SpaceAgeBlocks.ROCKET_COUPLING)
                .pattern("sis").pattern("pmp").pattern("sis")
                .define('s', ItemContent.STEEL_INGOT).define('i', Items.IRON_INGOT)
                .define('p', Items.PISTON).define('m', ItemContent.MOTOR)
                .unlockedBy("has_motor", has(ItemContent.MOTOR)).save(output);
        for (var block : List.of(SpaceAgeBlocks.MISSION_CONTROL, SpaceAgeBlocks.SPACE_SCANNER, SpaceAgeBlocks.ANTENNA)) {
            shaped(RecipeCategory.MISC, block)
                    .pattern("sss").pattern("pcp").pattern("sfs")
                    .define('s', ItemContent.STEEL_INGOT).define('p', ItemContent.PROCESSING_UNIT)
                    .define('c', block == SpaceAgeBlocks.ANTENNA ? Items.COPPER_INGOT : Items.ENDER_PEARL)
                    .define('f', BlockContent.MACHINE_FRAME)
                    .unlockedBy("has_processor", has(ItemContent.PROCESSING_UNIT)).save(output);
        }
        shaped(RecipeCategory.MISC, SpaceAgeItems.MISSION_CARD)
                .pattern("pp").pattern("sc").define('p', Items.PAPER).define('s', ItemContent.STEEL_INGOT)
                .define('c', ItemContent.PROCESSING_UNIT).unlockedBy("has_processor", has(ItemContent.PROCESSING_UNIT)).save(output);

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.ROCKET_ASSEMBLER)
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

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.ROCKET_PAD, 4)
                .pattern("sss")
                .pattern("ipi")
                .define('s', ItemContent.STEEL_INGOT)
                .define('i', Items.IRON_BLOCK)
                .define('p', BlockContent.IRON_PLATING)
                .unlockedBy("has_steel", has(ItemContent.STEEL_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.BASIC_BOOSTER_ROCKET)
                .pattern("sms")
                .pattern("pep")
                .pattern(" s ")
                .define('s', ItemContent.STEEL_INGOT)
                .define('m', ItemContent.MOTOR)
                .define('p', BlockContent.ENERGY_PIPE)
                .define('e', ItemContent.ADVANCED_BATTERY)
                .unlockedBy("has_motor", has(ItemContent.MOTOR))
                .save(output);

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.ASTEROID_ANCHOR)
                .pattern("sis")
                .pattern("ama")
                .pattern("sis")
                .define('s', ItemContent.STEEL_INGOT)
                .define('i', Items.IRON_BLOCK)
                .define('a', ItemContent.ADVANCED_COMPUTING_ENGINE)
                .define('m', ItemContent.MOTOR)
                .unlockedBy("has_advanced_computing_engine", has(ItemContent.ADVANCED_COMPUTING_ENGINE))
                .save(output);

        shaped(RecipeCategory.MISC, SpaceAgeBlocks.ION_BOOSTER_ROCKET)
                .pattern("ded")
                .pattern("ete")
                .pattern("ded")
                .define('d', ItemContent.DURATIUM_INGOT)
                .define('e', ItemContent.ENERGITE_INGOT)
                .define('t', SpaceAgeBlocks.BASIC_BOOSTER_ROCKET)
                .unlockedBy("has_basic_booster_rocket", has(SpaceAgeBlocks.BASIC_BOOSTER_ROCKET))
                .save(output);
    }

    private void blockRecipe(String name, Block first, Block second, Block result, int ticks) {

        output.accept(ResourceKey.create(Registries.RECIPE,
                OritechSpaceAge.id("vacuum/" + name)), new VacuumRecipe(
                BlockIngredient.of(first), BlockIngredient.of(second), result.defaultBlockState(), ticks, 4000), null);
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
