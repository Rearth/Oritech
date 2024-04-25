package rearth.oritech.item.tools.harvesting;

import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class ChainsawItem extends AxeItem implements SimpleEnergyItem {
    
    public static final int BAR_STEP_COUNT = 13;
    private final float energyUsageMultiplier = 10f;
    
    public ChainsawItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }
    
    @Override
    public boolean isSuitableFor(BlockState state) {
        return Items.DIAMOND_AXE.isSuitableFor(state)
                 || Items.DIAMOND_SWORD.isSuitableFor(state)
                 || Items.SHEARS.isSuitableFor(state);
    }
    
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        var amount = state.getBlock().getHardness();
        var random = Random.create();
        int unbreakingLevel = EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
        if (unbreakingLevel > 0) {
            amount = amount / (random.nextInt(unbreakingLevel) + 1);
        }
        
        amount *= energyUsageMultiplier;
        amount = Math.min(amount, this.getStoredEnergy(stack));
        
        return this.tryUseEnergy(stack, (long) amount);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var text = Text.literal(String.format("%o/%o RF", this.getStoredEnergy(stack), this.getEnergyCapacity(stack)));
        tooltip.add(text.formatted(Formatting.GOLD));
    }
    
    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        var enoughEnergy = getStoredEnergy(stack) >= state.getBlock().getHardness() * energyUsageMultiplier;
        var multiplier = enoughEnergy ? 1 : 0.3f;
        return super.getMiningSpeedMultiplier(stack, state) * multiplier;
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return 10000;
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return 100;
    }
    
    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
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
    
    @Override
    public int getItemBarStep(ItemStack stack) {
        var energyItem = (SimpleEnergyItem) stack.getItem();
        return Math.round((energyItem.getStoredEnergy(stack) * 100f / energyItem.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Enchantment enchantment, EnchantingContext context) {
        return enchantment.target == EnchantmentTarget.WEAPON || enchantment.target == EnchantmentTarget.BREAKABLE || super.canBeEnchantedWith(stack, enchantment, context);
    }
    
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
}
