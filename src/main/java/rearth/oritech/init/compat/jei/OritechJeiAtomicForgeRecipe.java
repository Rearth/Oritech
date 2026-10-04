package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;

final class OritechJeiAtomicForgeRecipe extends AbstractRecipeCategory<RecipeHolder<OritechRecipe>> {

    private static final int WIDTH = 170;
    private static final int HEIGHT = 92;
    private static final int[] INPUT_X = {18, 41, 41};
    private static final int[] INPUT_Y = {38, 27, 49};

    private final IDrawable atomicForge;
    private final IDrawable endericLaser;

    OritechJeiAtomicForgeRecipe(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.ATOMIC_FORGE,
                Component.translatable("emi.category.oritech.atomic_forge"),
                guiHelper.createDrawableItemLike(BlockContent.ATOMIC_FORGE.get()),
                WIDTH,
                HEIGHT
        );
        this.atomicForge = guiHelper.createDrawableItemLike(BlockContent.ATOMIC_FORGE.get());
        this.endericLaser = guiHelper.createDrawableItemLike(BlockContent.ENDERIC_LASER.get());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OritechRecipe> holder,
                                   IFocusGroup focuses) {
        // Show the forge being charged from both sides. One laser is enough; additional input only makes it faster.
        builder.addDrawable(endericLaser, 45, 3);
        builder.addText(Component.literal("→"), 10, 10).setPosition(64, 6);
        builder.addDrawable(atomicForge, 77, 3);
        builder.addText(Component.literal("←"), 10, 10).setPosition(97, 6);
        builder.addDrawable(endericLaser, 109, 3);

        builder.addAnimatedRecipeArrow(40).setPosition(70, 37);

        var totalEnergy = (long) OritechConfig.processingMachines.atomicForgeData.energyPerTick.get()
                * holder.value().time();
        builder.addText(
                        Component.translatable("emi.title.oritech.atomic_forge_energy", totalEnergy),
                        WIDTH - 4, 10
                )
                .setPosition(2, 69);
        builder.addText(
                        Component.translatable("emi.description.oritech.atomic_forge_laser_speed"),
                        WIDTH - 4, 10
                )
                .setPosition(2, 81);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OritechRecipe> holder, IFocusGroup focuses) {
        var recipe = holder.value();

        for (int i = 0; i < Math.min(recipe.itemInputs().size(), INPUT_X.length); i++) {
            builder.addInputSlot(INPUT_X[i], INPUT_Y[i])
                    .add(recipe.itemInputs().get(i))
                    .setStandardSlotBackground();
        }

        if (!recipe.itemResults().isEmpty()) {
            builder.addOutputSlot(113, 38)
                    .add(recipe.itemResults().getFirst())
                    .setOutputSlotBackground();
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION)
                .add(BlockContent.ATOMIC_FORGE.get())
                .add(BlockContent.ENDERIC_LASER.get());
    }
}
