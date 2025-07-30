package rearth.oritech.init.compat.jei;

import dev.architectury.fluid.FluidStack;
import dev.architectury.platform.Platform;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.FluidIngredient;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.ScreenProvider.GuiSlot;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;


public class OritechJeiRecipeCategory implements IRecipeCategory<OritechRecipe> {
    
    public final OritechRecipeType type;
    private final List<ScreenProvider.GuiSlot> slots;
    private final InventorySlotAssignment slotOffsets;
    public final IDrawable icon;
    public final IDrawableAnimated arrow;
    public final IDrawableStatic background;
    public final IDrawableStatic fluidBackground;
    private final ScreenProvider.ArrowConfiguration indicatorConfig;
    
    // JEI really feels like the worst of the 3 recipe viewers here
    public OritechJeiRecipeCategory(OritechRecipeType type, Class<? extends MachineBlockEntity> screenProviderSource, Block machine, IGuiHelper helper) {
        this.type = type;
        this.icon = helper.createDrawableItemStack(new ItemStack(machine.asItem()));
        
        try {
            var screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), machine.defaultBlockState());
            this.slots = screenProvider.getGuiSlots();
            this.slotOffsets = screenProvider.getSlotAssignments();
            this.indicatorConfig = screenProvider.getIndicatorConfiguration();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        this.fluidBackground = helper.drawableBuilder(GUI_COMPONENTS, 48, 0, 14, 50).setTextureSize(98, 96).build();
        
    }
    
    public OritechJeiRecipeCategory(OritechRecipeType type, Block machine, IGuiHelper helper, Boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment slotOffsets) {
        this.type = type;
        this.icon = helper.createDrawableItemStack(new ItemStack(machine.asItem()));
        
        this.arrow = helper.createAnimatedRecipeArrow(40);
        this.background = helper.getSlotDrawable();
        this.fluidBackground = helper.drawableBuilder(GUI_COMPONENTS, 48, 0, 14, 50).setTextureSize(98, 96).build();
        
        this.slots = slots;
        this.slotOffsets = slotOffsets;
        this.indicatorConfig = new ScreenProvider.ArrowConfiguration(
          Oritech.id("textures/gui/modular/arrow_empty.png"),
          Oritech.id("textures/gui/modular/arrow_full.png"),
          80, 35, 29, 16, true);
        
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
        return 150;
    }
    
    @Override
    public int getHeight() {
        return 70;
    }
    
    @Override
    public void draw(OritechRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        
        arrow.draw(guiGraphics, indicatorConfig.x() - 23, indicatorConfig.y() - 17);
        
        // data
        var duration = String.format("%.0f", recipe.getTime() / 20f);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("emi.title.oritech.cookingtime", duration, recipe.getTime()), (int) (getWidth() * 0.35), (int) (getHeight() * 0.9), 0xffffff);
        
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
            var usedY = Math.clamp(2, pos.y() - offsetY, getHeight() - 18 - 4);
            builder.addInputSlot(pos.x() - offsetX, usedY).addIngredients(input).setBackground(background, -1, -1);
        }
        
        // fluid inputs
        if (recipe.getFluidInput() != null && recipe.getFluidInput().amount() > 0) {
            var fluidIngredient = recipe.getFluidInput();
            var shownAmount = Math.max(1, fluidIngredient.amount());
            
            // no idea why this is needed, but the 'var inputSlot = ' seems to break the architectury transformer for some reason
            if (Platform.isModLoaded("jei")) {
                var inputSlot = builder.addInputSlot(10, 6).setBackground(fluidBackground, -2, -2).setFluidRenderer(shownAmount, false, 10, 46);
                for (var fluidStack : fluidIngredient.getFluidStacks()) {
                    inputSlot = inputSlot.addFluidStack(fluidStack.getFluid(), shownAmount);
            }
            }
        }
        
        // results
        var outputs = recipe.getResults();
        for (int i = 0; i < outputs.size(); i++) {
            var output = outputs.get(i);
            if (output.isEmpty()) continue;
            
            var pos = slots.get(slotOffsets.outputStart() + i);
            var usedY = Math.clamp(1, pos.y() - offsetY, getHeight() - 18 - 4);
            builder.addOutputSlot(pos.x() - offsetX, usedY).addItemStack(output).setBackground(background, -1, -1);
        }
        
        // fluid outputs
        var tankCount = 0;
        var tankStartX = recipe.getFluidOutputs().size() > 1 ? 80 : 120;
        for (var fluidResult : recipe.getFluidOutputs()) {
            if (fluidResult.isEmpty()) continue;
            builder.addOutputSlot(tankStartX + tankCount *  20, 6).addFluidStack(fluidResult.getFluid(), fluidResult.getAmount()).setBackground(fluidBackground, -2, -2).setFluidRenderer(fluidResult.getAmount(), false, 10, 46);
            tankCount++;
        }
    }
}
