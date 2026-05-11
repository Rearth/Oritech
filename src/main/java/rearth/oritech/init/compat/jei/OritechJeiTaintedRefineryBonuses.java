package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.init.BlockContent;

import java.util.ArrayList;
import java.util.List;

public class OritechJeiTaintedRefineryBonuses implements IRecipeCategory<OritechJeiTaintedRefineryBonuses.BonusInfo> {
    
    public static final RecipeType<BonusInfo> RECIPE_TYPE = RecipeType.create("oritech", "tainted_refinery_bonuses", BonusInfo.class);
    
    public final IDrawable icon;
    public final IDrawable background;
    
    public OritechJeiTaintedRefineryBonuses(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(BlockContent.TAINTED_REFINERY_BLOCK.asItem()));
        this.background = helper.getSlotDrawable();
    }
    
    @Override
    public @NotNull RecipeType<BonusInfo> getRecipeType() {
        return RECIPE_TYPE;
    }
    
    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("emi.category.oritech.tainted_refinery_bonuses");
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
        return 90;
    }
    
    @Override
    public void draw(BonusInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // title
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.title.oritech.tainted_bonus." + recipe.bonusType()), 2, 2, 0xFFFFFF);
        
        // description at bottom
        var cols = 8;
        var rows = (recipe.blocks().size() + cols - 1) / cols;
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.description.oritech.tainted_bonus." + recipe.bonusType()), 2, 16 + rows * 18, 0xFFFFFF);
    }
    
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BonusInfo recipe, IFocusGroup focuses) {
        var cols = 8;
        for (int i = 0; i < recipe.blocks().size(); i++) {
            var col = i % cols;
            var row = i / cols;
            builder.addInputSlot(2 + col * 18, 14 + row * 18)
                .addItemStack(new ItemStack(recipe.blocks().get(i)))
                .setBackground(background, -1, -1);
        }
    }
    
    public record BonusInfo(String bonusType, List<Block> blocks) {
        public static BonusInfo fromTag(TagKey<Block> tag, String bonusType) {
            var blocks = new ArrayList<Block>();
            var tagEntries = BuiltInRegistries.BLOCK.getTag(tag);
            if (tagEntries.isPresent()) {
                tagEntries.get().forEach(holder -> blocks.add(holder.value()));
            }
            return new BonusInfo(bonusType, blocks);
        }
    }
}
