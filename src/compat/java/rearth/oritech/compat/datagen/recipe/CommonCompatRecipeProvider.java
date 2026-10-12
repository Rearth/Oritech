package rearth.oritech.compat.datagen.recipe;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import rearth.oritech.Oritech;
import rearth.oritech.compat.datagen.tag.CompatTags;
import rearth.oritech.datagen.builders.CentrifugeRecipeBuilder;
import rearth.oritech.init.ItemContent;



public class CommonCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/common";
    
    public CommonCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);
    }

    protected ICondition tagNotEmpty(TagKey<Item> itemTag) {
        return new NotCondition(new TagEmptyCondition<Item>(itemTag));
    }

    @Override
    protected void buildRecipes() {
        // enderic compound from ender pearl dust
        new CentrifugeRecipeBuilder(registries)
            .input(CompatTags.Items.C_ENDER_DUST).result(ItemContent.ENDERIC_COMPOUND, 2)
            .export(this.output.withConditions(tagNotEmpty(CompatTags.Items.C_ENDER_DUST)), PATH, "enderic_compound", Oritech.MOD_ID);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new CommonCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "Common Oritech Compat";
        }
    }
    
}
