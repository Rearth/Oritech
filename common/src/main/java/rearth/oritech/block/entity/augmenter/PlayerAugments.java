package rearth.oritech.block.entity.augmenter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;

import java.util.HashMap;
import java.util.Map;

public class PlayerAugments {
    
    public static final Map<Identifier, Augment> allAugments = new HashMap<>();
    
    // this is called after recipe manager init / recipe reload
    public static void loadAllAugments(RecipeManager manager) {
        allAugments.clear();
        manager.listAllOfType(RecipeContent.AUGMENT_DATA).forEach(recipe -> allAugments.put(recipe.id(), recipe.value().createAugment(recipe.id())));
    }
    
    // called when a client connects to a server / changes world
    public static void refreshPlayerAugments(PlayerEntity player) {
        NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentPlayerStatePacket(AttachmentApi.getAttachmentValue(player, Augment.ACTIVE_AUGMENTS_DATA)));
        for (var augment : PlayerAugments.allAugments.values()) {
            if (augment.isEnabled(player))
                augment.refreshServer(player);
        }
    }
    
    public static void serverTickAugments(ServerPlayerEntity player) {
        
        if (player.getWorld().getTime() % 80 == 0)
            refreshPlayerAugments(player);
        
        for (var augment : allAugments.values()) {
            if (augment.isEnabled(player)) {
                if (player.getServerWorld().getTime() % augment.refreshInterval() == 0)
                    augment.refreshServer(player);
            }
        }
    }
    
    public enum AugmentApplicatorOperation {
        RESEARCH, ADD, REMOVE, NONE, NEEDS_INIT
    }
}
