package rearth.oritech.util;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.base.entity.ExpandableEnergyStorageBlockEntity;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.interaction.DeepDrillEntity;
import rearth.oritech.block.entity.processing.AtomicForgeBlockEntity;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;

public class TooltipHelper {
    
    public static String getEnergyText(long amount) {
        if (amount < 1000) {
            return String.valueOf(amount);
        } else if (amount < 1_000_000) {
            return getFormatted(amount / 1_000.0) + I18n.get("tooltip.oritech.thousand_abbrev");
        } else if (amount < 1_000_000_000) {
            return getFormatted(amount / 1_000_000.0) + I18n.get("tooltip.oritech.million_abbrev");
        } else if (amount < 1_000_000_000_000L)  {
            return getFormatted(amount / 1_000_000_000.0) + I18n.get("tooltip.oritech.billion_abbrev");
        } else {
            return getFormatted(amount / 1_000_000_000_000.0) + I18n.get("tooltip.oritech.trillion_abbrev");
        }
    }
    
    private static String getFormatted(double number) {
        var formatter = NumberFormat.getNumberInstance(Locale.ROOT);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(number);
    }
    
    public static void addMachineTooltip(List<Component> tooltip, Block block, EntityBlock entityProvider) {
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            var entity = entityProvider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            
            var isAtomicForge = entity instanceof AtomicForgeBlockEntity;
            
            if (entity instanceof MultiblockMachineController multiblockController) {
                var corePositions = multiblockController.getCorePositions();
                tooltip.add(Component.translatable("tooltip.oritech.core_desc").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(corePositions.size())).withStyle(ChatFormatting.GOLD)));
            }
            if (entity instanceof FrameInteractionBlockEntity) {
                tooltip.add(Component.translatable("tooltip.oritech.frame_needed").withStyle(ChatFormatting.GRAY));
            }
            if (entity instanceof MachineAddonController addonProvider) {
                var addonSlots = addonProvider.getAddonSlots();
                tooltip.add(Component.translatable("tooltip.oritech.addon_desc").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(addonSlots.size())).withStyle(ChatFormatting.GOLD)));
            }
            if (entity instanceof MachineBlockEntity machineEntity && machineEntity.getEnergyPerTick() > 1 && !isAtomicForge) {
                var energyRate = machineEntity.getEnergyPerTick();
                if (entity instanceof UpgradableGeneratorBlockEntity) {
                    tooltip.add(Component.translatable("tooltip.oritech.generator_rate_desc").withStyle(ChatFormatting.GRAY).append(Component.translatable("tooltip.oritech.energy_transfer_rate", energyRate).withStyle(ChatFormatting.GOLD)));
                } else if (entity instanceof MachineBlockEntity) {
                    tooltip.add(Component.translatable("tooltip.oritech.machine_rate_desc").withStyle(ChatFormatting.GRAY).append(Component.translatable("tooltip.oritech.energy_transfer_rate", energyRate).withStyle(ChatFormatting.GOLD)));
                }
            } else if (entity instanceof ExpandableEnergyStorageBlockEntity energyStorage) {
                var transferRate = energyStorage.getDefaultExtractionRate();
                tooltip.add(Component.translatable("tooltip.oritech.energy_max_transfer").withStyle(ChatFormatting.GRAY).append(Component.translatable("tooltip.oritech.energy_transfer_rate", transferRate).withStyle(ChatFormatting.GOLD)));
            }
            
            
            if (entity instanceof EnergyApi.BlockProvider energyProvider) {
                var maxStorage = getEnergyText(energyProvider.getEnergyStorage(null).getCapacity());
                if (!isAtomicForge)
                    tooltip.add(Component.translatable("tooltip.oritech.machine_capacity_desc").withStyle(ChatFormatting.GRAY).append(Component.translatable("tooltip.oritech.energy_capacity", maxStorage).withStyle(ChatFormatting.GOLD)));
                
                if (isAtomicForge || energyProvider instanceof DeepDrillEntity)
                    tooltip.add(Component.translatable("tooltip.oritech.needs_laser_power").withStyle(ChatFormatting.BOLD));
                
                var id = BuiltInRegistries.BLOCK.getKey(block);
                if (I18n.exists("tooltip.oritech." + id.getPath() + ".extra")) {
                    tooltip.add(Component.translatable("tooltip.oritech." + id.getPath() + ".extra").withStyle(ChatFormatting.GRAY));
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }
    
    public static Component getFormattedEnergyChangeTooltip(long amount, String unit) {
        var formatted = getEnergyText(amount);
        var text = amount > 0 ? "+" + formatted : formatted;
        return Component.literal(text).withStyle(ChatFormatting.GOLD).append(unit).withStyle(ChatFormatting.GOLD);
    }
    
    public static Component getFormattedValueChangeTooltip(int amount) {
        var text = amount > 0 ? "+" + amount : String.valueOf(amount);
        var color = amount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(text).withStyle(color).append("%").withStyle(color);
    }
    
}
