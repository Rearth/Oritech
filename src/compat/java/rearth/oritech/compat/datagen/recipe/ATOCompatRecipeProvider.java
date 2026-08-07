package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import net.allthemods.alltheores.api.ATO;
import net.allthemods.alltheores.common.material.Material;
import net.allthemods.alltheores.common.parts.BlockPartType;
import net.allthemods.alltheores.common.parts.ItemPartType;
import net.allthemods.alltheores.core.registry.Materials;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import rearth.oritech.Oritech;
import rearth.oritech.compat.datagen.resolver.ATOMaterialResolver;
import rearth.oritech.datagen.builders.FoundryRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.datagen.builders.util.MetalProcessingChainBuilder;

public class ATOCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/ato";
    private final RecipeOutput modLoadedOutput;
    private final ATOMaterialResolver resolver;
    
    public ATOCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(ATO.MOD_ID));
        this.resolver = new ATOMaterialResolver(this.items);
    }

    @Override
    protected void buildRecipes() {
        addMetalProcessing(Materials.ALUMINUM, Materials.IRON);
        addMetalProcessing(Materials.LEAD, Materials.GOLD);
        // addMetalProcessing(Materials.NICKEL, Materials.IRON);
        addMetalProcessing(Materials.OSMIUM, Materials.NICKEL);
        // addMetalProcessing(Materials.PLATINUM, Materials.URANIUM);
        addMetalProcessing(Materials.SILVER, Materials.COPPER);
        addMetalProcessing(Materials.TIN, Materials.COPPER);
        // addMetalProcessing(Materials.URANIUM, Materials.PLATINUM);
        addMetalProcessing(Materials.ZINC, Materials.COPPER);
        addMetalProcessing(Materials.IRIDIUM, Materials.NICKEL);

        // Output 1 for basic copper alloys, and 2 for more advanced alloys
        addFoundryAlloying(Materials.COPPER, Materials.ZINC, Materials.BRASS);
        addFoundryAlloying(Materials.COPPER, Materials.TIN, Materials.BRONZE);
        addFoundryAlloying(Materials.IRON, Materials.NICKEL, Materials.INVAR, 2);
        addFoundryAlloying(Materials.COPPER, Materials.NICKEL, Materials.CONSTANTAN, 2);

        oreToGem(Materials.CINNABAR);
        gemToDust(Materials.CINNABAR);
        oreToGem(Materials.FLUORITE);
        gemToDust(Materials.FLUORITE);
        oreToGem(Materials.PERIDOT);
        gemToDust(Materials.PERIDOT);
        oreToGem(Materials.RUBY);
        gemToDust(Materials.RUBY);
        oreToGem(Materials.SAPPHIRE);
        gemToDust(Materials.SAPPHIRE);

        oreToDust(Materials.SALT);
        oreToDust(Materials.SULFUR);

        gemToDust(Materials.DIAMOND);
    }

    private void addMetalProcessing(Material material, Material secondary) {
        new MetalProcessingChainBuilder(material.getGroup(), this.registries)
            .ore(resolver.ingredient(material, BlockPartType.STONE_ORE))
            .rawOre(resolver.ingredient(material, ItemPartType.RAW),
                resolver.item(material, ItemPartType.RAW))
            .rawOreByproduct(resolver.item(secondary, ItemPartType.RAW))
            .ingot(resolver.ingredient(material, ItemPartType.INGOT),
                resolver.item(material, ItemPartType.INGOT))
            .nugget(resolver.item(material, ItemPartType.NUGGET))
            .dust(resolver.item(material, ItemPartType.DUST))
            .dustByproduct(resolver.item(secondary, ItemPartType.NUGGET))
            .byproductAmount(6)
            .prefix(PATH).export(this.modLoadedOutput);
    }

    private void addFoundryAlloying(Material materialA, Material materialB, Material output) {
        addFoundryAlloying(materialA, materialB, output, 1);
    }

    private void addFoundryAlloying(Material materialA, Material materialB, Material output, int outputCount) {
        new FoundryRecipeBuilder(this.registries)
            .input(resolver.ingredient(materialA, ItemPartType.INGOT))
            .input(resolver.ingredient(materialB, ItemPartType.INGOT))
            .result(new ItemStackTemplate(output.get(ItemPartType.INGOT).getHolder(), outputCount))
            .export(this.modLoadedOutput, PATH, output.getGroup(), Oritech.MOD_ID);
    }

    private void oreToGem(Material material) {
        new PulverizerRecipeBuilder(this.registries)
            .input(TagKey.create(Registries.ITEM, material.get(BlockPartType.STONE_ORE).getTag().location()))
            .result(new ItemStackTemplate(material.get(ItemPartType.GEM).getHolder().get(), 2))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "gem/" + material.getGroup(), Oritech.MOD_ID);
    }

    private void gemToDust(Material material) {
        new PulverizerRecipeBuilder(this.registries)
            .input(TagKey.create(Registries.ITEM, material.get(ItemPartType.GEM).getTag().location()))
            .result(new ItemStackTemplate(material.get(ItemPartType.DUST).getHolder().get()))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "dust/" + material.getGroup(), Oritech.MOD_ID);
    }

    private void oreToDust(Material material) {
        new PulverizerRecipeBuilder(this.registries)
            .input(TagKey.create(Registries.ITEM, material.get(BlockPartType.STONE_ORE).getTag().location()))
            .result(new ItemStackTemplate(material.get(ItemPartType.DUST).getHolder().get()))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "dust/" + material.getGroup(), Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ATOCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "ATO Oritech Compat";
        }
    }
    
}
