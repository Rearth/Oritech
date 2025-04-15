package rearth.oritech.fabricgen.datagen.compat;

import net.emilsg.clutter.block.ModBlocks;
import net.emilsg.clutter.item.ModItems;
import net.emilsg.clutter.util.ModItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.Items;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.api.recipe.util.MetalProcessingChainBuilder;

public class ClutterRecipeGenerator {
    private static final String PATH = "compat/clutter/";
    public static void generateRecipes(RecipeExporter exporter) {
        MetalProcessingChainBuilder.build(PATH + "silver")
            .ore(ModItemTags.C_RAW_SILVER_ORES)
            .rawOre(ModItemTags.C_RAW_SILVERS, ModItems.RAW_SILVER).rawOreByproduct(Items.RAW_GOLD)
            .export(exporter);
        PulverizerRecipeBuilder.build().input(ModBlocks.SULPHUR_BLOCK).result(ModItems.SULPHUR).addToGrinder().export(exporter, PATH + "sulphur");
    }
}
