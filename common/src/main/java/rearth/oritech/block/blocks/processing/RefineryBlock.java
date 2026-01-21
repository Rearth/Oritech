package rearth.oritech.block.blocks.processing;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RefineryBlock extends MultiblockMachine implements EntityBlock {
    
    public RefineryBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RefineryBlockEntity.class;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.refinery_block").withStyle(ChatFormatting.GRAY));
        }
    }
}
