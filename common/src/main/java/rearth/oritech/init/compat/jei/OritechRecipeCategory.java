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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;

public class OritechRecipeCategory implements IRecipeCategory<OritechRecipe> {
    
    public final OritechRecipeType type;
    private final Boolean isGenerator;
    private final List<ScreenProvider.GuiSlot> slots;
    private final InventorySlotAssignment slotOffsets;
    public final IDrawable icon;
    public final IDrawableAnimated arrow;
    public final IDrawableStatic background;
    public final IDrawableStatic fluidBackground;
    
    // JEI really feels like the worst of the 3 recipe viewers here
    public OritechRecipeCategory(OritechRecipeType type, Class<? extends MachineBlockEntity> screenProviderSource, Block machine, IGuiHelper helper) {
        this.type = type;
        this.icon = helper.createDrawableItemStack(new ItemStack(machine.asItem()));
        
        try {
            var screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), machine.getDefaultState());
            this.isGenerator = screenProvider instanceof UpgradableGeneratorBlockEntity;
            this.slots = screenProvider.getGuiSlots();
            this.slotOffsets = screenProvider.getSlotAssignments();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        this.fluidBackground = helper.drawableBuilder(GUI_COMPONENTS, 48, 0, 14, 50).setTextureSize(98, 96).build();
        
    }
    
    public OritechRecipeCategory(OritechRecipeType type, Block machine, IGuiHelper helper, Boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment slotOffsets) {
        this.type = type;
        this.icon = helper.createDrawableItemStack(new ItemStack(machine.asItem()));
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        this.fluidBackground = helper.drawableBuilder(GUI_COMPONENTS, 48, 0, 14, 50).setTextureSize(98, 96).build();
        
        this.isGenerator = isGenerator;
        this.slots = slots;
        this.slotOffsets = slotOffsets;
        
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
        return 150;
    }
    
    @Override
    public int getHeight() {
        return 66;
    }
    
    @Override
    public void draw(OritechRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext guiGraphics, double mouseX, double mouseY) {
        
        arrow.draw(guiGraphics, 80 - 23, 41 - 17);
        
        // data
        var duration = String.format("%.0f", recipe.getTime() / 20f);
        guiGraphics.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.translatable("emi.title.oritech.cookingtime", duration, recipe.getTime()), (int) (getWidth() * 0.35), (int) (getHeight() * 0.88), 0xffffff);
        
    }
    
    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, OritechRecipe recipe, @NotNull IFocusGroup focuses) {
        
        var offsetX = 23;
        var offsetY = 17;
        
        // inputs
        var inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.isEmpty()) continue;
            
            var pos = slots.get(slotOffsets.inputStart() + i);
            builder.addInputSlot(pos.x() - offsetX, pos.y() - offsetY).addIngredients(input).setBackground(background, -1, -1);
        }
        
        // fluid inputs
        if (!(recipe.getFluidInput() != null && recipe.getFluidInput().isEmpty())) {
            var fluidIngredient = recipe.getFluidInput();
            var shownAmount = Math.max(1, fluidIngredient.amount());
            var inputSlot = builder.addInputSlot(10, 6);
            for (var fluidStack : fluidIngredient.getFluidStacks()) {
                inputSlot = inputSlot.addFluidStack(fluidStack.getFluid(), shownAmount);
            }
            inputSlot.setBackground(fluidBackground, -2, -2).setFluidRenderer(shownAmount, false, 10, 46);
        }
        
        // results
        var outputs = recipe.getResults();
        for (int i = 0; i < outputs.size(); i++) {
            var output = outputs.get(i);
            if (output.isEmpty()) continue;
            
            var pos = slots.get(slotOffsets.outputStart() + i);
            builder.addOutputSlot(pos.x() - offsetX, pos.y() - offsetY).addItemStack(output).setBackground(background, -1, -1);
        }
        
        // fluid outputs
        if (!(recipe.getFluidOutput() != null && recipe.getFluidOutput().isEmpty())) {
            var stack = recipe.getFluidOutput();
            builder.addOutputSlot(120, 6).addFluidStack(stack.getFluid(), stack.getAmount()).setBackground(fluidBackground, -2, -2).setFluidRenderer(stack.getAmount(), false, 10, 46);
        }
    }
}
