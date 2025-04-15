package rearth.oritech.fabricgen.datagen.compat;

import io.wispforest.owo.util.ReflectionUtils;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import nourl.mythicmetals.MythicMetals;
import nourl.mythicmetals.item.ItemSet;
import nourl.mythicmetals.item.MythicItems;
import nourl.mythicmetals.misc.RegistryHelper;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import static rearth.oritech.init.TagContent.cItemTag;
import static rearth.oritech.init.TagContent.itemTag;

public class MythicMetalsRecipeGenerator {
    private static final String PATH = "compat/mythicmetals/";

    public static void generateRecipes(RecipeExporter exporter) {
        addMMFragmentRecipes(exporter);
        addMMAlloyRecipes(exporter);
    }

    public static void addMMFragmentRecipes(RecipeExporter exporter) {
        ReflectionUtils.iterateAccessibleStaticFields(MythicItems.class, ItemSet.class, (itemSet, name, field) -> {
            var rawOre = itemSet.getRawOre();
            if (rawOre != null)
                GrinderRecipeBuilder.build().input(TagKey.of(RegistryKeys.ITEM, RegistryHelper.id("ores/" + name))).result(rawOre, 2).export(exporter, "compat/mythicmetals/" + name);
        });

    }

    public static void addMMAlloyRecipes(RecipeExporter exporter) {
        FoundryRecipeBuilder.build().input(ConventionalItemTags.COPPER_INGOTS).input(cItemTag("ingots/tin")).result(MythicItems.BRONZE.getIngot(), 2).export(exporter, PATH + "bronze");
        FoundryRecipeBuilder.build().input(MythicItems.MANGANESE.getIngot()).input(MythicItems.QUADRILLUM.getIngot()).result(MythicItems.DURASTEEL.getIngot()).export(exporter, PATH + "durasteel");
        FoundryRecipeBuilder.build().input(TagContent.PLATINUM_INGOTS).input(MythicItems.Mats.STARRITE).result(MythicItems.STAR_PLATINUM.getIngot()).export(exporter, PATH + "star_platinum");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.IRON_INGOTS).input(MythicItems.MANGANESE.getIngot()).result(ItemContent.STEEL_INGOT, 2).export(exporter, PATH + "manganese_steel");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.GOLD_INGOTS).input(cItemTag("ingots/silver")).result(ItemContent.ELECTRUM_INGOT, 2).export(exporter, PATH + "electrumalt");
        FoundryRecipeBuilder.build().input(itemTag(MythicMetals.MOD_ID, "midas_raw_ores")).input(MythicItems.MIDAS_GOLD.getRawOre()).result(Items.GOLD_INGOT, 2).export(exporter, PATH + "midasgold");

    }
    
}