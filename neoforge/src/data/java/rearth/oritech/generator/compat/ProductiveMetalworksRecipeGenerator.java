package rearth.oritech.generator.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.of;

import java.util.List;

import cy.jdkdigital.productivemetalworks.datagen.recipe.BlockCastingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.FluidAlloyingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.ItemCastingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.datagen.recipe.ItemMeltingRecipeBuilder;
import cy.jdkdigital.productivemetalworks.registry.MetalworksRegistrator;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class ProductiveMetalworksRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) {
        addItemMeltingRecipes(exporter);
        addFluidAlloyingRecipes(exporter);
        addCastingRecipes(exporter);
    }

    private static void addItemMeltingRecipes(RecipeOutput exporter) {
        // gem/clump/dust for vanilla ores
        ItemMeltingRecipeBuilder.of(of(ItemContent.COPPER_GEM), new FluidStack(MetalworksRegistrator.MOLTEN_COPPER.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/copper"));
        ItemMeltingRecipeBuilder.of(of(TagContent.COPPER_CLUMPS), new FluidStack(MetalworksRegistrator.MOLTEN_COPPER.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/clump/copper"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_COPPER_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_COPPER.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smallclump/copper"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_COPPER_DUST), new FluidStack(MetalworksRegistrator.MOLTEN_COPPER.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smalldust/copper"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.IRON_GEM), new FluidStack(MetalworksRegistrator.MOLTEN_IRON.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/iron"));
        ItemMeltingRecipeBuilder.of(of(TagContent.IRON_CLUMPS), new FluidStack(MetalworksRegistrator.MOLTEN_IRON.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/clump/iron"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_IRON_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_IRON.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smallclump/iron"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_IRON_DUST), new FluidStack(MetalworksRegistrator.MOLTEN_IRON.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smalldust/iron"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.GOLD_GEM), new FluidStack(MetalworksRegistrator.MOLTEN_GOLD.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/gold"));
        ItemMeltingRecipeBuilder.of(of(TagContent.GOLD_CLUMPS), new FluidStack(MetalworksRegistrator.MOLTEN_GOLD.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/clump/gold"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_GOLD_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_GOLD.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smallclump/gold"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_GOLD_DUST), new FluidStack(MetalworksRegistrator.MOLTEN_GOLD.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smalldust/gold"));

        // gem/clump/dust for nickel
        ItemMeltingRecipeBuilder.of(of(ItemContent.NICKEL_GEM), new FluidStack(MetalworksRegistrator.MOLTEN_NICKEL.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/nickel"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.NICKEL_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_NICKEL.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/clump/nickel"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_NICKEL_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_NICKEL.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smallclump/nickel"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_NICKEL_DUST), new FluidStack(MetalworksRegistrator.MOLTEN_NICKEL.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smalldust/nickel"));

        // gem/clump/dust for platinum
        ItemMeltingRecipeBuilder.of(of(ItemContent.PLATINUM_GEM), new FluidStack(MetalworksRegistrator.MOLTEN_PLATINUM.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/platinum"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.PLATINUM_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_PLATINUM.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/clump/platinum"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_PLATINUM_CLUMP), new FluidStack(MetalworksRegistrator.MOLTEN_PLATINUM.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smallclump/platinum"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.SMALL_PLATINUM_DUST), new FluidStack(MetalworksRegistrator.MOLTEN_PLATINUM.get(), 10)).save(exporter, Oritech.id("compat/productivemetalworks/melting/smalldust/platinum"));

        // adamant
        ItemMeltingRecipeBuilder.of(of(ItemContent.ADAMANT_INGOT), new FluidStack(FluidContent.STILL_MOLTEN_ADAMANT.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/ingot/adamant"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.ADAMANT_DUST), new FluidStack(FluidContent.STILL_MOLTEN_ADAMANT.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/dust/adamant"));
        ItemMeltingRecipeBuilder.of(of(BlockContent.ADAMANT_BLOCK.asItem()), new FluidStack(FluidContent.STILL_MOLTEN_ADAMANT.get(), 810)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/adamant"));
        
        // biosteel
        ItemMeltingRecipeBuilder.of(of(ItemContent.BIOSTEEL_INGOT), new FluidStack(FluidContent.STILL_MOLTEN_BIOSTEEL.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/ingot/biosteel"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.BIOSTEEL_DUST), new FluidStack(FluidContent.STILL_MOLTEN_BIOSTEEL.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/dust/biosteel"));
        ItemMeltingRecipeBuilder.of(of(BlockContent.BIOSTEEL_BLOCK.asItem()), new FluidStack(FluidContent.STILL_MOLTEN_BIOSTEEL.get(), 810)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/biosteel"));

        // duratium
        ItemMeltingRecipeBuilder.of(of(ItemContent.DURATIUM_INGOT), new FluidStack(FluidContent.STILL_MOLTEN_DURATIUM.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/ingot/duratium"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.DURATIUM_DUST), new FluidStack(FluidContent.STILL_MOLTEN_DURATIUM.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/dust/duratium"));
        ItemMeltingRecipeBuilder.of(of(BlockContent.DURATIUM_BLOCK.asItem()), new FluidStack(FluidContent.STILL_MOLTEN_DURATIUM.get(), 810)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/duratium"));

        // energite
        ItemMeltingRecipeBuilder.of(of(ItemContent.ENERGITE_INGOT), new FluidStack(FluidContent.STILL_MOLTEN_ENERGITE.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/ingot/energite"));
        ItemMeltingRecipeBuilder.of(of(ItemContent.ENERGITE_DUST), new FluidStack(FluidContent.STILL_MOLTEN_ENERGITE.get(), 90)).save(exporter, Oritech.id("compat/productivemetalworks/melting/dust/energite"));
        ItemMeltingRecipeBuilder.of(of(BlockContent.ENERGITE_BLOCK.asItem()), new FluidStack(FluidContent.STILL_MOLTEN_ENERGITE.get(), 810)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/energite"));

        // fluxite
        ItemMeltingRecipeBuilder.of(of(ItemContent.FLUXITE), new FluidStack(FluidContent.STILL_MOLTEN_FLUXITE.get(), 100)).save(exporter, Oritech.id("compat/productivemetalworks/melting/gem/fluxite"));
        ItemMeltingRecipeBuilder.of(of(BlockContent.FLUXITE_BLOCK.asItem()), new FluidStack(FluidContent.STILL_MOLTEN_FLUXITE.get(), 1000)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/fluxite"));

        // biomass
        ItemMeltingRecipeBuilder.of(of(TagContent.BIOMASS), new FluidStack(FluidContent.STILL_BIOFUEL.get(), 100)).save(exporter, Oritech.id("compat/productivemetalworks/melting/glob/bio"));
        ItemMeltingRecipeBuilder.of(of(TagContent.BIOMASS_BLOCK), new FluidStack(FluidContent.STILL_BIOFUEL.get(), 1000)).save(exporter, Oritech.id("compat/productivemetalworks/melting/block/bio"));
    }

    private static void addFluidAlloyingRecipes(RecipeOutput exporter) {
        FluidAlloyingRecipeBuilder.of(
                List.of(
                    new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_DIAMOND.get()), 1),
                    new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 1)),
                10, new FluidStack(FluidContent.STILL_MOLTEN_ADAMANT.get(), 1))
            .save(exporter, Oritech.id("compat/productivemetalworks/alloying/adamant"));
        FluidAlloyingRecipeBuilder.of(
                List.of(
                    new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_BIOFUEL.get()), 1),
                    new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_IRON.get()), 1)),
                10, new FluidStack(FluidContent.STILL_MOLTEN_BIOSTEEL.get(), 1))
            .save(exporter, Oritech.id("compat/productivemetalworks/alloying/biosteel"));
        FluidAlloyingRecipeBuilder.of(
            List.of(
                new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NETHERITE.get()), 1),
                new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_PLATINUM.get()), 1)),
            10, new FluidStack(FluidContent.STILL_MOLTEN_DURATIUM.get(), 1))
        .save(exporter, Oritech.id("compat/productivemetalworks/alloying/duratium"));
        FluidAlloyingRecipeBuilder.of(
            List.of(
                new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_REDSTONE.get()), 1),
                new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 1)),
            10, new FluidStack(MetalworksRegistrator.MOLTEN_ELECTRUM.get(), 1))
        .save(exporter, Oritech.id("compat/productivemetalworks/alloying/electrum"));
        FluidAlloyingRecipeBuilder.of(
            List.of(
                new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_FLUXITE.get()), 1),
                new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 1)),
            10, new FluidStack(FluidContent.STILL_MOLTEN_ENERGITE.get(), 1))
        .save(exporter, Oritech.id("compat/productivemetalworks/alloying/energite"));
        FluidAlloyingRecipeBuilder.of(
            List.of(
                new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_BIOFUEL.get()), 1),
                new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_FLUXITE.get()), 1)),
            10, new FluidStack(FluidContent.STILL_FUEL.get(), 1))
        .save(exporter, Oritech.id("compat/productivemetalworks/alloying/turbofuel"));
    }

    private static void addCastingRecipes(RecipeOutput exporter) {
        // nickel
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_NUGGET.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 10), new ItemStack(ItemContent.NICKEL_NUGGET), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/nugget/nickel"));
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 90), new ItemStack(ItemContent.NICKEL_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/nickel"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 810), new ItemStack(BlockContent.NICKEL_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/nickel"));

        // platinum
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_NUGGET.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_PLATINUM.get()), 10), new ItemStack(ItemContent.PLATINUM_NUGGET), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/nugget/platinum"));
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_PLATINUM.get()), 90), new ItemStack(ItemContent.PLATINUM_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/platinum"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_NICKEL.get()), 810), new ItemStack(BlockContent.NICKEL_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/platinum"));
        
        // biosteel
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_BIOSTEEL.get()), 90), new ItemStack(ItemContent.BIOSTEEL_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/biosteel"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_BIOSTEEL.get()), 810), new ItemStack(BlockContent.BIOSTEEL_BLOCK.asItem()))
                                 .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/biosteel"));
        // steel
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_STEEL.get()), 90), new ItemStack(ItemContent.STEEL_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/steel"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_STEEL.get()), 810), new ItemStack(BlockContent.STEEL_BLOCK.asItem()))
                                 .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/steel"));
        
        // duratium
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_DURATIUM.get()), 90), new ItemStack(ItemContent.DURATIUM_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/duratium"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_DURATIUM.get()), 810), new ItemStack(BlockContent.DURATIUM_BLOCK.asItem()))
                                 .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/duratium"));

        // electrum
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_ELECTRUM.get()), 90), new ItemStack(ItemContent.ELECTRUM_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/electrum"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(MetalworksRegistrator.MOLTEN_ELECTRUM.get()), 810), new ItemStack(BlockContent.ELECTRUM_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/electrum"));
        
        // energite
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_INGOT.get()), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_ENERGITE.get()), 90), new ItemStack(ItemContent.ENERGITE_INGOT), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/ingot/energite"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_ENERGITE.get()), 810), new ItemStack(BlockContent.ENERGITE_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/energite"));
        
        // fluxite
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_GEM.get()), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_FLUXITE.get()), 100), new ItemStack(ItemContent.FLUXITE), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/gem/fluxite"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_MOLTEN_FLUXITE.get()), 1000), new ItemStack(BlockContent.FLUXITE_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/fluxite"));

        // biofuel
        ItemCastingRecipeBuilder.of(of(MetalworksRegistrator.CAST_GEM.get()), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_BIOFUEL.get()), 100), new ItemStack(ItemContent.BIOMASS), false)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/glob/biomass"));
        ItemCastingRecipeBuilder.of(of(Items.BUCKET), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_BIOFUEL.get()), 1000), new ItemStack(FluidContent.STILL_BIOFUEL_BUCKET), true)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/bucket/biofuel"));
        BlockCastingRecipeBuilder.of(new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_BIOFUEL.get()), 1000), new ItemStack(BlockContent.BIOMASS_BLOCK.asItem()))
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/block/biomass"));
        
        // turbofuel
        ItemCastingRecipeBuilder.of(of(Items.BUCKET), new SizedFluidIngredient(FluidIngredient.of(FluidContent.STILL_FUEL.get()), 1000), new ItemStack(FluidContent.STILL_FUEL_BUCKET), true)
                                .save(exporter, Oritech.id("compat/productivemetalworks/casting/bucket/turbofuel"));
    }
}
