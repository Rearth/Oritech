package rearth.oritech.block.entity.augmenter;

import io.wispforest.owo.network.ClientAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.api.Augment;

import java.util.Map;

// for stupid reasons that I don't understand moving this to another second method helps somehow, otherwise we get crashes on server environments because it's loading
// a ClientPlayerEntity class?
public class PlayerAugmentsClient {
    
    public static void receiveAugmentState(Map<Identifier, Augment.AugmentState> data) {
        AttachmentApi.setAttachment(MinecraftClient.getInstance().player, Augment.ACTIVE_AUGMENTS_DATA, data);
    }
}
