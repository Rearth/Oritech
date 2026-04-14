package rearth.oritech.generator.compat;

import static rearth.oritech.util.TagUtils.cItemTag;

import com.shynieke.geore.registry.GeOreBlockReg;
import com.shynieke.geore.registry.GeOreRegistry;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.AssemblerRecipeBuilder;
import rearth.oritech.api.recipe.LaserRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class GeOreRecipeGenerator {
    private static final String PATH = "compat/geore/";

    public static void generateRecipes(RecipeOutput exporter) {
        for (GeOreBlockReg geOreBlock : GeOreRegistry.getGeOres()) {
            AssemblerRecipeBuilder.build()
                .input(geOreBlock.getShard().get())
                .input(geOreBlock.getShard().get())
                .input(ItemContent.ENDERIC_COMPOUND)
                .input(ItemContent.OVERCHARGED_CRYSTAL)
                .result(geOreBlock.getBudding().get().asItem())
                .export(exporter, PATH + "budding" + geOreBlock.getName());
            
            
        }
        // enderic laser should yield plutonium dust when harvesting uranium clusters
        LaserRecipeBuilder.build().input(GeOreRegistry.URANIUM_GEORE.getCluster().get()).result(ItemContent.PLUTONIUM_DUST).export(exporter, PATH + "plutoniumdust");

        // pulverize quartz shards into quartz dust, no need for intermediate smelting
        PulverizerRecipeBuilder.build().input(GeOreRegistry.QUARTZ_GEORE.getShard().get()).result(ItemContent.QUARTZ_DUST).addToGrinder().export(exporter, PATH + "quartzdust");
    }
}
