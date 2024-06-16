package rearth.oritech.block.blocks.machines.addons;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

public class EnergyAddonBlock extends MachineAddonBlock {
    
    private final long addedCapacity;
    private final long addedInsert;
    private final boolean acceptEnergy;
    
    public EnergyAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity, long addedInsert, boolean acceptEnergy, boolean needsSupport) {
        super(settings, extender, speedMultiplier, efficiencyMultiplier, needsSupport);
        this.addedCapacity = addedCapacity;
        this.addedInsert = addedInsert;
        this.acceptEnergy = acceptEnergy;
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        if (acceptEnergy) {
            return EnergyAcceptorAddonBlockEntity.class;
        } else {
            return super.getBlockEntityType();
        }
    }
    
    public long getAddedCapacity() {
        return addedCapacity;
    }
    
    public long getAddedInsert() {
        return addedInsert;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            
            if (addedCapacity != 0) {
                tooltip.add(
                  Text.translatable("tooltip.oritech.addon_capacity_desc").formatted(Formatting.DARK_GRAY)
                    .append(TooltipHelper.getFormattedEnergyChangeTooltip(addedCapacity, " RF")));
            }
            if (addedInsert != 0) {
                tooltip.add(Text.translatable("tooltip.oritech.addon_transfer_desc").formatted(Formatting.DARK_GRAY)
                              .append(TooltipHelper.getFormattedEnergyChangeTooltip(addedInsert, " RF/t")));
            }
        }
        
        super.appendTooltip(stack, context, tooltip, options);
        
    }
}
