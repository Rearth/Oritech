package rearth.oritech.generator.compat;

import static rearth.oritech.util.TagUtils.cItemTag;
import static rearth.oritech.util.TagUtils.itemTag;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.CentrifugeFluidRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class EvilCraftRecipeGenerator {
    private static final String PATH = "compat/evilcraft/";

    public static void generateRecipes(RecipeOutput exporter) {
        // poison
        CentrifugeFluidRecipeBuilder.build()
            .input(itemTag(Reference.MOD_ID, "poisonous"))
            .fluidInput(Fluids.WATER, 0.25f)
            .fluidOutput(RegistryEntries.FLUID_POISON.get(), 0.25f)
            .export(exporter, PATH + "poison");

        // blood from leaves
        CentrifugeFluidRecipeBuilder.build()
            .input(RegistryEntries.BLOCK_UNDEAD_LEAVES.get())
            .fluidInput(Fluids.WATER, 0.1f)
            .fluidOutput(RegistryEntries.FLUID_BLOOD.get(), 0.05f)
            .result(RegistryEntries.ITEM_HARDENED_BLOOD_SHARD.get())
            .export(exporter, PATH + "bloodfromleaves");
        
        // red sand from sand + blood
        CentrifugeFluidRecipeBuilder.build()
            .input(cItemTag("sands/colorless"))
            .fluidInput(RegistryEntries.FLUID_BLOOD.get(), 0.01f)
            .result(Items.RED_SAND)
            .export(exporter, PATH + "stainedsand");

        // crushing gem
        PulverizerRecipeBuilder.build()
            .input(RegistryEntries.ITEM_DARK_GEM.get())
            .result(RegistryEntries.ITEM_DARK_GEM_CRUSHED.get())
            .addToGrinder()
            .export(exporter, PATH + "crusheddarkgem");
        
        // dark ore processing
        PulverizerRecipeBuilder.build()
            .input(itemTag(Reference.MOD_ID, "dark_ores"))
            .result(RegistryEntries.ITEM_DARK_GEM.get(), 2)
            .result(RegistryEntries.ITEM_DARK_GEM_CRUSHED.get())
            .addToGrinder()
            .export(exporter, PATH + "darkores");
    }
}
