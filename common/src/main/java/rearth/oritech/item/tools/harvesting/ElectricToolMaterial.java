package rearth.oritech.item.tools.harvesting;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class ElectricToolMaterial implements Tier {
    @Override
    public int getUses() {
        return 1000;
    }
    
    @Override
    public float getSpeed() {
        return 9f;
    }
    
    @Override
    public float getAttackDamageBonus() {
        return 3.0f;
    }
    
    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return Tiers.NETHERITE.getIncorrectBlocksForDrops();
    }
    
    @Override
    public int getEnchantmentValue() {
        return 22;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT);
    }
}
