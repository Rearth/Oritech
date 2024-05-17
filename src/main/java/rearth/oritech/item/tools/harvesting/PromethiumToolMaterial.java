package rearth.oritech.item.tools.harvesting;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class PromethiumToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 10000;
    }
    
    @Override
    public float getMiningSpeedMultiplier() {
        return 18f;
    }
    
    @Override
    public float getAttackDamage() {
        return 5.0f;
    }
    
    @Override
    public int getMiningLevel() {
        return MiningLevels.NETHERITE;
    }
    
    @Override
    public int getEnchantability() {
        return 28;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.NETHERITE_INGOT);
    }
}
