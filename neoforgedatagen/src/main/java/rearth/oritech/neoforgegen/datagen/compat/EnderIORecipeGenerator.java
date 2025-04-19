package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.createInsulatedCableRecipe;

import java.util.List;
import java.util.Optional;

import com.enderio.base.common.init.EIOItems;
import com.enderio.base.common.recipe.FireCraftingRecipe;
import com.enderio.machines.common.blocks.alloy.AlloySmeltingRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.neoforgegen.datagen.loot.FireCraftingLootProvider;

public class EnderIORecipeGenerator {
    private static final String PATH = "compat/enderio/";

    public static void generateRecipes(RecipeOutput exporter, RecipeProvider provider) {
        addAlloys(exporter);
        conduitBinderCrafting(exporter, provider);
        addFireCrafting(exporter);

        CentrifugeRecipeBuilder.build().input(EIOItems.POWDERED_ENDER_PEARL.get()).result(ItemContent.ENDERIC_COMPOUND, 2).export(exporter, PATH + "endericcompound");
    }

    private static void addAlloys(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_COPPER).input(TagContent.SILICON).result(EIOItems.COPPER_ALLOY_INGOT.get()).export(exporter, PATH + "copperalloy");
        FoundryRecipeBuilder.build().input(Tags.Items.DUSTS_REDSTONE).input(TagContent.SILICON).result(EIOItems.REDSTONE_ALLOY_INGOT.get()).export(exporter, PATH + "redstonealloy");
        FoundryRecipeBuilder.build().input(TagContent.ELECTRUM_INGOTS).input(Tags.Items.DUSTS_GLOWSTONE).result(EIOItems.ENERGETIC_ALLOY_INGOT.get()).export(exporter, PATH + "energeticalloy");
        FoundryRecipeBuilder.build().input(EIOItems.ENERGETIC_ALLOY_INGOT.get()).input(Tags.Items.ENDER_PEARLS).result(EIOItems.VIBRANT_ALLOY_INGOT.get()).export(exporter, PATH + "vibrantalloy");
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_IRON).input(Tags.Items.ENDER_PEARLS).result(EIOItems.PULSATING_ALLOY_INGOT.get()).export(exporter, PATH + "pulsatingalloy");
        FoundryRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(Tags.Items.OBSIDIANS_NORMAL).result(EIOItems.DARK_STEEL_INGOT.get()).export(exporter, PATH + "darksteel");
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_GOLD).input(ItemTags.SOUL_FIRE_BASE_BLOCKS).result(EIOItems.SOULARIUM_INGOT.get()).export(exporter, PATH + "soularium");

        exporter.accept(Oritech.id(PATH + "alloy/adamant"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.NICKEL_INGOTS), 1), new SizedIngredient(Ingredient.of(Tags.Items.GEMS_DIAMOND), 1)), new ItemStack(ItemContent.ADAMANT_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/steel"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_IRON), 1), new SizedIngredient(Ingredient.of(TagContent.COAL_DUSTS), 1)), new ItemStack(ItemContent.STEEL_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/biosteel"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_IRON), 1), new SizedIngredient(Ingredient.of(ItemContent.RAW_BIOPOLYMER), 1)), new ItemStack(ItemContent.BIOSTEEL_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/duratium"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.PLATINUM_INGOTS), 1), new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_NETHERITE), 1)), new ItemStack(ItemContent.DURATIUM_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/energite"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.NICKEL_INGOTS), 1), new SizedIngredient(Ingredient.of(ItemContent.FLUXITE), 1)), new ItemStack(ItemContent.ENERGITE_INGOT), 3200, 0.3f), null);
    }

    private static void conduitBinderCrafting(RecipeOutput exporter, RecipeProvider provider) {
        var conduitBinder = EIOItems.CONDUIT_BINDER.get();
        createInsulatedCableRecipe(RecipeCategory.MISC,
                BlockContent.FLUID_PIPE.asItem(), 6,
                Ingredient.of(conduitBinder),
                Ingredient.of(Tags.Items.INGOTS_COPPER))
            .unlockedBy(provider.getHasName(conduitBinder), provider.has(conduitBinder))
            .save(exporter, Oritech.id(PATH + "crafting/fluidpipe"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockContent.PUMP_BLOCK.asItem(), 1)
            .define('s', Ingredient.of(conduitBinder))
            .define('c', Ingredient.of(ItemContent.MOTOR))
            .define('f', Ingredient.of(Tags.Items.INGOTS_COPPER))
            .define('b', Ingredient.of(TagContent.MACHINE_PLATING))
            .pattern("fff")
            .pattern("fcf")
            .pattern("sbs")
            .unlockedBy(provider.getHasName(conduitBinder), provider.has(conduitBinder)).save(exporter, PATH + "crafting/pump");
    }

    private static void addFireCrafting(RecipeOutput exporter) {
        exporter.accept(Oritech.id(PATH + "firecrafting/sculk"), new FireCraftingRecipe(FireCraftingLootProvider.SCULK_CRAFTING, 2, List.of(Blocks.SCULK), List.of(), List.of(Level.OVERWORLD), Optional.of(Blocks.AIR)), null);
        exporter.accept(Oritech.id(PATH + "firecrafting/endstone"), new FireCraftingRecipe(FireCraftingLootProvider.SCULK_CRAFTING, 1, List.of(Blocks.END_STONE), List.of(), List.of(Level.END), Optional.of(Blocks.BLACKSTONE)), null);
    }
}
