package rearth.oritech.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.machines.interaction.DeepDrillEntity;
import rearth.oritech.block.entity.machines.processing.AtomicForgeBlockEntity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TooltipHelper {
    
    public static String getEnergyText(long amount) {
        if (amount < 1000) {
            return String.valueOf(amount);
        } else if (amount < 1_000_000) {
            return getFormatted(amount / 1000.0) + "K";
        } else if (amount < 1_000_000_000) {
            return getFormatted(amount / 1000000.0) + "M";
        } else {
            return getFormatted(amount / 1000000000.0) + "B";
        }
    }
    
    private static String getFormatted(double number) {
        var formatter = NumberFormat.getNumberInstance(Locale.ROOT);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(number);
    }
    
    public static void addMachineTooltip(List<Text> tooltip, Block block, BlockEntityProvider entityProvider) {
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            var entity = entityProvider.createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
            
            if (entity instanceof MultiblockMachineController multiblockController) {
                var corePositions = multiblockController.getCorePositions();
                tooltip.add(Text.translatable("tooltip.oritech.core_desc").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(corePositions.size())).formatted(Formatting.GOLD)));
            }
            if (entity instanceof FrameInteractionBlockEntity) {
                tooltip.add(Text.translatable("tooltip.oritech.frame_needed").formatted(Formatting.GRAY));
            }
            if (entity instanceof MachineAddonController addonProvider) {
                var addonSlots = addonProvider.getAddonSlots();
                tooltip.add(Text.translatable("tooltip.oritech.addon_desc").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(addonSlots.size())).formatted(Formatting.GOLD)));
            }
            if (entity instanceof MachineBlockEntity machineEntity) {
                var energyRate = machineEntity.getEnergyPerTick();
                if (entity instanceof UpgradableGeneratorBlockEntity) {
                    tooltip.add(Text.translatable("tooltip.oritech.generator_rate_desc").formatted(Formatting.GRAY).append(Text.literal(energyRate + " RF/t").formatted(Formatting.GOLD)));
                } else if (entity instanceof MachineBlockEntity) {
                    tooltip.add(Text.translatable("tooltip.oritech.machine_rate_desc").formatted(Formatting.GRAY).append(Text.literal(energyRate + " RF/t").formatted(Formatting.GOLD)));
                }
            }
            if (entity instanceof EnergyProvider energyProvider) {
                var maxStorage = getEnergyText(energyProvider.getStorage(null).getCapacity());
                tooltip.add(Text.translatable("tooltip.oritech.machine_capacity_desc").formatted(Formatting.GRAY).append(Text.literal(maxStorage + " RF").formatted(Formatting.GOLD)));
                
                if (energyProvider instanceof AtomicForgeBlockEntity || energyProvider instanceof DeepDrillEntity)
                    tooltip.add(Text.translatable("tooltip.oritech.needs_laser_power").formatted(Formatting.BOLD));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
    }
    
    public static Text getFormattedEnergyChangeTooltip(long amount, String unit) {
        var formatted = getEnergyText(amount);
        var text = amount > 0 ? "+" + formatted : formatted;
        return Text.literal(text).formatted(Formatting.GOLD).append(unit).formatted(Formatting.GOLD);
    }
    
    public static Text getFormattedValueChangeTooltip(int amount) {
        var text = amount > 0 ? "+" + amount : String.valueOf(amount);
        var color = amount > 0 ? Formatting.GREEN : Formatting.RED;
        return Text.literal(text).formatted(color).append("%").formatted(color);
    }
    
}
