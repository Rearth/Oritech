package rearth.oritech.fabricgen.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import me.jddev0.ep.api.EPAPI;
import net.emilsg.clutter.Clutter;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.impl.resource.conditions.conditions.AllModsLoadedResourceCondition;
import net.fabricmc.fabric.impl.resource.conditions.conditions.TagsPopulatedResourceCondition;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryWrapper;
import nourl.mythicmetals.MythicMetals;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.OritechRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.AlloyForgeryRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.ClutterRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.EnergizedPowerRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.MythicMetalsRecipeGenerator;
import rearth.oritech.fabricgen.datagen.compat.TechRebornRecipeGenerator;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import techreborn.TechReborn;
import wraith.alloyforgery.AlloyForgery;

public class RecipeGenerator extends FabricRecipeProvider {
    private final FabricDataOutput output;
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;
    
    public RecipeGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
        this.output = output;
        this.registriesFuture = registriesFuture;
    }
    
    @Override
    public void generate(RecipeExporter exporter) {
        var oritechRecipes = new OritechRecipeGenerator(output, registriesFuture);
        oritechRecipes.generate(exporter);
        
        // Fabric mod compat generation
        AlloyForgeryRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(AlloyForgery.MOD_ID))));
        ClutterRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(Clutter.MOD_ID))));
        EnergizedPowerRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(EPAPI.MOD_ID))));
        MythicMetalsRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(MythicMetals.MOD_ID))));
        TechRebornRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(TechReborn.MOD_ID))));
    }
}
