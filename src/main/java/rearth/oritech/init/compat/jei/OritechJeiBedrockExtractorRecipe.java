package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechJeiBedrockExtractorRecipe extends AbstractRecipeCategory<RecipeHolder<OritechRecipe>> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 88;

    OritechJeiBedrockExtractorRecipe(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.BEDROCK_EXTRACTOR,
                Component.translatable("block.oritech.bedrock_extractor"),
                guiHelper.createDrawableItemLike(BlockContent.BEDROCK_EXTRACTOR.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OritechRecipe> recipe,
                                   IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(40).setPosition(65, 20);
        builder.addText(Component.translatable("emi.title.oritech.bedrock_extractor_placement"), WIDTH - 4, 10)
                .setPosition(2, 60);
        builder.addText(Component.translatable("emi.title.oritech.bedrock_extractor_infinite"), WIDTH - 4, 10)
                .setPosition(2, 70);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OritechRecipe> holder, IFocusGroup focuses) {
        var recipe = holder.value();

        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 30, 10)
                .add(BlockContent.BEDROCK_EXTRACTOR.get())
                .setStandardSlotBackground();
        builder.addInputSlot(30, 32)
                .add(recipe.itemInputs().getFirst())
                .setStandardSlotBackground();
        builder.addOutputSlot(115, 20)
                .add(recipe.itemResults().getFirst())
                .setOutputSlotBackground();
    }
}
