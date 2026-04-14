package rearth.oritech.generator.compat;

import static rearth.oritech.util.TagUtils.cItemTag;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.FoundryRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class IronsSpellbooksRecipeGenerator {
    private static final String PATH = "compat/ironsspellbooks/";

    public static void generateRecipes(RecipeOutput exporter) {
        PulverizerRecipeBuilder.build()
            .input(cItemTag("ores/mithril"))
            .result(ItemRegistry.RAW_MITHRIL.get(), 3)
            .addToGrinder()
            .export(exporter, PATH + "rawmithril");

        FoundryRecipeBuilder.build()
            .input(cItemTag("raw_materials/mithril"))
            .input(cItemTag("raw_materials/mithril"))
            .result(ItemRegistry.MITHRIL_SCRAP.get(), 4)
            .export(exporter, PATH + "mithrilscrap");
    }
}
