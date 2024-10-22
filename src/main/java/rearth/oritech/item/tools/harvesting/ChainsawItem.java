package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.component.type.ToolComponent.Rule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class ChainsawItem extends AxeItem implements OritechEnergyItem {
    
    public static final int BAR_STEP_COUNT = 13;
    private final float energyUsageMultiplier = Oritech.CONFIG.chainSaw.energyUsage();
    
    public ChainsawItem(ToolMaterial toolMaterial, Item.Settings settings) {
        super(toolMaterial, settings);
        // a bit of a hack, but set tool components again after super()
        // this lets ChainsawItem extend AxeItem (for the right-click actions) and still ignore
        // the default tool components set up by AxeItem
        var toolComponent = new ToolComponent(List.of(
            Rule.ofNeverDropping(toolMaterial.getInverseTag()),
            Rule.ofAlwaysDropping(BlockTags.AXE_MINEABLE, toolMaterial.getMiningSpeedMultiplier()),
            Rule.of(BlockTags.SWORD_EFFICIENT, 1.5F),
            Rule.ofAlwaysDropping(List.of(Blocks.COBWEB), 15.0F)),
            1.0F, 1);
        this.components = settings.component(DataComponentTypes.TOOL, toolComponent).getValidatedComponents();
    }
    
    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        var amount = state.getBlock().getHardness() * energyUsageMultiplier;
        amount = Math.min(amount, this.getStoredEnergy(stack));
        
        return this.tryUseEnergy(stack, (long) amount);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var text = Text.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.formatted(Formatting.GOLD));
    }
    
    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        var enoughEnergy = getStoredEnergy(stack) >= state.getBlock().getHardness() * energyUsageMultiplier;
        var multiplier = enoughEnergy ? 1 : 0.1f;
        return super.getMiningSpeed(stack, state) * multiplier;
    }
    
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
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
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }
    
    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xff7007;
    }
    
    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return Oritech.CONFIG.chainSaw.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.chainSaw.chargeSpeed();
    }
}
