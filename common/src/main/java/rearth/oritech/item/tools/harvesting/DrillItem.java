package rearth.oritech.item.tools.harvesting;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DrillItem extends DiggerItem implements OritechEnergyItem {
    
    public static final int BAR_STEP_COUNT = 13;
    private final float energyUsageMultiplier = Oritech.CONFIG.basicDrill.energyUsage();
    
    public DrillItem(Tier toolMaterial, TagKey<Block> effectiveBlocks, Item.Properties settings) {
        super(toolMaterial, effectiveBlocks, settings);
    }
    
    // this overrides the fabric specific extensions
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return true;
    }
    
    // this overrides the neoforge specific extensions
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }
    
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!(miner instanceof Player player)) return true;
        
        var amount = state.getBlock().defaultDestroyTime() * energyUsageMultiplier;
        amount = Math.min(amount, this.getStoredEnergy(stack));
        
        return this.tryUseEnergy(stack, (long) amount, player);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var text = Component.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.withStyle(ChatFormatting.GOLD));
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        var enoughEnergy = getStoredEnergy(stack) >= state.getBlock().defaultDestroyTime() * energyUsageMultiplier;
        var multiplier = enoughEnergy ? 1 : 0.1f;
        return super.getDestroySpeed(stack, state) * multiplier;
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.basicDrill.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.basicDrill.chargeSpeed();
    }
}
