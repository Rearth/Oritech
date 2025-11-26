package rearth.oritech.init.compat.jei;

import java.util.List;
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
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

public class OritechJeiParticleCollisionRecipe implements IRecipeCategory<OritechRecipe> {
    
    public final OritechRecipeType type;
    public final IDrawable icon;
    public final IDrawableAnimated arrow;
    public final IDrawableStatic background;
    
    public OritechJeiParticleCollisionRecipe(IGuiHelper helper) {
        this.type = RecipeContent.PARTICLE_COLLISION;
        this.icon = helper.createDrawableItemStack(new ItemStack(BlockContent.ACCELERATOR_CONTROLLER.asItem()));
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        
    }
    
    @Override
    public @NotNull RecipeType<OritechRecipe> getRecipeType() {
        return RecipeType.create(type.getIdentifier().getNamespace(), type.getIdentifier().getPath(), OritechRecipe.class);
    }
    
    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("emi.category.oritech." + type.getIdentifier().getPath());
    }
    
    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }
    
    @Override
    public int getWidth() {
        return 170;
    }
    
    @Override
    public int getHeight() {
        return 66;
    }
    
    @Override
    public void draw(OritechRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        
        arrow.draw(guiGraphics, 80 - 23, 41 - 17);
        
        // data
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.title.oritech.collisionspeed", recipe.getTime()), 2, (int) (getHeight() * 0.88), 0xffffff);
        
    }
    
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OritechRecipe recipe, IFocusGroup focuses) {
        
        var inputs = recipe.getInputs();
        
        builder.addInputSlot(25, 10).addIngredients(inputs.get(0)).setBackground(background, -1, -1);
        builder.addInputSlot(25, 33).addIngredients(inputs.get(1)).setBackground(background, -1, -1);
        builder.addOutputSlot(90, 24).addItemStack(recipe.getResults().get(0)).setBackground(background, -1, -1);
    }
}
