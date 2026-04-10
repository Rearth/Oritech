package rearth.oritech.init.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Display for tainted refinery info entries (not backed by a real recipe).
 */
public class TaintedRefineryInfoDisplay implements Display {
    
    private final CategoryIdentifier<?> categoryId;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;
    private final String bonusType;
    
    // For creation display
    public TaintedRefineryInfoDisplay(CategoryIdentifier<?> categoryId, List<ItemStack> inputStacks, List<ItemStack> outputStacks) {
        this.categoryId = categoryId;
        this.inputs = inputStacks.stream().map(EntryIngredients::of).map(e -> (EntryIngredient) e).toList();
        this.outputs = outputStacks.stream().map(EntryIngredients::of).map(e -> (EntryIngredient) e).toList();
        this.bonusType = null;
    }
    
    // For bonus display
    public TaintedRefineryInfoDisplay(CategoryIdentifier<?> categoryId, TagKey<Block> blockTag, String bonusType) {
        this.categoryId = categoryId;
        this.bonusType = bonusType;
        
        var inputList = new ArrayList<EntryIngredient>();
        var tagEntries = BuiltInRegistries.BLOCK.getTag(blockTag);
        if (tagEntries.isPresent()) {
            tagEntries.get().forEach(holder -> inputList.add(EntryIngredients.of(holder.value())));
        }
        this.inputs = inputList;
        this.outputs = List.of();
    }
    
    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }
    
    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }
    
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return categoryId;
    }
    
    public String getBonusType() {
        return bonusType;
    }
}
