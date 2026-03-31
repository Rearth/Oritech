package rearth.oritech.generator.compat;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import nourl.mythicmetals.MythicMetals;
import nourl.mythicmetals.item.ItemSet;
import nourl.mythicmetals.item.MythicItems;
import nourl.mythicmetals.misc.RegistryHelper;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

import java.lang.reflect.Modifier;
import java.util.Locale;

import static rearth.oritech.util.TagUtils.cItemTag;
import static rearth.oritech.util.TagUtils.itemTag;

public class MythicMetalsRecipeGenerator {
    private static final String PATH = "compat/mythicmetals/";

    public static void generateRecipes(RecipeOutput exporter) {
        addMMFragmentRecipes(exporter);
        addMMAlloyRecipes(exporter);
    }

    public static void addMMFragmentRecipes(RecipeOutput exporter) {
        
        for (var field : MythicItems.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            
            ItemSet value;
            try {
                value = (ItemSet) field.get(null);
            } catch (IllegalAccessException e) {
                continue;
            }
            
            if (value == null || !ItemSet.class.isAssignableFrom(value.getClass())) continue;
            
            var rawOre = value.getRawOre();
            var name = field.getName().toLowerCase(Locale.ROOT);
            if (rawOre != null)
                GrinderRecipeBuilder.build().input(TagKey.create(Registries.ITEM, RegistryHelper.id("ores/" + name))).result(rawOre, 2).export(exporter, "compat/mythicmetals/" + name);
            
        }

    }

    public static void addMMAlloyRecipes(RecipeOutput exporter) {
        FoundryRecipeBuilder.build().input(ConventionalItemTags.COPPER_INGOTS).input(cItemTag("ingots/tin")).result(MythicItems.BRONZE.getIngot(), 2).export(exporter, PATH + "bronze");
        FoundryRecipeBuilder.build().input(MythicItems.MANGANESE.getIngot()).input(MythicItems.QUADRILLUM.getIngot()).result(MythicItems.DURASTEEL.getIngot()).export(exporter, PATH + "durasteel");
        FoundryRecipeBuilder.build().input(TagContent.PLATINUM_INGOTS).input(MythicItems.Mats.STARRITE).result(MythicItems.STAR_PLATINUM.getIngot()).export(exporter, PATH + "star_platinum");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.IRON_INGOTS).input(MythicItems.MANGANESE.getIngot()).result(ItemContent.STEEL_INGOT, 2).export(exporter, PATH + "manganese_steel");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.GOLD_INGOTS).input(cItemTag("ingots/silver")).result(ItemContent.ELECTRUM_INGOT, 2).export(exporter, PATH + "electrumalt");
        FoundryRecipeBuilder.build().input(itemTag(MythicMetals.MOD_ID, "midas_raw_ores")).input(MythicItems.MIDAS_GOLD.getRawOre()).result(Items.GOLD_INGOT, 2).export(exporter, PATH + "midasgold");

    }
    
}