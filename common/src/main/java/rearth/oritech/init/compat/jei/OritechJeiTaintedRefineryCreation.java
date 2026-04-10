package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;

public class OritechJeiTaintedRefineryCreation implements IRecipeCategory<OritechJeiTaintedRefineryCreation.CreationInfo> {
    
    public static final RecipeType<CreationInfo> RECIPE_TYPE = RecipeType.create("oritech", "tainted_refinery_creation", CreationInfo.class);
    
    public final IDrawable icon;
    public final IDrawableAnimated arrow;
    public final IDrawableStatic background;
    
    public OritechJeiTaintedRefineryCreation(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(BlockContent.TAINTED_REFINERY_BLOCK.asItem()));
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
    }
    
    @Override
    public @NotNull RecipeType<CreationInfo> getRecipeType() {
        return RECIPE_TYPE;
    }
    
    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("emi.category.oritech.tainted_refinery_creation");
    }
    
    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }
    
    @Override
    public int getWidth() {
        return 160;
    }
    
    @Override
    public int getHeight() {
        return 82;
    }
    
    @Override
    public void draw(CreationInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 50, 22);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.title.oritech.tainted_creation_hint"), 2, (int) (getHeight() * 0.76), 0xFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.title.oritech.tainted_creation_hint2"), 2, (int) (getHeight() * 0.88), 0xFFFFFF);
    }
    
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CreationInfo recipe, IFocusGroup focuses) {
        builder.addInputSlot(10, 10).addItemStack(new ItemStack(BlockContent.REFINERY_BLOCK)).setBackground(background, -1, -1);
        builder.addInputSlot(10, 35).addItemStack(new ItemStack(BlockContent.ENCHANTMENT_CATALYST_BLOCK)).setBackground(background, -1, -1);
        builder.addOutputSlot(90, 22).addItemStack(new ItemStack(BlockContent.TAINTED_REFINERY_BLOCK)).setBackground(background, -1, -1);
    }
    
    // Dummy record to serve as JEI "recipe" type
    public record CreationInfo() {}
}
