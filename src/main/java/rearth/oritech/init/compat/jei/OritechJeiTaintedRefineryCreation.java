package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;

final class OritechJeiTaintedRefineryCreation
        extends AbstractRecipeCategory<OritechJeiTaintedRefineryCreation.CreationInfo> {

    OritechJeiTaintedRefineryCreation(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.TAINTED_REFINERY_CREATION,
                Component.translatable("emi.category.oritech.tainted_refinery_creation"),
                guiHelper.createDrawableItemLike(BlockContent.TAINTED_REFINERY.get()),
                160,
                82
        );
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CreationInfo recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(40).setPosition(50, 22);
        builder.addText(Component.translatable("emi.title.oritech.tainted_creation_hint"), 156, 10)
                .setPosition(2, 62);
        builder.addText(Component.translatable("emi.title.oritech.tainted_creation_hint2"), 156, 10)
                .setPosition(2, 72);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CreationInfo recipe, IFocusGroup focuses) {
        builder.addInputSlot(10, 10)
                .add(BlockContent.REFINERY.get())
                .setStandardSlotBackground();
        builder.addInputSlot(10, 35)
                .add(BlockContent.ARCANE_CATALYST.get())
                .setStandardSlotBackground();
        builder.addOutputSlot(90, 22)
                .add(BlockContent.TAINTED_REFINERY.get())
                .setOutputSlotBackground();
    }

    @Override
    public Identifier getIdentifier(CreationInfo recipe) {
        return Oritech.id("tainted_refinery_creation/info");
    }

    record CreationInfo() {
    }
}
