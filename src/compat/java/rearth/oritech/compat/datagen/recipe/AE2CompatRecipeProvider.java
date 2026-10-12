package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import appeng.api.ids.AEConstants;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.recipes.handlers.ChargerRecipe;
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
import rearth.oritech.datagen.builders.LaserRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

import static rearth.oritech.util.TagUtils.cItemTag;

public class AE2CompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/ae2";
    private final RecipeOutput modLoadedOutput;
    
    public AE2CompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(AEConstants.MOD_ID));
    }

    @Override
    protected void buildRecipes() {
        // enderic laser should yield charged certus crystals instead of regular certus crystals
        new LaserRecipeBuilder(registries).input(AEBlocks.QUARTZ_CLUSTER).result(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.get()).export(this.modLoadedOutput, PATH, "charged_quartz", Oritech.MOD_ID);

        new PulverizerRecipeBuilder(registries)
            .input(AEBlocks.SKY_STONE_BLOCK).result(AEItems.SKY_DUST.get())
            .addToGrinder().export(this.modLoadedOutput, PATH, "sky_dust", Oritech.MOD_ID);
        new PulverizerRecipeBuilder(registries)
            .input(cItemTag("gems/certus_quartz")).result(AEItems.CERTUS_QUARTZ_DUST.get())
            .addToGrinder().export(this.modLoadedOutput, PATH, "certus_dust", Oritech.MOD_ID);
        new PulverizerRecipeBuilder(registries)
            .input(cItemTag("gems/fluix")).result(AEItems.FLUIX_DUST.get())
            .addToGrinder().export(this.modLoadedOutput, PATH, "fluix_dust", Oritech.MOD_ID);

        // fluxite in AE2 charger
        this.modLoadedOutput.accept(ResourceKey.create(Registries.RECIPE, Oritech.id(PATH + "/charger/fluxite")),
            new ChargerRecipe(Ingredient.of(this.items.get(Tags.Items.GEMS_AMETHYST).get()), new ItemStackTemplate(ItemContent.FLUXITE)),
            null);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new AE2CompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "AE2 Oritech Compat";
        }
    }
    
}
