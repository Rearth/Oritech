package rearth.oritech.generator.compat;

import static rearth.oritech.util.TagUtils.cItemTag;

import com.glodblock.github.extendedae.common.EAESingletons;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class ExtendedAERecipeGenerator {
    private static final String PATH = "compat/extendedae/";

    public static void generateRecipes(RecipeOutput exporter) {
        PulverizerRecipeBuilder.build().input(cItemTag("gems/entro")).result(EAESingletons.ENTRO_DUST).addToGrinder().export(exporter, PATH + "entrodust");
    }
}
