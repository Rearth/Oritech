package rearth.oritech.fabricgen.datagen.compat;

import io.wispforest.owo.util.ReflectionUtils;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import nourl.mythicmetals.item.ItemSet;
import nourl.mythicmetals.item.MythicItems;
import nourl.mythicmetals.misc.RegistryHelper;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.datagen.RecipeGeneratorUtil;

public class MythicMetalsRecipeGenerator {
    public static void generateRecipes(RecipeExporter exporter) {
        addMMFragmentRecipes(exporter);
        addMMAlloyRecipes(exporter);
    }

    public static void addMMFragmentRecipes(RecipeExporter exporter) {
        ReflectionUtils.iterateAccessibleStaticFields(MythicItems.class, ItemSet.class, (itemSet, name, field) -> {
            var rawOre = itemSet.getRawOre();
            if (rawOre != null)
                RecipeGeneratorUtil.addGrinderRecipe(exporter, Ingredient.fromTag(TagKey.of(RegistryKeys.ITEM, RegistryHelper.id("ores/" + name))), rawOre, 2, "compat/mythicmetals/" + name);
        });

    }

    public static void addMMAlloyRecipes(RecipeExporter exporter) {
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.COPPER_INGOTS), Ingredient.fromTag(TagContent.TIN_INGOTS), MythicItems.BRONZE.getIngot(), 1, "compat/mythicmetals/bronze");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.ofItems(MythicItems.MANGANESE.getIngot()), Ingredient.ofItems(MythicItems.QUADRILLUM.getIngot()), MythicItems.DURASTEEL.getIngot(), "compat/mythicmetals/durasteel");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.fromTag(TagContent.PLATINUM_INGOTS), Ingredient.ofItems(MythicItems.Mats.STARRITE), MythicItems.STAR_PLATINUM.getIngot(), "compat/mythicmetals/star_platinum");
        RecipeGeneratorUtil.addAlloyRecipe(exporter, Ingredient.fromTag(ConventionalItemTags.IRON_INGOTS), Ingredient.ofItems(MythicItems.MANGANESE.getIngot()), ItemContent.STEEL_INGOT, "compat/mythicmetals/manganese_steel");
    }
    
}