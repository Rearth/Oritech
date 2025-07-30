package rearth.oritech.item.tools.harvesting;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import java.util.Deque;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Tool.Rule;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ChainsawItem extends AxeItem implements OritechEnergyItem {
    
    public static final int BAR_STEP_COUNT = 13;
    private final float energyUsageMultiplier = Oritech.CONFIG.chainSaw.energyUsage();
    
    public ChainsawItem(Tier toolMaterial, Item.Properties settings) {
        super(toolMaterial, settings);
        // a bit of a hack, but set tool components again after super()
        // this lets ChainsawItem extend AxeItem (for the right-click actions) and still ignore
        // the default tool components set up by AxeItem
        var toolComponent = new Tool(List.of(
            Rule.deniesDrops(toolMaterial.getIncorrectBlocksForDrops()),
            Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, toolMaterial.getSpeed()),
            Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F),
            Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F)),
            1.0F, 1);
        this.components = settings.component(DataComponents.TOOL, toolComponent).buildAndValidateComponents();
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
        
        var energySuccess = this.tryUseEnergy(stack, (long) amount, player);
        
        if (!world.isClientSide && miner.isShiftKeyDown() && energySuccess && Oritech.CONFIG.chainsawTreeCutting()) {
            var startPos = pos.above();
            var startState = world.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                PromethiumAxeItem.pendingBlocks.addAll(treeBlocks.stream().map(elem -> new Tuple<>(world, elem)).toList());
                
                var extraEnergyUsed = treeBlocks.size() * energyUsageMultiplier / 2;
                this.tryUseEnergy(stack, (long) extraEnergyUsed, player);
            }
        }
        
        return energySuccess;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        var text = Component.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(stack), this.getEnergyCapacity(stack));
        tooltip.add(text.withStyle(ChatFormatting.GOLD));
        
        if (Oritech.CONFIG.chainsawTreeCutting())
            tooltip.add(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
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
        return Oritech.CONFIG.chainSaw.energyCapacity();
    }
    
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return Oritech.CONFIG.chainSaw.chargeSpeed();
    }
}
