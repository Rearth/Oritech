package rearth.oritech.util.datagen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.architectury.fluid.FluidStack;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.block.Block;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.SizedIngredient;

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

    public static void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, float timeMultiplier, String suffix) {
        addAssemblerRecipe(exporter, A, B, C, D, result, 1, timeMultiplier, suffix);
    }
    
    public static void addAssemblerRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Ingredient C, Ingredient D, Item result, int count, float timeMultiplier, String suffix) {
        var defaultSpeed = 160;
        var speed = (int) (defaultSpeed * timeMultiplier);
        var inputs = new ArrayList<Ingredient>();
        inputs.add(A);
        if (B != null) inputs.add(B);
        if (C != null) inputs.add(C);
        if (D != null) inputs.add(D);
        var entry = new OritechRecipe(speed, inputs, List.of(new ItemStack(result, count)), RecipeContent.ASSEMBLER, null, null);
        exporter.accept(Oritech.id("assembler/" + suffix), entry, null);
    }

    // A is inserted twice, surrounding B
    public static void addAtomicForgeRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, Item result, int time, String suffix) {
        addAtomicForgeRecipe(exporter, A, B, new ItemStack(result), time, suffix);
    }

    public static void addAtomicForgeRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, ItemStack result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(B, A, A), List.of(result), RecipeContent.ATOMIC_FORGE, null, null);
        exporter.accept(Oritech.id("atomicforge/" + suffix), entry, null);
    }

    public static void addAugmentRecipe(RecipeExporter exporter, List<SizedIngredient> inputs, List<SizedIngredient> applyCost, List<String> requirements, Identifier requiredStation, int uiX, int uiY, int time, long rfCost, String id) {
        var entry = new AugmentRecipe(RecipeContent.AUGMENT, inputs, applyCost, requirements.stream().map(elem -> Identifier.of(elem)).toList(), requiredStation, uiX, uiY, time, rfCost);
        exporter.accept(Oritech.id(id), entry, null);
    }

    public static void addBioGenRecipe(RecipeExporter exporter, Ingredient A, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(A), List.of(), RecipeContent.BIO_GENERATOR, null, null);
        exporter.accept(Oritech.id("biogen/" + suffix), entry, null);
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

    public static void addCoolerRecipe(RecipeExporter exporter, FluidStack input, Item result, int count, float speedMultiplier, String suffix) {
        var coolerDefaultSpeed = (int) (200 * speedMultiplier);
        
        var entry = new OritechRecipe(coolerDefaultSpeed, List.of(), List.of(new ItemStack(result, count)), RecipeContent.COOLER, input, null);
        exporter.accept(Oritech.id("cooler/" + suffix), entry, null);
    }

    public static void addDeepDrillRecipe(RecipeExporter exporter, Block input, Item result, int time, String suffix) {
        var entry = new OritechRecipe(time, List.of(of(input.asItem())), List.of(new ItemStack(result)), RecipeContent.DEEP_DRILL, null, null);
        exporter.accept(Oritech.id("deepdrill/" + suffix), entry, null);
    }

    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    
    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, @Nullable Item ingotSmelted, String suffix) {
        RecipeGeneratorUtil.addPulverizerRecipe(exporter, ingot, dust, suffix);
        RecipeGeneratorUtil.addGrinderRecipe(exporter, ingot, dust, suffix);
        if (ingotSmelted != null) {
            RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }

    public static void addFuelGenRecipe(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.FUEL_GENERATOR, input, null);
        exporter.accept(Oritech.id("fuelgen/" + suffix), entry, null);
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

    public static void addLaserRecipe(RecipeExporter exporter, Ingredient input, ItemConvertible output, String suffix) {
        var entry = new OritechRecipe(1, List.of(input), List.of(new ItemStack(output)), RecipeContent.LASER, null, null);
        exporter.accept(Oritech.id("laser/" + suffix), entry, null);
    }

    public static void addLavaGen(RecipeExporter exporter, FluidStack input, int timeInSeconds, String suffix) {
        var entry = new OritechRecipe(timeInSeconds * 20, List.of(), List.of(), RecipeContent.LAVA_GENERATOR, input, null);
        exporter.accept(Oritech.id("lavagen/" + suffix), entry, null);
    }

    public static void addMetalProcessingChain(RecipeExporter exporter, OreTransform oreTransform) {
        // ore block -> raw ores
        addPulverizerRecipe(exporter, oreTransform.ore(), List.of(new ItemStack(oreTransform.rawOreItem(), 2)), oreTransform.timeMultiplier(), "ore/" + oreTransform.name());
        addGrinderRecipe(exporter, oreTransform.ore(), List.of(new ItemStack(oreTransform.rawOreItem(), 2), new ItemStack(oreTransform.rawOreByproduct(), 1)), oreTransform.timeMultiplier(), "ore/" + oreTransform.name());
        
        // raw ores -> dusts
        var dustOutput = new ArrayList<ItemStack>();
        if (oreTransform.dustItem() != null) {
            dustOutput.add(new ItemStack(oreTransform.dustItem()));
            if (oreTransform.smallDustItem() != null) {
                dustOutput.add(new ItemStack(oreTransform.smallDustItem(), 3));
            } else {
                dustOutput.add(new ItemStack(oreTransform.nuggetItem(), 3));
            }
            addPulverizerRecipe(exporter, oreTransform.rawOreIngredient(), dustOutput, oreTransform.timeMultiplier(), "raw/" + oreTransform.name());
        }
        // raw ores -> clumps
        var clumpOutput = new ArrayList<ItemStack>();
        if (oreTransform.clumpItem() == null) {
            clumpOutput.addAll(dustOutput);
        } else {
            clumpOutput.add(new ItemStack(oreTransform.clumpItem(), 1));
        }
        if (oreTransform.smallClumpItem() != null) {
            clumpOutput.add(new ItemStack(oreTransform.smallClumpItem(), 3));
        } else {
            clumpOutput.add(new ItemStack(oreTransform.nuggetItem(), 3));
        }
        if (oreTransform.smallClumpByproduct() != null && oreTransform.byproductAmount() > 0) {
            clumpOutput.add(new ItemStack(oreTransform.smallClumpByproduct(), oreTransform.byproductAmount()));
        }
        if (!clumpOutput.isEmpty()) {
            addGrinderRecipe(exporter, oreTransform.rawOreIngredient(), clumpOutput, oreTransform.timeMultiplier(), "raw/" + oreTransform.name());
        }
        
        // clump processing into gems
        if (oreTransform.gemItem() != null) {
            if (oreTransform.clumpIngredient() != null) {
                addCentrifugeRecipe(exporter, oreTransform.clumpIngredient(), List.of(new ItemStack(oreTransform.gemItem(), 1), new ItemStack(oreTransform.smallDustByproduct(), oreTransform.byproductAmount())), oreTransform.timeMultiplier(), "clump/" + oreTransform.name());
                addCentrifugeFluidRecipe(exporter, oreTransform.clumpIngredient(), List.of(new ItemStack(oreTransform.gemItem(), 2)), Fluids.WATER, 1, null, 0, oreTransform.timeMultiplier() * 1.5f, "clump/" + oreTransform.name());
                // gems can either be directly smelted for 1:1 results, atomic forge for 1:2, and foundry for 1:1.5
            }
            
            // gems to dust (doubling)
            if (oreTransform.gemIngredient() != null) {
                addAtomicForgeRecipe(exporter, oreTransform.gemCatalyst(), oreTransform.gemIngredient(), new ItemStack(oreTransform.dustItem(), 2), 20, "dust/" + oreTransform.name());
            
                // atomic forge alternative: 2 gems -> 3 ingots
                addAlloyRecipe(exporter, oreTransform.gemIngredient(), oreTransform.gemIngredient(), oreTransform.ingotItem(), 3, oreTransform.timeMultiplier(), "gem/" + oreTransform.name());
            }
        }

        // ingots/nuggets to dust
        if (oreTransform.dustItem() != null)
            addDustRecipe(exporter, oreTransform.ingotIngredient(), oreTransform.dustItem(), "dust/" + oreTransform.name());
        if (oreTransform.smallDustItem() != null)
            addDustRecipe(exporter, oreTransform.nuggetIngredient(), oreTransform.smallDustItem(), "smalldust/" + oreTransform.name());
        
        // smelting/compacting
        // Using item instead of ingredient for recipe inputs, as that's what the offerSmelting/offerBlasting methods accept
        // This should be fine, because any mod that adds ores, dusts, etc. will provide their own vanilla smelting/blasting recipes
        if (oreTransform.addVanillaProcessing()) {
            RecipeProvider.offerSmelting(exporter, List.of(oreTransform.dustItem()), RecipeCategory.MISC, oreTransform.ingotItem(), 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerSmelting(exporter, List.of(oreTransform.gemItem()), RecipeCategory.MISC, oreTransform.ingotItem(), 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerSmelting(exporter, List.of(oreTransform.smallDustItem()), RecipeCategory.MISC, oreTransform.nuggetItem(), 0.5f, 50, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(oreTransform.dustItem()), RecipeCategory.MISC, oreTransform.ingotItem(), 1, 100, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(oreTransform.gemItem()), RecipeCategory.MISC, oreTransform.ingotItem(), 1, 100, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(oreTransform.smallDustItem()), RecipeCategory.MISC, oreTransform.nuggetItem(), 0.5f, 50, Oritech.MOD_ID);
            RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, oreTransform.clumpItem(), oreTransform.smallClumpItem());
            RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, oreTransform.dustItem(), oreTransform.smallDustItem());
            RecipeProvider.offerCompactingRecipe(exporter, RecipeCategory.MISC, oreTransform.ingotItem(), oreTransform.nuggetItem());
        }
    }

    public static void addParticleCollisionRecipe(RecipeExporter exporter, Ingredient A, Ingredient B, ItemStack result, int requiredSpeed, String suffix) {
        var particle = new OritechRecipe(requiredSpeed, List.of(A, B), List.of(result), RecipeContent.PARTICLE_COLLISION, null, null);
        exporter.accept(Oritech.id("particle/" + suffix), particle, null);
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
