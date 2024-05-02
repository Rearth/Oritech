package rearth.oritech.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;

import java.util.List;

public class TooltipHelper {
    
    public static void addMachineTooltip(List<Text> tooltip, Block block, BlockEntityProvider entityProvider) {
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            var entity = entityProvider.createBlockEntity(BlockPos.ORIGIN, block.getDefaultState());
            
            if (entity instanceof MultiblockMachineController multiblockController) {
                var corePositions = multiblockController.getCorePositions();
                tooltip.add(Text.translatable("tooltip.oritech.core_desc").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(corePositions.size())).formatted(Formatting.GOLD)));
            }
            if (entity instanceof MachineAddonController addonProvider) {
                var addonSlots = addonProvider.getAddonSlots();
                tooltip.add(Text.translatable("tooltip.oritech.addon_desc").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(addonSlots.size())).formatted(Formatting.GOLD)));
            }
            if (entity instanceof MachineBlockEntity machineEntity) {
                var energyRate = machineEntity.getEnergyPerTick();
                if (entity instanceof UpgradableGeneratorBlockEntity) {
                    tooltip.add(Text.translatable("tooltip.oritech.generator_rate_desc").formatted(Formatting.GRAY).append(Text.literal(energyRate + " RF").formatted(Formatting.GOLD)));
                } else if (entity instanceof MachineBlockEntity) {
                    tooltip.add(Text.translatable("tooltip.oritech.machine_rate_desc").formatted(Formatting.GRAY).append(Text.literal(energyRate + " RF").formatted(Formatting.GOLD)));
                }
            }
            if (entity instanceof EnergyProvider energyProvider) {
                var maxStorage = energyProvider.getStorage(null).getCapacity();
                tooltip.add(Text.translatable("tooltip.oritech.machine_capacity_desc").formatted(Formatting.GRAY).append(Text.literal(maxStorage + " RF").formatted(Formatting.GOLD)));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        }
    }
    
}
