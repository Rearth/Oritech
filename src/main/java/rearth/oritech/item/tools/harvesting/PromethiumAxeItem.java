package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.BlockState;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;

public class PromethiumAxeItem extends AxeItem {
    public PromethiumAxeItem(ToolMaterial material, float attackDamage, float attackSpeed) {
        super(material, attackDamage, attackSpeed, new Settings().maxDamage(-1).maxCount(1));
    }
    
    @Override
    public boolean isSuitableFor(BlockState state) {
        return Items.DIAMOND_AXE.isSuitableFor(state)
                 || Items.DIAMOND_SWORD.isSuitableFor(state)
                 || Items.SHEARS.isSuitableFor(state);
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isDamageable() {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
