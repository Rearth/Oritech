package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

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
    public TagKey<Block> getInverseTag() {
        return ToolMaterials.NETHERITE.getInverseTag();
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
