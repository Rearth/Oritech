package rearth.oritech.init.compat.emi;

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

import java.lang.reflect.InvocationTargetException;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;

public class OritechEMIRecipe extends BasicEmiRecipe {
    
    private final MachineBlockEntity screenProvider;
    private final OritechRecipe recipe;
    
    public OritechEMIRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category, Class<? extends MachineBlockEntity> screenProviderSource, BlockState machineState) {
        super(category, entry.id(), 150, 66);
        
        recipe = entry.value();
        recipe.getInputs().forEach(ingredient -> this.inputs.add(EmiIngredient.of(ingredient)));
        recipe.getResults().forEach(stack -> this.outputs.add(EmiStack.of(stack)));
        
        if (recipe.getFluidInput() != null)
            this.inputs.add(EmiStack.of(recipe.getFluidInput().variant().getFluid(), recipe.getFluidInput().amount()));
        if (recipe.getFluidOutput() != null)
            this.outputs.add(EmiStack.of(recipe.getFluidOutput().variant().getFluid(), recipe.getFluidInput().amount()));
        
        try {
            this.screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), machineState);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        
        var slots = screenProvider.getGuiSlots();
        var slotOffsets = screenProvider.getSlots();
        var offsetX = 23;
        var offsetY = 17;
        
        // central arrow/flame
        var isGenerator = screenProvider instanceof UpgradableGeneratorBlockEntity;
        if (isGenerator) {
            widgets.addTexture(EmiTexture.FULL_FLAME, 76 - offsetX, 41 - offsetY);
        } else {
            widgets.addFillingArrow(80 - offsetX, 41 - offsetY, 3000);
        }
        
        // inputs
        var emiIngredients = this.inputs;
        for (int i = 0; i < emiIngredients.size(); i++) {
            var input = emiIngredients.get(i);
            
            var isFluid = input.getEmiStacks().stream().anyMatch(stack -> stack.getKey() instanceof Fluid);
            if (isFluid) {
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
            
            var isFluid = result.getEmiStacks().stream().anyMatch(stack -> stack.getKey() instanceof Fluid);
            if (isFluid) {
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
