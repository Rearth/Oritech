package rearth.oritech.item.tools.harvesting;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.item.tools.util.OritechEnergyItem;

import java.util.function.Consumer;

public class DrillItem extends Item implements OritechEnergyItem {

    public static final int BAR_STEP_COUNT = 13;

    public DrillItem(Properties properties) {
        super(properties);
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
        return OritechStartupConfig.basicDrill.energyUsage.get();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {

        if (!(miner instanceof Player player)) return true;

        var amount = state.getBlock().defaultDestroyTime() * getEnergyUsageMultiplier();
        amount = Math.min(amount, this.getStoredEnergy(stack, ItemAccess.forStack(stack)));

        return this.tryUseEnergy(stack, (int) amount, player);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        var text = Component.translatable("tooltip.oritech.energy_indicator", this.getStoredEnergy(itemStack, ItemAccess.forStack(itemStack)), this.getEnergyCapacity());
        builder.accept(text.withStyle(ChatFormatting.GOLD));
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
        return OritechStartupConfig.basicDrill.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.basicDrill.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        return OritechStartupConfig.basicDrill.chargeSpeed.get();
    }
}
