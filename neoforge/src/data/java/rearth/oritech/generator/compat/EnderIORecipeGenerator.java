package rearth.oritech.generator.compat;

import static rearth.oritech.api.recipe.util.RecipeHelpers.createInsulatedCableRecipe;

import java.util.List;
import java.util.Optional;

import com.enderio.enderio.init.EIOFluids;
import com.enderio.enderio.init.EIOItems;
import com.enderio.enderio.content.machines.soul_binder.SoulBindingRecipe;
import com.enderio.enderio.content.machines.alloy.AlloySmeltingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeRecipeBuilder;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.FluidIngredient;

public class EnderIORecipeGenerator {
    private static final String PATH = "compat/enderio/";

    public static void generateRecipes(RecipeOutput exporter, RecipeProvider provider) {
        addAlloys(exporter);
        addDusts(exporter);
        conduitBinderCrafting(exporter, provider);
        soulBinding(exporter);

        // enderic compound from ender pearl dust
        CentrifugeRecipeBuilder.build().input(EIOItems.POWDERED_ENDER_PEARL.get()).result(ItemContent.ENDERIC_COMPOUND, 2).export(exporter, PATH + "endericcompound");

        // xp juice from sculk
        CentrifugeFluidRecipeBuilder.build().input(Items.SCULK).fluidInput(Fluids.WATER, 0.25f).fluidOutput(EIOFluids.XP_JUICE.source().get(), 0.1f).export(exporter, PATH + "sculkxp");
    }

