package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import dev.ftb.mods.ftbmaterials.FTBMaterials;
import dev.ftb.mods.ftbmaterials.resources.Resource;
import dev.ftb.mods.ftbmaterials.resources.ResourceType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import rearth.oritech.Oritech;
import rearth.oritech.compat.datagen.resolver.FTBMaterialResolver;
import rearth.oritech.datagen.builders.FoundryRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.datagen.builders.util.MetalProcessingChainBuilder;

public class FTBMaterialsCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/ftb";
    
    private final RecipeOutput modLoadedOutput;
    private final FTBMaterialResolver resolver;
    
    public FTBMaterialsCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(FTBMaterials.MOD_ID));
        this.resolver = new FTBMaterialResolver(this.items);
    }

    @Override
    protected void buildRecipes() {
        addMetalProcessing(Resource.ALUMINUM, Resource.IRON);
        addMetalProcessing(Resource.LEAD, Resource.GOLD);
        // addMetalProcessing(Resource.NICKEL, Resource.IRON); // native Oritech processing
        addMetalProcessing(Resource.OSMIUM, Resource.NICKEL);
        // addMetalProcessing(Resource.PLATINUM, Resource.URANIUM); // native Oritech processing
        addMetalProcessing(Resource.SILVER, Resource.COPPER);
        addMetalProcessing(Resource.TIN, Resource.COPPER);
        // addMetalProcessing(Resource.URANIUM, Resource.PLUTONIUM); // native Oritech processing
        addMetalProcessing(Resource.ZINC, Resource.COPPER);
        addMetalProcessing(Resource.ANTIMONY, Resource.GOLD);
        addMetalProcessing(Resource.IRIDIUM, Resource.NICKEL);
        // A little cycle here should be fine. Processing uranium gives
        // a bit of extra plutonium, and processing plutonium gives a bit
        // of extra uranium. It's like a little boost, not like an infinite
        // resource glitch.
        addMetalProcessing(Resource.PLUTONIUM, Resource.URANIUM);
        addMetalProcessing(Resource.TITANIUM, Resource.IRON);
        addMetalProcessing(Resource.TUNGSTEN, Resource.COPPER);

        // // Output 1 for basic copper alloys, and 2 for more advanced alloys
        addFoundryAlloying(Resource.COPPER, Resource.ZINC, Resource.BRASS);
        addFoundryAlloying(Resource.COPPER, Resource.TIN, Resource.BRONZE);
        addFoundryAlloying(Resource.IRON, Resource.NICKEL, Resource.INVAR, 2);
        addFoundryAlloying(Resource.COPPER, Resource.NICKEL, Resource.CONSTANTAN, 2);

        oreToGem(Resource.RESONATING_ORE);
        oreToGem(Resource.DIMENSIONAL_SHARD);
        
        oreToDust(Resource.BAUXITE);
        oreToDust(Resource.MONAZITE);

        oreToGem(Resource.CINNABAR);
        gemToDust(Resource.CINNABAR);
        oreToGem(Resource.NITER);
        gemToDust(Resource.NITER);
        oreToGem(Resource.RUBY);
        gemToDust(Resource.RUBY);
        oreToGem(Resource.SALT);
        gemToDust(Resource.SALT);
        oreToGem(Resource.SAPPHIRE);
        gemToDust(Resource.SAPPHIRE);

        gemToDust(Resource.DIAMOND);
        gemToDust(Resource.EMERALD);
    }

    private void addMetalProcessing(Resource resource, Resource secondary) {        
        new MetalProcessingChainBuilder(resource.name().toLowerCase(), this.registries)
            .ore(resolver.ingredient(resource, ResourceType.STONE_ORE))
            .rawOre(resolver.item(resource, ResourceType.RAW_ORE))
            .rawOreByproduct(resolver.item(secondary, ResourceType.RAW_ORE))
            .ingot(resolver.ingredient(resource, ResourceType.INGOT), resolver.item(resource, ResourceType.INGOT))
            .nugget(resolver.item(resource, ResourceType.NUGGET))
            .clump(resolver.item(resource, ResourceType.CLUMP))
            .clumpByproduct(resolver.item(secondary, ResourceType.TINY_DUST))
            .centrifugeResult(resolver.item(resource, ResourceType.DUST))
            .dust(resolver.item(resource, ResourceType.DUST))            
            .dustByproduct(resolver.item(secondary, ResourceType.TINY_DUST))
            .smallDust(resolver.item(resource, ResourceType.TINY_DUST))
            .prefix(PATH).export(this.modLoadedOutput);
    }

    private void addFoundryAlloying(Resource resourceA, Resource resourceB, Resource result) {
        addFoundryAlloying(resourceA, resourceB, result, 1);
    }

    private void addFoundryAlloying(Resource resourceA, Resource resourceB, Resource result, int resultCount) {
        new FoundryRecipeBuilder(this.registries)
            .input(resolver.ingredient(resourceA, ResourceType.INGOT))
            .input(resolver.ingredient(resourceA, ResourceType.INGOT))
            .result(new ItemStackTemplate(resolver.item(result, ResourceType.INGOT), resultCount))
            .export(this.modLoadedOutput, PATH, result.name().toLowerCase(), Oritech.MOD_ID);
    }

    private void oreToGem(Resource resource) {
        new PulverizerRecipeBuilder(this.registries)
            .input(resolver.ingredient(resource, ResourceType.STONE_ORE))
            .result(resolver.stack(resource, ResourceType.GEM, 2))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "gem/" + resource.name().toLowerCase(), Oritech.MOD_ID);
    }

    private void gemToDust(Resource resource) {
        new PulverizerRecipeBuilder(this.registries)
            .input(resolver.ingredient(resource, ResourceType.GEM))
            .result(resolver.stack(resource, ResourceType.DUST))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "dust/" + resource.name().toLowerCase(), Oritech.MOD_ID);
    }

    private void oreToDust(Resource resource) {
        new PulverizerRecipeBuilder(this.registries)
            .input(resolver.ingredient(resource, ResourceType.STONE_ORE))
            .result(resolver.stack(resource, ResourceType.DUST))
            .addToGrinder()
            .export(this.modLoadedOutput, PATH, "dust/" + resource.name().toLowerCase(), Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new FTBMaterialsCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "FTB Materials Oritech Compat";
        }
    }
    
}
