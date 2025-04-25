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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

public class OritechJeiLaserRecipe implements IRecipeCategory<OritechRecipe> {
    
    public final OritechRecipeType type;
    public final IDrawable icon;
    public final IDrawableAnimated arrow;
    public final IDrawableStatic background;
    public final IDrawableStatic laserBackground;
    
    public static final Identifier LASER_RECIPE_OVERLAY = Oritech.id("textures/gui/modular/laser_recipe_background_jei.png");
    
    public OritechJeiLaserRecipe(IGuiHelper helper) {
        this.type = RecipeContent.LASER;
        this.icon = helper.createDrawableItemStack(new ItemStack(BlockContent.LASER_ARM_BLOCK.asItem()));
        
        // using low res texture because JEI is stupid I don't know how to scale it properly
        this.laserBackground = helper.drawableBuilder(LASER_RECIPE_OVERLAY, 0, 0, 80, 80).setTextureSize(80, 80).build();
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        
    }
    
    @Override
    public @NotNull RecipeType<OritechRecipe> getRecipeType() {
        return RecipeType.create(type.getIdentifier().getNamespace(), type.getIdentifier().getPath(), OritechRecipe.class);
    }
    
    @Override
    public @NotNull Text getTitle() {
        return Text.translatable("emi.category.oritech." + type.getIdentifier().getPath());
    }
    
    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }
    
    @Override
    public int getWidth() {
        return 165;
    }
    
    @Override
    public int getHeight() {
        return 80;
    }
    
    @Override
    public void draw(OritechRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext guiGraphics, double mouseX, double mouseY) {
        
        arrow.draw(guiGraphics, 105, 15);
        laserBackground.draw(guiGraphics, 10, 5);
    }
    
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OritechRecipe recipe, IFocusGroup focuses) {
        
        var inputs = recipe.getInputs();
        
        builder.addInputSlot(80, 15).addIngredients(inputs.get(0)).setBackground(background, -1, -1);
        builder.addOutputSlot(135, 15).addItemStack(recipe.getResults().get(0)).setBackground(background, -1, -1);
    }
}