    private static void addAlloys(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_COPPER).input(Tags.Items.INGOTS_IRON).result(EIOItems.CONDUCTIVE_ALLOY_INGOT.get()).time(120).export(exporter, PATH + "conductivealloy");
        FoundryRecipeBuilder.build().input(TagContent.ELECTRUM_INGOTS).input(EIOItems.CONDUCTIVE_ALLOY_INGOT.get()).result(EIOItems.ENERGETIC_ALLOY_INGOT.get()).time(150).export(exporter, PATH + "energeticalloy");
        FoundryRecipeBuilder.build().input(Tags.Items.DUSTS_REDSTONE).input(Tags.Items.INGOTS_COPPER).result(EIOItems.REDSTONE_ALLOY_INGOT.get()).time(120).export(exporter, PATH + "redstonealloy");
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_IRON).input(Tags.Items.ENDER_PEARLS).result(EIOItems.PULSATING_ALLOY_INGOT.get(), 2).time(150).export(exporter, PATH + "pulsatingalloy");
        FoundryRecipeBuilder.build().input(TagContent.STEEL_INGOTS).input(Tags.Items.OBSIDIANS_NORMAL).result(EIOItems.DARK_STEEL_INGOT.get()).time(120).export(exporter, PATH + "darksteel");
        FoundryRecipeBuilder.build().input(Tags.Items.INGOTS_GOLD).input(ItemTags.SOUL_FIRE_BASE_BLOCKS).result(EIOItems.SOULARIUM_INGOT.get()).time(180).export(exporter, PATH + "soularium");

        exporter.accept(Oritech.id(PATH + "alloy/adamant"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.NICKEL_INGOTS), 1), new SizedIngredient(Ingredient.of(Tags.Items.GEMS_DIAMOND), 1)), new ItemStack(ItemContent.ADAMANT_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/steel"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_IRON), 1), new SizedIngredient(Ingredient.of(TagContent.COAL_DUSTS), 1)), new ItemStack(ItemContent.STEEL_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/biosteel"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_IRON), 1), new SizedIngredient(Ingredient.of(ItemContent.RAW_BIOPOLYMER), 1)), new ItemStack(ItemContent.BIOSTEEL_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/duratium"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.PLATINUM_INGOTS), 1), new SizedIngredient(Ingredient.of(Tags.Items.INGOTS_NETHERITE), 1)), new ItemStack(ItemContent.DURATIUM_INGOT), 3200, 0.3f), null);
        exporter.accept(Oritech.id(PATH + "alloy/energite"), new AlloySmeltingRecipe(List.of(new SizedIngredient(Ingredient.of(TagContent.NICKEL_INGOTS), 1), new SizedIngredient(Ingredient.of(ItemContent.FLUXITE), 1)), new ItemStack(ItemContent.ENERGITE_INGOT), 3200, 0.3f), null);
    }

    private static void addDusts(RecipeOutput exporter) {
        // powdered obsidian from obsidian
        // EnderIO and Mekanism recipes are both equivalent, and both mods can use either obsidian dust. Loading both recipes shouldn't be a problem.
        PulverizerRecipeBuilder.build().input(Tags.Items.OBSIDIANS).result(EIOItems.POWDERED_OBSIDIAN.get(), 4).time(140).addToGrinder().export(exporter, PATH + "dust/obsidian");

        PulverizerRecipeBuilder.build().input(Tags.Items.GEMS_LAPIS).result(EIOItems.POWDERED_LAPIS_LAZULI.get()).time(120).addToGrinder().export(exporter, PATH + "dust/lapis");
        PulverizerRecipeBuilder.build().input(EIOItems.PRESCIENT_CRYSTAL).result(EIOItems.PRESCIENT_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/prescient");
        PulverizerRecipeBuilder.build().input(EIOItems.VIBRANT_CRYSTAL).result(EIOItems.VIBRANT_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/vibrant");
        PulverizerRecipeBuilder.build().input(EIOItems.PULSATING_CRYSTAL).result(EIOItems.PULSATING_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/piezal");
        PulverizerRecipeBuilder.build().input(EIOItems.ENDER_CRYSTAL).result(EIOItems.ENDER_CRYSTAL_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/endgrain");
        PulverizerRecipeBuilder.build().input(EIOItems.SOULARIUM_INGOT).result(EIOItems.SOUL_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/soul");
        PulverizerRecipeBuilder.build().input(Items.WITHER_SKELETON_SKULL).result(EIOItems.WITHERING_POWDER.get()).time(120).addToGrinder().export(exporter, PATH + "dust/withering");
    }

    private static void conduitBinderCrafting(RecipeOutput exporter, RecipeProvider provider) {
        var conduitBinder = EIOItems.CONDUIT_BINDER.get();
        createInsulatedCableRecipe(RecipeCategory.MISC,
                BlockContent.FLUID_PIPE.asItem(), 6,
                Ingredient.of(conduitBinder),
                Ingredient.of(Tags.Items.INGOTS_COPPER))
            .unlockedBy(RecipeProvider.getHasName(conduitBinder), RecipeProvider.has(conduitBinder))
            .save(exporter, Oritech.id(PATH + "crafting/fluidpipe"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockContent.PUMP_BLOCK.asItem(), 1)
            .define('s', Ingredient.of(conduitBinder))
            .define('c', Ingredient.of(ItemContent.MOTOR))
            .define('f', Ingredient.of(Tags.Items.INGOTS_COPPER))
            .define('b', Ingredient.of(TagContent.MACHINE_PLATING))
            .pattern("fff")
            .pattern("fcf")
            .pattern("sbs")
            .unlockedBy(provider.getHasName(conduitBinder), RecipeProvider.has(conduitBinder)).save(exporter, Oritech.id(PATH + "crafting/pump"));
    }

    private static void soulBinding(RecipeOutput exporter) {
        // Kind of redundant, but still fun. A soul vial is filled the exact same way a dubious container is--by "capturing" an entity with the item.
        for (EntityType entityType : List.of(EntityType.ALLAY, EntityType.VEX, EntityType.PHANTOM)) {
            var entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            exporter.accept(
                Oritech.id(PATH + entityKey.getPath() + "soul"),
                new SoulBindingRecipe(
                    new ItemStack(ItemContent.UNHOLY_INTELLIGENCE),
                    Ingredient.of(ItemContent.DUBIOS_CONTAINER),
                    51200,
                    4,
                    Optional.of(entityKey),
                    Optional.empty(),
                    Optional.empty(),
                    false), null);
        }
    }
}
