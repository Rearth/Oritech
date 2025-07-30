package rearth.oritech.item.tools.harvesting;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class PromethiumToolMaterial implements Tier {
    @Override
    public int getUses() {
        return 10000;
    }
    
    @Override
    public float getSpeed() {
        return 24f;
    }
    
    @Override
    public float getAttackDamageBonus() {
        return 5.0f;
    }
    
    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return Tiers.NETHERITE.getIncorrectBlocksForDrops();
    }
    
    @Override
    public int getEnchantmentValue() {
        return 28;
    }
    
    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT);
    }
}
