package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;

public class OritechEmiTaintedRefineryCreation extends BasicEmiRecipe {
    
    public OritechEmiTaintedRefineryCreation(EmiRecipeCategory category) {
        super(category, Oritech.id("/tainted_refinery_creation/info"), 150, 90);
        
        this.inputs.add(EmiStack.of(BlockContent.REFINERY_BLOCK));
        this.inputs.add(EmiStack.of(BlockContent.ENCHANTMENT_CATALYST_BLOCK));
        this.outputs.add(EmiStack.of(BlockContent.TAINTED_REFINERY_BLOCK));
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        // input: refinery
        widgets.addSlot(this.inputs.get(0), 10, 10);
        
        // input: catalyst (with souls)
        widgets.addSlot(this.inputs.get(1), 10, 35);
        
        // arrow
        widgets.addFillingArrow(50, 22, 3000);
        
        // output: tainted refinery
        widgets.addSlot(this.outputs.get(0), 90, 22).recipeContext(this);
        
        // description text
        widgets.addText(Component.translatable("emi.title.oritech.tainted_creation_hint"), 0, (int) (getDisplayHeight() * 0.77), 0xFFFFFF, true);
        widgets.addText(Component.translatable("emi.title.oritech.tainted_creation_hint2"), 0, (int) (getDisplayHeight() * 0.88), 0xFFFFFF, true);
    }
}
