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
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class MythicMetalsRecipeGenerator {
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
        FoundryRecipeBuilder.build().input(ConventionalItemTags.COPPER_INGOTS).input(TagContent.TIN_INGOTS).result(MythicItems.BRONZE.getIngot()).export(exporter, "compat/mythicmetals/bronze");
        FoundryRecipeBuilder.build().input(MythicItems.MANGANESE.getIngot()).input(MythicItems.QUADRILLUM.getIngot()).result(MythicItems.DURASTEEL.getIngot()).export(exporter, "compat/mythicmetals/durasteel");
        FoundryRecipeBuilder.build().input(TagContent.PLATINUM_INGOTS).input(MythicItems.Mats.STARRITE).result(MythicItems.STAR_PLATINUM.getIngot()).export(exporter, "compat/mythicmetals/star_platinum");
        FoundryRecipeBuilder.build().input(ConventionalItemTags.IRON_INGOTS).input(MythicItems.MANGANESE.getIngot()).result(ItemContent.STEEL_INGOT).export(exporter, "compat/mythicmetals/manganese_steel");
    }
    
}