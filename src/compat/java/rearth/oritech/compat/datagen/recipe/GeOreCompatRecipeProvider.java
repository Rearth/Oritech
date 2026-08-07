package rearth.oritech.compat.datagen.recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.shynieke.geore.Reference;
import com.shynieke.geore.registry.GeOreBlockReg;
import com.shynieke.geore.registry.GeOreRegistry;
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
import rearth.oritech.datagen.builders.AssemblerRecipeBuilder;
import rearth.oritech.datagen.builders.CentrifugeRecipeBuilder;
import rearth.oritech.datagen.builders.LaserRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class GeOreCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/geore";
    private final RecipeOutput modLoadedOutput;
    
    public GeOreCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(Reference.MOD_ID));
    }

    @Override
    protected void buildRecipes() {
        for (GeOreBlockReg geOreBlock : GeOreRegistry.getGeOres()) {
            new AssemblerRecipeBuilder(registries)
                .input(geOreBlock.getShard().get())
                .input(geOreBlock.getShard().get())
                .input(ItemContent.ENDERIC_COMPOUND)
                .input(ItemContent.OVERCHARGED_CRYSTAL)
                .result(geOreBlock.getBudding().get().asItem())
                .export(this.modLoadedOutput, PATH, "budding_" + geOreBlock.getName(), Oritech.MOD_ID);
        }

        // enderic laser should yield plutonium dust when harvesting uranium clusters
        new LaserRecipeBuilder(registries)
            .input(GeOreRegistry.URANIUM_GEORE.getCluster().get()).result(ItemContent.PLUTONIUM_DUST)
            .export(this.modLoadedOutput, PATH, "plutonium_dust", Oritech.MOD_ID);

        // pulverize quartz shards into quartz dust, no need for intermediate smelting
        new PulverizerRecipeBuilder(registries)
            .input(GeOreRegistry.QUARTZ_GEORE.getShard().get()).result(ItemContent.QUARTZ_DUST)
            .addToGrinder().export(this.modLoadedOutput, PATH, "quartz_dust", Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new GeOreCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "GeOre Oritech Compat";
        }
    }
    
}
