package rearth.oritech.compat.datagen.recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.itemTag;

import cy.jdkdigital.productivemetalworks.ProductiveMetalworks;
import cy.jdkdigital.productivemetalworks.datagen.recipe.BlockCastingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.FluidAlloyingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.ItemCastingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.ItemMeltingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.registry.MetalworksRegistrator;
import cy.jdkdigital.productivemetalworks.util.FluidStackTemplate;
import dev.ftb.mods.ftbmaterials.FTBMaterials;
import net.allthemods.alltheores.AllTheOres;
import net.allthemods.alltheores.api.ATO;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.CentrifugeFluidRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;

public class ProductiveMetalworksCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/productivemetalworks";
    private final RecipeOutput modLoadedOutput;
    
    public ProductiveMetalworksCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(ProductiveMetalworks.MODID));
    }

    @Override
    protected void buildRecipes() {
        addMelting("copper", ItemContent.COPPER_GEM, MetalworksRegistrator.MOLTEN_COPPER.get());
        addMelting("iron", ItemContent.IRON_GEM, MetalworksRegistrator.MOLTEN_IRON.get());
        addMelting("gold", ItemContent.GOLD_GEM, MetalworksRegistrator.MOLTEN_GOLD.get());
        addMelting("nickel", ItemContent.NICKEL_GEM, MetalworksRegistrator.MOLTEN_NICKEL.get());
        addMelting("platinum", ItemContent.PLATINUM_GEM, MetalworksRegistrator.MOLTEN_PLATINUM.get());
        
        addCasting("nickel", MetalworksRegistrator.MOLTEN_NICKEL.get(), BlockContent.NICKEL, ItemContent.NICKEL_INGOT, ItemContent.NICKEL_NUGGET);
        addCasting("platinum", MetalworksRegistrator.MOLTEN_PLATINUM.get(), BlockContent.PLATINUM, ItemContent.PLATINUM_INGOT, ItemContent.PLATINUM_NUGGET);
    }

    // metal "gem" melting - 1 gem = 90mb of molten metal
    private void addMelting(String name, ItemLike input, Fluid fluid) {
        ItemMeltingRecipeBuilder.of(Ingredient.of(input), new FluidStackTemplate(fluid, 90))
            .save(this.modLoadedOutput, Oritech.id(PATH + "/melting/" + name + "_gem"));
    }

    private void addCasting(String name, Fluid fluid, ItemLike block, ItemLike ingot, ItemLike nugget) {
        var noMaterialLibraryOutput = this.modLoadedOutput.withConditions(
            new NotCondition(new OrCondition(List.of(new ModLoadedCondition(ATO.MOD_ID), new ModLoadedCondition(FTBMaterials.MOD_ID)))));
        BlockCastingRecipeBuilder.of(
            new SizedFluidIngredient(FluidIngredient.of(fluid), 810),
            new ItemStackTemplate(block.asItem()))
            .save(noMaterialLibraryOutput, Oritech.id(PATH + "/casting/block/" + name));
        ItemCastingRecipeBuilder.of(
            Ingredient.of(MetalworksRegistrator.CAST_INGOT.get()),
            new SizedFluidIngredient(FluidIngredient.of(fluid), 90),
            new ItemStackTemplate(ingot.asItem()), false)
            .save(noMaterialLibraryOutput, Oritech.id(PATH + "/casting/ingot/" + name));
        ItemCastingRecipeBuilder.of(
            Ingredient.of(MetalworksRegistrator.CAST_NUGGET.get()),
            new SizedFluidIngredient(FluidIngredient.of(fluid), 10),
            new ItemStackTemplate(nugget.asItem()), false)
            .save(noMaterialLibraryOutput, Oritech.id(PATH + "/casting/nugget/" + name));
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ProductiveMetalworksCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "Productive Metalworks Oritech Compat";
        }
    }
}
