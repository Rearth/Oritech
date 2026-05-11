package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import rearth.oritech.Oritech;

public class OritechEmiTaintedRefineryBonuses extends BasicEmiRecipe {
    
    private final String bonusType;
    private final String descriptionKey;
    
    public OritechEmiTaintedRefineryBonuses(EmiRecipeCategory category, TagKey<Block> blockTag, String bonusType, String descriptionKey) {
        super(category, Oritech.id("/tainted_refinery_bonuses/" + bonusType), 160, 90);
        this.bonusType = bonusType;
        this.descriptionKey = descriptionKey;
        
        var tagEntries = BuiltInRegistries.BLOCK.getTag(blockTag);
        if (tagEntries.isPresent()) {
            tagEntries.get().forEach(holder -> this.inputs.add(EmiStack.of(holder.value())));
        }
    }
    
    @Override
    public void addWidgets(WidgetHolder widgets) {
        // title
        widgets.addText(Component.translatable("emi.title.oritech.tainted_bonus." + bonusType), 2, 2, 0xFFFFFF, true);
        
        // block grid (6 columns)
        var cols = 8;
        for (int i = 0; i < inputs.size(); i++) {
            var col = i % cols;
            var row = i / cols;
            widgets.addSlot(inputs.get(i), 2 + col * 18, 14 + row * 18).drawBack(true);
        }
        
        // description text at bottom
        var rows = (inputs.size() + cols - 1) / cols;
        widgets.addText(Component.translatable(descriptionKey), 2, 16 + rows * 18, 0xFFFFFF, true);
    }
    
    @Override
    public int getDisplayHeight() {
        var cols = 8;
        var rows = (inputs.size() + cols - 1) / cols;
        return 16 + rows * 18 + 22;
    }
}
