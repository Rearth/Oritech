package rearth.oritech.block.entity.augmenter;

import io.wispforest.owo.network.ClientAccess;
import rearth.oritech.network.NetworkContent;

public class PlayerAugmentsClient {
    
    public static void handlePlayerAugmentOperation(NetworkContent.AugmentOperationSyncPacket message, ClientAccess access) {
        
        var player = access.player();
        
        var augmentInstance = PlayerAugments.allAugments.get(message.id());
        if (message.operation() == PlayerAugments.AugmentOperation.ADD.ordinal()) {
            augmentInstance.installToPlayer(player);
        } else if (message.operation() == PlayerAugments.AugmentOperation.REMOVE.ordinal()) {
            augmentInstance.removeFromPlayer(player);
        } else if (message.operation() == PlayerAugments.AugmentOperation.TOGGLE.ordinal()) {
            augmentInstance.toggle(player);
        }
    }
}
