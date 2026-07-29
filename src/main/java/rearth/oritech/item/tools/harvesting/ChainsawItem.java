package rearth.oritech.item.tools.harvesting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.interaction.TreeCutterBlockEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.item.tools.util.OritechEnergyItem;

import java.util.function.Consumer;

public class ChainsawItem extends AxeItem implements OritechEnergyItem {

    public static final int BAR_STEP_COUNT = 13;

    public ChainsawItem(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, Item.Properties properties) {
        super(material, attackDamageBaseline, attackSpeedBaseline, properties);
    }

    // this overrides the neoforge specific extensions
    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return false;
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    private long getEnergyUsageMultiplier() {
        return OritechStartupConfig.chainSaw.energyUsage.get();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {

        if (!(miner instanceof Player player)) return true;

        var amount = state.getBlock().defaultDestroyTime() * getEnergyUsageMultiplier();
        amount = Math.min(amount, this.getStoredEnergy(stack, ItemAccess.forStack(stack)));

        var energySuccess = this.tryUseEnergy(stack, (int) amount, player);

        if (!level.isClientSide() && miner.isShiftKeyDown() && energySuccess && OritechConfig.chainsawTreeCutting.get()) {
            var startPos = pos.above();
            var startState = level.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreeCutterBlockEntity.getTreeBlocks(startPos, level);
                PromethiumAxeItem.pendingBlocks.addAll(treeBlocks.stream().map(elem -> new PromethiumAxeItem.PendingBlock(level, elem, stack)).toList());

                var extraEnergyUsed = treeBlocks.size() * getEnergyUsageMultiplier() / 2;
                this.tryUseEnergy(stack, (int) extraEnergyUsed, player);
            }
        }

        return energySuccess;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        var text = Component.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(itemStack, ItemAccess.forStack(itemStack)), this.getEnergyCapacity());
        builder.accept(text.withStyle(ChatFormatting.GOLD));

        if (OritechConfig.chainsawTreeCutting.get())
            builder.accept(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        var enoughEnergy = getStoredEnergy(stack, ItemAccess.forStack(stack)) >= state.getBlock().defaultDestroyTime() * getEnergyUsageMultiplier();
        var multiplier = enoughEnergy ? 1 : 0.1f;
        return super.getDestroySpeed(stack, state) * multiplier;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((getStoredEnergy(stack, ItemAccess.forStack(stack)) * 100f / this.getEnergyCapacity()) * BAR_STEP_COUNT) / 100;
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
    public int getEnergyCapacity() {
        return OritechStartupConfig.chainSaw.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.chainSaw.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.chainSaw.chargeSpeed.get();
    }
}
