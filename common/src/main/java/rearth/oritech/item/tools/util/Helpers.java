package rearth.oritech.item.tools.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

public class Helpers {
    
    public static void onClientTickEvent(Minecraft client) {
        
        if (client.player == null) return;
        
        // ensure prometheum pick is animated correctly
        var stack = client.player.getMainHandItem();
        if (stack.getItem() instanceof PromethiumPickaxeItem pickaxeItem) {
            pickaxeItem.onHeldTick(stack, client.player, client.level);
        }
        
    }
}
