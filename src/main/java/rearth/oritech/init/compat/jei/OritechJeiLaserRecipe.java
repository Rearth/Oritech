package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechJeiLaserRecipe extends AbstractRecipeCategory<RecipeHolder<OritechRecipe>> {

    private static final Identifier BACKGROUND_TEXTURE =
            Oritech.id("textures/gui/modular/laser_recipe_background_jei.png");
    private static final int WIDTH = 165;
    private static final int HEIGHT = 80;

    private final IDrawableStatic laserBackground;

    OritechJeiLaserRecipe(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.LASER,
                Component.translatable("emi.category.oritech.laser"),
                guiHelper.createDrawableItemLike(BlockContent.ENDERIC_LASER.get()),
                WIDTH,
                HEIGHT
        );
        this.laserBackground = guiHelper.drawableBuilder(BACKGROUND_TEXTURE, 0, 0, 80, 80)
                .setTextureSize(80, 80)
                .build();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OritechRecipe> recipe,
                                   IFocusGroup focuses) {
        builder.addDrawable(laserBackground, 10, 5);
        builder.addAnimatedRecipeArrow(40).setPosition(105, 15);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OritechRecipe> holder, IFocusGroup focuses) {
        var recipe = holder.value();
        builder.addInputSlot(80, 15)
                .add(recipe.itemInputs().getFirst())
                .setStandardSlotBackground();
        builder.addOutputSlot(135, 15)
                .add(recipe.itemResults().getFirst())
                .setOutputSlotBackground();
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION)
                .add(BlockContent.ENDERIC_LASER.get());
    }
}
