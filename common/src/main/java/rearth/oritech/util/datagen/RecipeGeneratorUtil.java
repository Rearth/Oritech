package rearth.oritech.util.datagen;

import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;

import dev.architectury.fluid.FluidStack;

import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.block.Block;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class RecipeGeneratorUtil {

    public static void addAugmentRecipe(RecipeExporter exporter, List<SizedIngredient> inputs, List<SizedIngredient> applyCost, List<String> requirements, Identifier requiredStation, int uiX, int uiY, int time, long rfCost, String id) {
        var entry = new AugmentRecipe(RecipeContent.AUGMENT, inputs, applyCost, requirements.stream().map(elem -> Identifier.of(elem)).toList(), requiredStation, uiX, uiY, time, rfCost);
        exporter.accept(Oritech.id(id), entry, null);
    }

    public static void addBioGenRecipe(RecipeExporter exporter, Ingredient A, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(A), List.of(), RecipeContent.BIO_GENERATOR, null, null);
        exporter.accept(Oritech.id("biogen/" + suffix), entry, null);
    }

    public static void addCoolerRecipe(RecipeExporter exporter, FluidStack input, Item result, int count, float speedMultiplier, String suffix) {
        var coolerDefaultSpeed = (int) (200 * speedMultiplier);
        
        var entry = new OritechRecipe(coolerDefaultSpeed, List.of(), List.of(new ItemStack(result, count)), RecipeContent.COOLER, input, null);
        exporter.accept(Oritech.id("cooler/" + suffix), entry, null);
    }

    public static void addDeepDrillRecipe(RecipeExporter exporter, Block input, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(of(input.asItem())), List.of(new ItemStack(result)), RecipeContent.DEEP_DRILL, null, null);
        exporter.accept(Oritech.id("deepdrill/" + suffix), entry, null);
    }

    public static void addLaserRecipe(RecipeExporter exporter, Ingredient input, ItemConvertible output, String suffix) {
        var entry = new OritechRecipe(1, List.of(input), List.of(new ItemStack(output)), RecipeContent.LASER, null, null);
        exporter.accept(Oritech.id("laser/" + suffix), entry, null);
    }

    public static void addLavaGen(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.LAVA_GENERATOR, input, null);
        exporter.accept(Oritech.id("lavagen/" + suffix), entry, null);
    }

    public static void addParticleCollisionRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, ItemStack result, int requiredSpeed, String suffix) {
        var particle = new OritechRecipe(requiredSpeed, List.of(A, B), List.of(result), RecipeContent.PARTICLE_COLLISION, null, null);
        exporter.accept(Oritech.id("particle/" + suffix), particle, null);
    }

    public static void addReactorGen(RecipeExporter exporter, Ingredient input, int timeInTicks, String suffix) {
        var entry = new OritechRecipe(timeInTicks, List.of(input), List.of(), RecipeContent.REACTOR, null, null);
        exporter.accept(Oritech.id("reactor/" + suffix), entry, null);
    }

    public static void addSteamEngineGen(RecipeExporter exporter, FluidStack input, int timeInTicks, String suffix) {
        var entry = new OritechRecipe(timeInTicks, List.of(), List.of(), RecipeContent.STEAM_ENGINE, input, null);
        exporter.accept(Oritech.id("steamgen/" + suffix), entry, null);
    }

    public static Ingredient of(ItemConvertible item) {
        return Ingredient.ofItems(item);
    }
    
    public static Ingredient of(TagKey<Item> item) {
        return Ingredient.fromTag(item);
    }

    public static TagKey<Item> cItemTag(String path) {
      return TagKey.of(RegistryKeys.ITEM, Identifier.of(TagUtil.C_TAG_NAMESPACE, path));
    }
}
