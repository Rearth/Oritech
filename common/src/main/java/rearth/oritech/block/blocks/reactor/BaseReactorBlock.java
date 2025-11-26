package rearth.oritech.block.blocks.reactor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public abstract class BaseReactorBlock extends Block {
    
    public BaseReactorBlock(Properties settings) {
        super(settings);
    }
    
    public boolean validForWalls() { return false; }
    
    public Block requiredStackCeiling() {
        return Blocks.AIR;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        
        var showExtra = Screen.hasControlDown();
        
        if (showExtra) {
            var machineId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            tooltip.add(Component.translatable("tooltip.oritech." + machineId));
            
            for (int i = 0; i < 6; i++) {
                var key = "tooltip.oritech." + machineId + "." + i;
                if (I18n.exists(key)) {
                    tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.oritech.item_extra_info").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
        
    }
}
