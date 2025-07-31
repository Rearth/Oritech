package rearth.oritech.generator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import me.jddev0.ep.api.EPAPI;
import net.emilsg.clutter.Clutter;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.impl.resource.conditions.conditions.AllModsLoadedResourceCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import nourl.mythicmetals.MythicMetals;
import rearth.oritech.api.recipe.OritechRecipeGenerator;
import rearth.oritech.generator.compat.AlloyForgeryRecipeGenerator;
import rearth.oritech.generator.compat.ClutterRecipeGenerator;
import rearth.oritech.generator.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.generator.compat.MythicMetalsRecipeGenerator;
import rearth.oritech.generator.compat.TechRebornRecipeGenerator;
import techreborn.TechReborn;
import wraith.alloyforgery.AlloyForgery;

public class RecipeGenerator extends FabricRecipeProvider {
    private final FabricDataOutput output;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;
    
    public RecipeGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.output = output;
        this.registriesFuture = registriesFuture;
    }
    
    @Override
    public void buildRecipes(RecipeOutput exporter) {
        var oritechRecipes = new OritechRecipeGenerator(output, registriesFuture);
        oritechRecipes.buildRecipes(exporter);
        
        // Fabric mod compat generation
        AlloyForgeryRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(AlloyForgery.MOD_ID))));
        ClutterRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(Clutter.MOD_ID))));
        EnergizedPowerRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(EPAPI.MOD_ID))));
        MythicMetalsRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(MythicMetals.MOD_ID))));
        TechRebornRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(TechReborn.MOD_ID))));
    }
}
