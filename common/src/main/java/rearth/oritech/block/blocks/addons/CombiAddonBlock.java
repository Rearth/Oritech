package rearth.oritech.block.blocks.addons;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.addons.CombiAddonEntity;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.TooltipHelper;

import java.util.ArrayList;
import java.util.List;

public class CombiAddonBlock extends MachineAddonBlock {
    
    public CombiAddonBlock(Properties settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return CombiAddonEntity.class;
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        
        if (!world.isClientSide() && itemStack.has(ComponentContent.ADDON_DATA.get()) && world.getBlockEntity(pos) instanceof CombiAddonEntity combiAddonEntity) {
            combiAddonEntity.storedData = itemStack.get(ComponentContent.ADDON_DATA.get());
        }
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            
            if (!stack.has(ComponentContent.ADDON_DATA.get())) return;
            
            var data = stack.get(ComponentContent.ADDON_DATA.get());
            var foundTexts = getShrinkTooltip(data);
            
            tooltip.add(Component.translatable("tooltip.oritech.combi_addon_desc").withStyle(ChatFormatting.GRAY));
            
            tooltip.addAll(foundTexts);
            
        }
    }
    
    public static @NotNull ArrayList<Component> getShrinkTooltip(ShrinkerBlockEntity.ShrunkAddonData data) {
        var usedSettings = data.data();
        var foundTexts = new ArrayList<Component>();
        
        if (usedSettings.speed() != 1) {
            var displayedNumber = Math.round((1 / usedSettings.speed() - 1) * 100);
            foundTexts.add(Component.translatable("tooltip.oritech.addon_speed_desc").withStyle(ChatFormatting.DARK_GRAY)
                          .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
        }
        
        if (usedSettings.efficiency() != 1) {
            var displayedNumber = Math.round((1 - usedSettings.efficiency()) * 100);
            foundTexts.add(Component.translatable("tooltip.oritech.addon_efficiency_desc").withStyle(ChatFormatting.DARK_GRAY)
                          .append(TooltipHelper.getFormattedValueChangeTooltip(displayedNumber)));
        }
        
        if (usedSettings.energyBonusCapacity() != 0) {
            foundTexts.add(
              Component.translatable("tooltip.oritech.addon_capacity_desc").withStyle(ChatFormatting.DARK_GRAY)
                .append(TooltipHelper.getFormattedEnergyChangeTooltip(usedSettings.energyBonusCapacity(), " RF")));
        }
        
        if (usedSettings.energyBonusTransfer() != 0) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_transfer_desc").withStyle(ChatFormatting.DARK_GRAY)
                          .append(TooltipHelper.getFormattedEnergyChangeTooltip(usedSettings.energyBonusTransfer(), " RF/t")));
        }
        
        if (usedSettings.maxBurstTicks() != 0) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_burst", usedSettings.maxBurstTicks()).withStyle(ChatFormatting.GRAY));
        }
        
        if (data.cropFilter()) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_crop").withStyle(ChatFormatting.GRAY));
        }
        
        if (data.fluid()) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_fluid").withStyle(ChatFormatting.GRAY));
        }
        
        if (data.quarryCount() > 0) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_quarry", data.quarryCount()).withStyle(ChatFormatting.GRAY));
        }
        if (data.yieldCount() > 0) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_yield", data.yieldCount()).withStyle(ChatFormatting.GRAY));
        }
        if (data.silk()) {
            foundTexts.add(Component.translatable("tooltip.oritech.addon_combi_silk").withStyle(ChatFormatting.GRAY));
        }
        return foundTexts;
    }
}
