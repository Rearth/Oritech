package rearth.oritech.init.compat.emi;

import dev.architectury.platform.Platform;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;

public class OritechEMIRecipe extends BasicEmiRecipe {
    
    private final Boolean isGenerator;
    private final List<ScreenProvider.GuiSlot> slots;
    private final InventorySlotAssignment slotOffsets;
    private final OritechRecipe recipe;
    
    public OritechEMIRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category, Class<? extends MachineBlockEntity> screenProviderSource, BlockState machineState) {
        super(category, entry.id(), 150, 66);
        
        
        var fluidDivider = Platform.isNeoForge() ? 81 : 1;  // no idea why this is needed
        
        recipe = entry.value();
        recipe.getInputs().forEach(ingredient -> this.inputs.add(EmiIngredient.of(ingredient)));
        recipe.getResults().forEach(stack -> this.outputs.add(EmiStack.of(stack)));
        
        if (recipe.getFluidInput() != null)
            this.inputs.add(EmiStack.of(recipe.getFluidInput().getFluid(), Math.max(recipe.getFluidInput().getAmount() / fluidDivider, 1)));
        if (recipe.getFluidOutput() != null)
            this.outputs.add(EmiStack.of(recipe.getFluidOutput().getFluid(), Math.max(recipe.getFluidInput().getAmount() / fluidDivider, 1)));
        
        try {
            var screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), machineState);
            this.isGenerator = screenProvider instanceof UpgradableGeneratorBlockEntity;
            this.slots = screenProvider.getGuiSlots();
            this.slotOffsets = screenProvider.getSlotAssignments();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    public OritechEMIRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category, Boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment slotOffsets) {
        super(category, entry.id(), 150, 66);
        
        this.isGenerator = isGenerator;
        this.slots = slots;
        this.slotOffsets = slotOffsets;
        
        
        var fluidDivider = Platform.isNeoForge() ? 81 : 1;  // no idea why this is needed
        
        recipe = entry.value();
        recipe.getInputs().forEach(ingredient -> this.inputs.add(EmiIngredient.of(ingredient)));
        recipe.getResults().forEach(stack -> this.outputs.add(EmiStack.of(stack)));
        
        if (recipe.getFluidInput() != null)
            this.inputs.add(EmiStack.of(recipe.getFluidInput().getFluid(), recipe.getFluidInput().getAmount() / fluidDivider));
        if (recipe.getFluidOutput() != null)
            this.outputs.add(EmiStack.of(recipe.getFluidOutput().getFluid(), recipe.getFluidInput().getAmount() / fluidDivider));
            
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        
        var offsetX = 23;
        var offsetY = 17;
        
        // central arrow/flame
        if (isGenerator) {
            widgets.addTexture(EmiTexture.FULL_FLAME, 76 - offsetX, 41 - offsetY);
        } else {
            widgets.addFillingArrow(80 - offsetX, 41 - offsetY, 3000);
        }
        
        // inputs
        var emiIngredients = this.inputs;
        for (int i = 0; i < emiIngredients.size(); i++) {
            var input = emiIngredients.get(i);
            if (input.isEmpty()) continue;
            
            var isFluid = input.getEmiStacks().stream().anyMatch(stack -> stack.getKey() instanceof Fluid);
            if (isFluid && input.getAmount() > 0) {
                widgets.addTank(input, 10, 6, 18, 50, (int) input.getAmount()).drawBack(false);
                widgets.addTexture(GUI_COMPONENTS, 10, 6, 18, 50, 48, 0, 14, 50, 98, 96);
            } else {
                var pos = slots.get(slotOffsets.inputStart() + i);
                widgets.addSlot(input, pos.x() - offsetX, pos.y() - offsetY);
            }
        }
        
        // outputs
        var emiStacks = this.outputs;
        for (int i = 0; i < emiStacks.size(); i++) {
            var result = emiStacks.get(i);
            if (result.isEmpty() || result.getAmount() <= 0) continue;
            
            var isFluid = result.getEmiStacks().stream().anyMatch(stack -> stack.getKey() instanceof Fluid);
            if (isFluid && result.getAmount() > 0) {
                widgets.addTank(result, 120, 6, 18, 50, (int) result.getAmount()).drawBack(false);
                widgets.addTexture(GUI_COMPONENTS, 120, 6, 18, 50, 48, 0, 14, 50, 98, 96);
            } else {
                var pos = slots.get(slotOffsets.outputStart() + i);
                widgets.addSlot(result, pos.x() - offsetX, pos.y() - offsetY).recipeContext(this);
            }
        }
        
        // data
        var duration = String.format("%.0f", recipe.getTime() / 20f);
        widgets.addText(Text.translatable("emi.title.oritech.cookingtime", duration, recipe.getTime()), (int) (getDisplayWidth() * 0.35), (int) (getDisplayHeight() * 0.88), 0xFFFFFF, true);
        
    }
}
