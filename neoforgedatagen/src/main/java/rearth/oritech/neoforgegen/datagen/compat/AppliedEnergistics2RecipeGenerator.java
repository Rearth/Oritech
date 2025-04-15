package rearth.oritech.neoforgegen.datagen.compat;

import static rearth.oritech.init.TagContent.cItemTag;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.recipes.handlers.ChargerRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.LaserRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import rearth.oritech.init.ItemContent;

public class AppliedEnergistics2RecipeGenerator {
    private static final String PATH = "compat/ae2/";

    public static void generateRecipes(RecipeOutput exporter) {
        // enderic laser should yield charged certus crystals instead of regular certus crystals
        LaserRecipeBuilder.build().input(AEBlocks.QUARTZ_CLUSTER).result(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.get()).export(exporter, PATH + "chargedquartz");

        PulverizerRecipeBuilder.build().input(AEBlocks.SKY_STONE_BLOCK).result(AEItems.SKY_DUST.get()).addToGrinder().export(exporter, PATH + "skydust");
        PulverizerRecipeBuilder.build().input(cItemTag("gems/certus_quartz")).result(AEItems.CERTUS_QUARTZ_DUST.get()).addToGrinder().export(exporter, PATH + "certusdust");

        exporter.accept(Oritech.id(PATH + "charger/fluxite"), new ChargerRecipe(Ingredient.of(Tags.Items.GEMS_AMETHYST), new ItemStack(ItemContent.FLUXITE)), null);
    }
}
