package rearth.oritech.item.tools.harvesting;

import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class ElectricToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 1000;
    }
    
    @Override
    public float getMiningSpeedMultiplier() {
        return 9f;
    }
    
    @Override
    public float getAttackDamage() {
        return 3.0f;
    }
    
    @Override
    public int getMiningLevel() {
        return 3;
    }
    
    @Override
    public int getEnchantability() {
        return 22;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.NETHERITE_INGOT);
    }
}
