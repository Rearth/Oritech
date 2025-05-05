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

        // Uranium clumps don't exist in Oritech, but Oritech should still be able to do something with them if they're added by another mod (like Create).
          // in the compat space so that the Fabric versions won't go into Neoforge
        CentrifugeRecipeBuilder.build()
        .input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 2).result(ItemContent.SMALL_PLUTONIUM_DUST).timeMultiplier(0.5f)
        .export(this.withConditions(exporter, new TagsPopulatedResourceCondition(TagContent.URANIUM_CLUMPS)), "compat/clump/crushed_uranium");
        CentrifugeFluidRecipeBuilder.build()
        .input(TagContent.URANIUM_CLUMPS).result(ItemContent.URANIUM_DUST, 3).fluidInput(Fluids.WATER).timeMultiplier(0.5f)
        .export(this.withConditions(exporter, new TagsPopulatedResourceCondition(TagContent.URANIUM_CLUMPS)), "compat/clumpwet/crushed_uranium");
        
        // Fabric mod compat generation
        AlloyForgeryRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(AlloyForgery.MOD_ID))));
        ClutterRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(Clutter.MOD_ID))));
        EnergizedPowerRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(EPAPI.MOD_ID))));
        MythicMetalsRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(MythicMetals.MOD_ID))));
        TechRebornRecipeGenerator.generateRecipes(this.withConditions(exporter, new AllModsLoadedResourceCondition(List.of(TechReborn.MOD_ID))));
    }
}
