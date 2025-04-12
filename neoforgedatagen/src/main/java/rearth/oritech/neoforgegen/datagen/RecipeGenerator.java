package rearth.oritech.neoforgegen.datagen;

import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.neoforgegen.datagen.compat.AlloySmelterRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.CreateRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.ImmersiveEngineeringRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.IndustrialForegoingRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.MekanismRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.MekanismGeneratorsRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.PowahRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.ProductiveMetalworksRecipeGenerator;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.conditions.WithConditions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

public class RecipeGenerator extends RecipeProvider implements IConditionBuilder {
    PackOutput packOutput;
    CompletableFuture<HolderLookup.Provider> registries;

    public RecipeGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);

        this.packOutput = output;
        this.registries = registries;
    }

    @Override
    protected void buildRecipes(RecipeOutput exporter) {
        // Not working yet. See https://github.com/TheFogIOF/AlloySmelter/issues/6
        // AlloySmelterRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("alloy_smelter")));
        CreateRecipeGenerator.generateRecipes(packOutput, registries, exporter.withConditions(this.modLoaded("create")));
        EnergizedPowerRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("energizedpower")));
        ImmersiveEngineeringRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("immersiveengineering")));
        IndustrialForegoingRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("industrialforegoing")));
        MekanismRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("mekanism")));
        MekanismGeneratorsRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("mekanismgenerators")));
        PowahRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("powah")));
        ProductiveMetalworksRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded("productivemetalworks")));

        // Uranium clumps don't exist in Oritech, but Oritech should still be able to do something with them if they're added by another mod (like Create).
        // Also added in Fabric datagen with Fabric load conditions, but the Fabric versions should be excluded from the Neoforge build
        CentrifugeRecipeBuilder.build().input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 2).result(ItemContent.SMALL_PLUTONIUM_DUST).timeMultiplier(0.5f).export(exporter.withConditions(this.not(this.tagEmpty(TagContent.URANIUM_CLUMPS))), "compat/clump/crushed_uranium");
        CentrifugeRecipeBuilder.build().input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 3).fluidInput(Fluids.WATER).timeMultiplier(0.5f).export(exporter.withConditions(this.not(this.tagEmpty(TagContent.URANIUM_CLUMPS))), "compat/clumpwet/crushed_uranium");
    }
}
