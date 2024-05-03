package rearth.oritech.block.blocks.machines.addons;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

public class EnergyAddonBlock extends MachineAddonBlock {
    
    private final long addedCapacity;
    private final long addedInsert;
    private final boolean acceptEnergy;
    
    public EnergyAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier, long addedCapacity, long addedInsert, boolean acceptEnergy) {
        super(settings, extender, speedMultiplier, efficiencyMultiplier, true);
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
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        
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
        
        super.appendTooltip(stack, world, tooltip, options);
        
    }
}
