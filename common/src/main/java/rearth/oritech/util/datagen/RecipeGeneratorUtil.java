package rearth.oritech.util.datagen;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public class RecipeGeneratorUtil {
    public static int DEFAULT_SPEED = 200;
    public static int DEFAULT_SPEED_GRINDER = 140;

    public static void addAlloyRecipe(RecipeExporter exporter, Item A, Item B, Item result, String suffix) {
        addAlloyRecipe(exporter, Ingredient.ofItems(A), Ingredient.ofItems(B), result, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, String suffix) {
        addAlloyRecipe(exporter, A, B, result, 1, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int count, String suffix) {
        addAlloyRecipe(exporter, A, B, result, count, 1f, suffix);
    }
    
    public static void addAlloyRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int count, float speedMultiplier, String suffix) {
        var foundryDefaultSpeed = (int) (DEFAULT_SPEED * speedMultiplier);
        
        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(A, B), List.of(new ItemStack(result, count)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundry/alloy/" + suffix), entry, null);
        
        var entryInverse = new OritechRecipe(foundryDefaultSpeed, List.of(B, A), List.of(new ItemStack(result, count)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundry/alloy/inverse/" + suffix), entryInverse, null);
    }

    public static void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, float timeMultiplier, String suffix) {
        addCentrifugeRecipe(exporter, input, List.of(new ItemStack(result)), timeMultiplier, suffix);
    }
    
    public static void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, Item result, int count, float timeMultiplier, String suffix) {
        addCentrifugeRecipe(exporter, input, List.of(new ItemStack(result, count)), timeMultiplier, suffix);
    }
    
    public static void addCentrifugeRecipe(RecipeExporter exporter, Ingredient input, List<ItemStack> results, float timeMultiplier, String suffix) {
        var speed = (int) (DEFAULT_SPEED * timeMultiplier);
        var entry = new OritechRecipe(speed, List.of(input), results, RecipeContent.CENTRIFUGE, null, null);
        exporter.accept(Oritech.id("centrifuge/" + suffix), entry, null);
    }
    
    public static void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, Item result, Fluid in, float bucketsIn, @Nullable Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        addCentrifugeFluidRecipe(exporter, input, result != null ? List.of(new ItemStack(result)) : List.of(), in, bucketsIn, out, bucketsOut, timeMultiplier, suffix);
    }

    public static void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, Item result, int count, Fluid in, float bucketsIn, @Nullable Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        addCentrifugeFluidRecipe(exporter, input, List.of(new ItemStack(result, count)), in, bucketsIn, out, bucketsOut, timeMultiplier, suffix);
    }

    public static void addCentrifugeFluidRecipe(RecipeExporter exporter, Ingredient input, List<ItemStack> results, Fluid in, float bucketsIn, @Nullable Fluid out, float bucketsOut, float timeMultiplier, String suffix) {
        var speed = (int) (DEFAULT_SPEED * timeMultiplier);
        var inputStack = in != null ? FluidStack.create(in, (long) (bucketsIn * 81000)) : null;
        var outputStack = out != null ? FluidStack.create(out, (long) (bucketsOut * 81000)) : null;
        var entry = new OritechRecipe(speed, List.of(input), results, RecipeContent.CENTRIFUGE_FLUID, inputStack, outputStack);
        exporter.accept(Oritech.id("centrifuge/fluid/" + suffix), entry, null);
    }

    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addGrinderRecipe(exporter, ingot, List.of(new ItemStack(dust, 1)), 1f, suffix);
    }
    
    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        addGrinderRecipe(exporter, ingot, List.of(new ItemStack(dust, dustCount)), 1f, suffix);
    }
    
    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, List<ItemStack> outputs, String suffix) {
        addGrinderRecipe(exporter, ingot, outputs, 1f, suffix);
    }

    public static void addGrinderRecipe(RecipeExporter exporter, Ingredient ingot, List<ItemStack> outputs, float timeMultiplier, String suffix) {
        var grinder = new OritechRecipe((int)(DEFAULT_SPEED_GRINDER * timeMultiplier), List.of(ingot), outputs, RecipeContent.GRINDER, null, null);
        exporter.accept(Oritech.id("grinder/" + suffix), grinder, null);
    }

    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addPulverizerRecipe(exporter, ingot, dust, 1, suffix);
    }
    
    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, int dustCount, String suffix) {
        addPulverizerRecipe(exporter, ingot, List.of(new ItemStack(dust, dustCount)), suffix);
    }

    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, List<ItemStack> dusts, String suffix) {
        addPulverizerRecipe(exporter, ingot, dusts, 1f, suffix);
    }

    public static void addPulverizerRecipe(RecipeExporter exporter, Ingredient ingot, List<ItemStack> dusts, float timeMultiplier, String suffix) {
        var pulverizer = new OritechRecipe((int)(DEFAULT_SPEED * timeMultiplier), List.of(ingot), dusts, RecipeContent.PULVERIZER, null, null);
        exporter.accept(Oritech.id("pulverizer/" + suffix), pulverizer, null);
    }
}
