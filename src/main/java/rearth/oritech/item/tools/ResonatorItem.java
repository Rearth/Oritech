package rearth.oritech.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import rearth.oritech.config.OritechStartupConfig;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.item.tools.util.OritechEnergyItem;
import rearth.oritech.util.ResonatorSearch;
import rearth.oritech.util.TooltipHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ResonatorItem extends Item implements OritechEnergyItem {
    private final Predicate<BlockState> targets;
    private final String translationKey;
    private final int particleColor;

    public ResonatorItem(Properties properties, TagKey<Block> targets, String translationKey, int particleColor) {
        this(properties, state -> state.is(targets), translationKey, particleColor);
    }

    public ResonatorItem(Properties properties, Predicate<BlockState> targets, String translationKey, int particleColor) {
        super(properties.enchantable(10));
        this.targets = targets;
        this.translationKey = translationKey;
        this.particleColor = particleColor;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = player.getItemInHand(hand);
            var lastResult = stack.get(ComponentContent.TARGET_GLOBAL_POSITION.get());
            if (!player.isShiftKeyDown() && lastResult != null) {
                if (lastResult.dimension().equals(level.dimension())
                        && !consumeEnergy(stack, OritechStartupConfig.resonator.guidanceEnergyUsage.get(), serverPlayer)) {
                    return InteractionResult.FAIL;
                }
                ResonatorSearch.pointTo(serverPlayer, lastResult, translationKey, particleColor);
                player.getCooldowns().addCooldown(stack, 10);
            } else if (!ResonatorSearch.isSearching(serverPlayer)) {
                if (!consumeEnergy(stack, OritechStartupConfig.resonator.searchEnergyUsage.get(), serverPlayer)) {
                    return InteractionResult.FAIL;
                }
                // Energy transactions can replace the inventory stack. Search the
                // current instance so the queued job still recognizes its owner.
                stack = player.getItemInHand(hand);
                if (ResonatorSearch.start(serverPlayer, stack, targets, translationKey, particleColor)) {
                    player.getCooldowns().addCooldown(stack, 100);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private boolean consumeEnergy(ItemStack stack, int amount, ServerPlayer player) {
        if (tryUseEnergy(stack, amount, player)) return true;
        player.sendSystemMessage(Component.translatable("message.oritech.resonator.no_energy"), true);
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public int getEnergyCapacity() {
        return OritechStartupConfig.resonator.energyCapacity.get();
    }

    @Override
    public int getMaxRFInputRate() {
        return OritechStartupConfig.resonator.chargeSpeed.get();
    }

    @Override
    public int getMaxRFOutputRate() {
        // A search spends its whole cost at once, independently of the charging rate.
        return getEnergyCapacity();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(13, Math.round(getStoredEnergy(stack, ItemAccess.forStack(stack)) * 13f / getEnergyCapacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xff7007;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.oritech.energy_indicator",
                TooltipHelper.getEnergyText(getStoredEnergy(stack, ItemAccess.forStack(stack))),
                TooltipHelper.getEnergyText(getEnergyCapacity())).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.oritech." + translationKey).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech.resonator.controls").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.oritech.resonator.new_search").withStyle(ChatFormatting.GRAY));
    }
}
