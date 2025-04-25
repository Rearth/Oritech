package rearth.oritech.item.tools.harvesting;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.component.type.ToolComponent.Rule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
import rearth.oritech.item.tools.util.OritechEnergyItem;

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
    
    // this overrides the fabric specific extensions
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    
    public boolean allowContinuingBlockBreaking(PlayerEntity player, ItemStack oldStack, ItemStack newStack) {
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
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!(miner instanceof PlayerEntity player)) return true;
        
        var amount = state.getBlock().getHardness() * energyUsageMultiplier;
        amount = Math.min(amount, this.getStoredEnergy(stack));
        
        var energySuccess = this.tryUseEnergy(stack, (long) amount, player);
        
        if (!world.isClient && miner.isSneaking() && energySuccess && Oritech.CONFIG.chainsawTreeCutting()) {
            var startPos = pos.up();
            var startState = world.getBlockState(startPos);
            if (startState.isIn(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                PromethiumAxeItem.pendingBlocks.addAll(treeBlocks.stream().map(elem -> new Pair<>(world, elem)).toList());
                
                var extraEnergyUsed = treeBlocks.size() * energyUsageMultiplier / 2;
                this.tryUseEnergy(stack, (long) extraEnergyUsed, player);
            }
        }
        
        return energySuccess;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var text = Text.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.formatted(Formatting.GOLD));
        
        if (Oritech.CONFIG.chainsawTreeCutting())
            tooltip.add(Text.translatable("tooltip.oritech.promethium_axe").formatted(Formatting.DARK_GRAY));
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
        return Math.round((getStoredEnergy(stack) * 100f / this.getEnergyCapacity(stack)) * BAR_STEP_COUNT) / 100;
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
