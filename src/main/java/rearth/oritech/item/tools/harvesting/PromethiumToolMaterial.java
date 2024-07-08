package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

public class PromethiumToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 10000;
    }
    
    @Override
    public float getMiningSpeedMultiplier() {
        return 24f;
    }
    
    @Override
    public float getAttackDamage() {
        return 5.0f;
    }
    
    @Override
    public TagKey<Block> getInverseTag() {
        return ToolMaterials.NETHERITE.getInverseTag();
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
