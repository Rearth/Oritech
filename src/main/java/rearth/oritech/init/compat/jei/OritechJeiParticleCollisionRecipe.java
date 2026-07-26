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

final class OritechJeiParticleCollisionRecipe extends AbstractRecipeCategory<RecipeHolder<OritechRecipe>> {

    private static final int WIDTH = 170;
    private static final int HEIGHT = 66;

    OritechJeiParticleCollisionRecipe(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.PARTICLE_COLLISION,
                Component.translatable("emi.category.oritech.particle_collision"),
                guiHelper.createDrawableItemLike(BlockContent.ACCELERATOR_CONTROLLER.get()),
                WIDTH,
                HEIGHT
        );
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OritechRecipe> holder,
                                   IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(40).setPosition(57, 24);
        builder.addText(
                        Component.translatable("emi.title.oritech.collisionspeed", holder.value().time()),
                        WIDTH - 4, 10
                )
                .setPosition(2, (int) (HEIGHT * 0.88));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OritechRecipe> holder, IFocusGroup focuses) {
        var recipe = holder.value();
        builder.addInputSlot(25, 10)
                .add(recipe.itemInputs().get(0))
                .setStandardSlotBackground();
        builder.addInputSlot(25, 33)
                .add(recipe.itemInputs().get(1))
                .setStandardSlotBackground();
        builder.addOutputSlot(90, 24)
                .add(recipe.itemResults().getFirst())
                .setOutputSlotBackground();
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION)
                .add(BlockContent.ACCELERATOR_CONTROLLER.get());
    }
}
