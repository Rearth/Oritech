package rearth.oritech.neoforgegen.datagen;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import appeng.api.ids.AEConstants;
import blusunrize.immersiveengineering.ImmersiveEngineering;
import com.buuz135.industrial.utils.Reference;
import com.enderio.core.EnderCore;
import com.simibubi.create.Create;
import cy.jdkdigital.productivemetalworks.ProductiveMetalworks;
import de.ellpeck.actuallyadditions.mod.ActuallyAdditions;
import me.desht.pneumaticcraft.api.lib.Names;
import me.jddev0.ep.api.EPAPI;
import mekanism.common.Mekanism;
import mekanism.generators.common.MekanismGenerators;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import owmii.powah.Powah;
import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.OritechRecipeGenerator;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.neoforgegen.datagen.compat.ActuallyAdditionsRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.AppliedEnergistics2RecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.CreateRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.EnderIORecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.ImmersiveEngineeringRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.IndustrialForegoingRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.MekanismRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.MekanismGeneratorsRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.PneumaticcraftRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.PowahRecipeGenerator;
import rearth.oritech.neoforgegen.datagen.compat.ProductiveMetalworksRecipeGenerator;

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
        var oritechRecipes = new OritechRecipeGenerator(packOutput, registries);
        // runs OritechRecipeGenerator.generate(), but it's named differently here due to mapping differences
        oritechRecipes.buildRecipes(exporter);

        // Uranium clumps don't exist in Oritech, but Oritech should still be able to do something with them if they're added by another mod (like Create).
        // Also added in Fabric datagen with Fabric load conditions, but the Fabric versions should be excluded from the Neoforge build
        CentrifugeRecipeBuilder.build().input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 2).result(ItemContent.SMALL_PLUTONIUM_DUST).timeMultiplier(0.5f).export(exporter.withConditions(this.not(this.tagEmpty(TagContent.URANIUM_CLUMPS))), "compat/clump/crushed_uranium");
        CentrifugeFluidRecipeBuilder.build().input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 3).fluidInput(Fluids.WATER).timeMultiplier(0.5f).export(exporter.withConditions(this.not(this.tagEmpty(TagContent.URANIUM_CLUMPS))), "compat/clumpwet/crushed_uranium");

        ActuallyAdditionsRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(ActuallyAdditions.MODID)));
        AppliedEnergistics2RecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(AEConstants.MOD_ID)));
        CreateRecipeGenerator.generateRecipes(this, packOutput, registries, exporter.withConditions(this.modLoaded(Create.ID)));
        EnderIORecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(EnderCore.MOD_ID)), this);
        EnergizedPowerRecipeGenerator.generateRecipes(this, exporter.withConditions(this.modLoaded(EPAPI.MOD_ID)));
        ImmersiveEngineeringRecipeGenerator.generateRecipes(this, exporter.withConditions(this.modLoaded(ImmersiveEngineering.MODID)));
        IndustrialForegoingRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(Reference.MOD_ID)));
        MekanismRecipeGenerator.generateRecipes(this, exporter.withConditions(this.modLoaded(Mekanism.MODID)));
        MekanismGeneratorsRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(MekanismGenerators.MODID)));
        PneumaticcraftRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(Names.MOD_ID)));
        PowahRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(Powah.MOD_ID)));
        ProductiveMetalworksRecipeGenerator.generateRecipes(exporter.withConditions(this.modLoaded(ProductiveMetalworks.MODID)));
    }
}
