package rearth.oritech.item.tools.util;

import net.minecraft.client.MinecraftClient;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;

public class Helpers {
    
    public static void onClientTickEvent(MinecraftClient client) {
        
        if (client.player == null) return;
        
        // ensure prometheum pick is animated correctly
        var stack = client.player.getMainHandStack();
        if (stack.getItem() instanceof PromethiumPickaxeItem pickaxeItem) {
            pickaxeItem.onHeldTick(stack, client.player, client.world);
        }
        
    }
}
